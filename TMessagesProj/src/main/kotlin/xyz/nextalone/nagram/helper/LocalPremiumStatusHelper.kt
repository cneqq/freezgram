package xyz.nextalone.nagram.helper

import androidx.annotation.Keep
import androidx.core.content.edit
import com.google.gson.Gson
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stars
import tw.nekomimi.nekogram.NekoConfig
import xyz.nextalone.nagram.NaConfig

@Keep
data class LocalEmojiStatusData(
    val documentId: Long?,
    val until: Int?,
    val collectible: LocalCollectibleStatus? = null,
    val locallyCleared: Boolean = false
)

@Keep
data class LocalCollectibleStatus(
    val id: Long,
    val title: String?,
    val slug: String?,
    val patternDocumentId: Long,
    val centerColor: Int,
    val edgeColor: Int,
    val patternColor: Int,
    val textColor: Int
) {
    constructor(status: TLRPC.TL_emojiStatusCollectible) : this(
        status.collectible_id, status.title, status.slug, status.pattern_document_id,
        status.center_color, status.edge_color, status.pattern_color, status.text_color
    )

    fun toStatus(documentId: Long, expires: Int?): TLRPC.TL_emojiStatusCollectible =
        TLRPC.TL_emojiStatusCollectible().also {
            it.collectible_id = id
            it.document_id = documentId
            it.title = title.orEmpty()
            it.slug = slug.orEmpty()
            it.pattern_document_id = patternDocumentId
            it.center_color = centerColor
            it.edge_color = edgeColor
            it.pattern_color = patternColor
            it.text_color = textColor
            if (expires != null) {
                it.flags = it.flags or 1
                it.until = expires
            }
        }
}

object LocalPremiumStatusHelper {
    const val KEY_PREFIX = "useLocalEmojiStatusData_"

    private val dataMap = mutableMapOf<Long, LocalEmojiStatusData?>()
    private val loadedUsers = mutableSetOf<Long>()

    @JvmStatic
    @Synchronized
    fun getLocalStatus(user: TLRPC.User): TLRPC.EmojiStatus? {
        val data = getActiveData(user.id) ?: return null
        if (data.locallyCleared) return TLRPC.TL_emojiStatusEmpty()
        val documentId = data.documentId ?: return null
        data.collectible?.let { return it.toStatus(documentId, data.until) }
        return TLRPC.TL_emojiStatus().apply {
            document_id = documentId
            data.until?.let {
                flags = flags or 1
                until = it
            }
        }
    }

    private fun getActiveData(userId: Long): LocalEmojiStatusData? {
        if (!NekoConfig.localPremium.Bool() || !isLocalUser(userId)) return null
        val data = getDataForUser(userId) ?: return null
        val until = data.until ?: 0
        if (until != 0 && until <= (System.currentTimeMillis() / 1000).toInt()) {
            return null
        }
        return data
    }

    private fun getDataForUser(userId: Long): LocalEmojiStatusData? {
        if (userId == 0L) return null
        initForUser(userId)
        return dataMap[userId]
    }

    private fun isLocalUser(userId: Long): Boolean {
        for (i in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            if (UserConfig.getInstance(i).isClientActivated &&
                UserConfig.getInstance(i).getClientUserId() == userId
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    @Synchronized
    fun initForUser(userId: Long, force: Boolean = false) {
        if (!force && loadedUsers.contains(userId)) return
        loadedUsers.add(userId)

        try {
            val gson = Gson()
            val userKey = KEY_PREFIX + userId

            var jsonStr = NaConfig.getPreferences().getString(userKey, null)

            if (jsonStr == null) {
                val legacyJson = NaConfig.useLocalEmojiStatusData.String()
                if (legacyJson.isNotEmpty()) {
                    jsonStr = legacyJson
                    NaConfig.getPreferences().edit { putString(userKey, jsonStr) }
                }
            }

            dataMap[userId] = if (!jsonStr.isNullOrEmpty()) {
                gson.fromJson(jsonStr, LocalEmojiStatusData::class.java)
            } else {
                null
            }
        } catch (_: Exception) {
            dataMap[userId] = null
        }
    }

    @JvmStatic
    @Synchronized
    fun apply(userId: Long, status: TLRPC.EmojiStatus?, localOnly: Boolean) {
        if (!NekoConfig.localPremium.Bool() || userId == 0L) return
        val data = when (status) {
            is TLRPC.TL_emojiStatus -> LocalEmojiStatusData(
                status.document_id, status.until.takeIf { (status.flags and 1) != 0 }
            )
            is TLRPC.TL_emojiStatusCollectible -> LocalEmojiStatusData(
                status.document_id, status.until.takeIf { (status.flags and 1) != 0 }, LocalCollectibleStatus(status)
            )
            null, is TLRPC.TL_emojiStatusEmpty ->
                if (localOnly) LocalEmojiStatusData(null, null, locallyCleared = true) else null
            else -> return
        }
        store(userId, data)
    }

    @JvmStatic
    @Synchronized
    fun recoverCollectible(userId: Long, gifts: List<TL_stars.SavedStarGift>): Boolean {
        val data = getActiveData(userId) ?: return false
        val documentId = data.documentId ?: return false
        if (documentId == 0L || data.collectible != null) return false
        var match: TL_stars.TL_starGiftUnique? = null
        for (saved in gifts) {
            val gift = saved.gift as? TL_stars.TL_starGiftUnique ?: continue
            if (saved.refunded || gift.burned || DialogObject.getPeerDialogId(gift.owner_id) != userId
                || gift.getDocument()?.id != documentId) continue
            if (match != null && match.id != gift.id) return false
            match = gift
        }
        val gift = match ?: return false
        val status = MessagesController.emojiStatusCollectibleFromGift(gift)
        store(userId, data.copy(collectible = LocalCollectibleStatus(status)))
        return true
    }

    private fun store(userId: Long, data: LocalEmojiStatusData?) {
        loadedUsers.add(userId)
        dataMap[userId] = data
        NaConfig.getPreferences().edit { putString(KEY_PREFIX + userId, data?.let { Gson().toJson(it) } ?: "") }
    }
}
