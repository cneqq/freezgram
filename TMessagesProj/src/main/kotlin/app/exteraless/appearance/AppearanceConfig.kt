package app.exteraless.appearance

import android.content.SharedPreferences
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.ui.Components.blur3.GlassOutlineStyle
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.NekoXConfig
import tw.nekomimi.nekogram.config.ConfigItem
import xyz.nextalone.nagram.NaConfig

/**
 * Настройки экрана «Appearance», перенесённые из exteraGram.
 *
 * Здесь живут ТОЛЬКО те настройки, аналогов которых нет в NagramX
 * ([xyz.nextalone.nagram.NaConfig] / [tw.nekomimi.nekogram.NekoConfig]).
 * Всё остальное экран берёт из существующих конфигов, чтобы не плодить дубли.
 *
 * Схема повторяет [app.exteraless.OpenExteraConfig]: те же SharedPreferences, тот же [ConfigItem],
 * собственный список. Ключи с префиксом OEAppearance.
 *
 * Загрузка ленивая: [ensureLoaded] вызывается из геттеров, потому что ApplicationLoader
 * трогать нельзя. После первой загрузки проверка стоит один volatile-read.
 */
object AppearanceConfig {

    private val sync = Any()
    private val configs = ArrayList<ConfigItem>()

    @JvmStatic
    fun getConfigTypes(): Map<String, Int> = configs.associate { it.key to it.type }

    @Volatile
    private var configLoaded = false

    /** Максимум слайдера закругления аватарок: радиус = половина стороны, то есть круг. */
    const val AVATAR_CORNERS_MAX = 28

    /** Темы Monet на ролях Telemone — то, чем набор стал 19.08.2026. */
    const val MONET_STYLE_TELEMONE = 0

    /** Прежний набор Monet, пришедший вместе с базой форка (токены вида `a1_100`). */
    const val MONET_STYLE_CLASSIC = 1

    @JvmStatic
    fun getPreferences(): SharedPreferences = NekoConfig.getPreferences()

    // ---- Аватары ----

    /** Закругление аватарок: 0 — квадрат, [AVATAR_CORNERS_MAX] — круг. */
    @JvmField
    val avatarCorners =
        addConfig("OEAppearanceAvatarCorners", ConfigItem.configTypeInt, AVATAR_CORNERS_MAX)

    /** Единое закругление: форумы получают ту же форму аватарки, что и обычные чаты. */
    @JvmField
    val singleCornerRadius =
        addConfig("OEAppearanceSingleCornerRadius", ConfigItem.configTypeBool, false)

    // ---- Список чатов ----

    /** Мини-аватарки отправителей в списке чатов. */
    @JvmField
    val senderMiniAvatars =
        addConfig("OEAppearanceSenderMiniAvatars", ConfigItem.configTypeBool, true)

    /** Прятать эмодзи-статус рядом с заголовком шапки. Дефолт false, как в exteraGram. */
    @JvmField
    val hideActionBarStatus =
        addConfig("OEAppearanceHideActionBarStatus", ConfigItem.configTypeBool, false)

    /**
     * Текст заголовка списка чатов: 0 — имя приложения, 1 — username, 2 — имя,
     * 3 — «Чаты». Только UI.
     */
    @JvmField
    val titleText =
        addConfig("OEAppearanceTitleText", ConfigItem.configTypeInt, 0)

    /**
     * Какой набор темы Monet берут «Monet Light/Dark/AMOLED».
     *
     * Наборы отличаются словарём токенов, а не механикой: [MONET_STYLE_TELEMONE] собран
     * из ролей Material 3, [MONET_STYLE_CLASSIC] — из тональных палитр базы форка. На части
     * прошивок роли M3 система отдаёт со своими поправками, и второй набор выглядит ровнее.
     */
    @JvmField
    val monetStyle =
        addConfig("OEAppearanceMonetStyle", ConfigItem.configTypeInt, MONET_STYLE_TELEMONE)

    /** Суффикс файла темы для выбранного набора: пустой для Telemone. */
    @JvmStatic
    fun monetAssetSuffix(): String = when (monetStyle.Int()) {
        MONET_STYLE_CLASSIC -> "_gram"
        else -> ""
    }

    // ---- Только UI: аналогов в NagramX нет, визуальный эффект пока не подключён ----

    /**
     * Квадратная («squircle») плавающая кнопка вместо круглой. Дефолт true, как в exteraGram —
     * одно из самых заметных отличий экстеры из коробки.
     * Радиус считается как ceil(size * 16 / 56) dp, то есть 16 dp при кнопке 56 dp.
     */
    @JvmField
    val squareFab = addConfig("OEAppearanceSquareFab", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun squareFab(): Boolean {
        ensureLoaded()
        return squareFab.Bool()
    }

    /** Радиус скругления кнопки со стороной [size] dp. */
    @JvmStatic
    fun fabCornerRadius(size: Int): Int =
        if (squareFab()) Math.ceil((size * 16) / 56.0).toInt() else size / 2

    /** Заголовок ActionBar по центру. Дефолт false, как в exteraGram (BooleanPref(0)). */
    private val centerTitle = addConfig("OEAppearanceCenterTitle", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun centerTitle(): Boolean {
        ensureLoaded()
        return NaConfig.centerActionBarTitle.Bool()
    }

    @JvmStatic
    fun setCenterTitle(value: Boolean) {
        NaConfig.centerActionBarTitle.setConfigBool(value)
        NaConfig.centerActionBarTitleType.setConfigInt(if (value) 1 else 0)
    }

    /** «Gooey»-анимация аватарки при оттягивании шапки профиля. Дефолт true, как в exteraGram. */
    @JvmField
    val gooeyAvatarAnimation =
        addConfig("OEAppearanceGooeyAvatarAnimation", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun gooeyAvatarAnimation(): Boolean {
        ensureLoaded()
        return gooeyAvatarAnimation.Bool()
    }

    /**
     * Индивидуальные темы и обои в чатах.
     *
     * Выключено — приложение не применяет тему и обои, выставленные для
     * конкретного диалога, и везде остаётся общая тема. exteraGram гейтит этим
     * ключом две точки: ChatActivity.setupChatTheme (12.9.0:12501) и
     * ChatThemeController.getDialogWallpaper (12.9.0:950).
     *
     * По умолчанию включено — как в exteraGram: иначе обновление приложения
     * молча погасило бы у всех уже настроенные темы чатов.
     */
    @JvmField
    val customThemes =
        addConfig("OEAppearanceCustomThemes", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun customThemes(): Boolean {
        ensureLoaded()
        return customThemes.Bool()
    }

    /** Радиус карточек-секций, dp. Применяется ко всем спискам через RecyclerListView.setSections(). */
    @JvmField
    val sectionRadius =
        addConfig("OEAppearanceSectionRadius", ConfigItem.configTypeInt, 20)

    /** Отдельные заголовки секций. Только UI. */
    @JvmField
    val separateHeaders =
        addConfig("OEAppearanceSeparateHeaders", ConfigItem.configTypeBool, true)

    /** Стиль разделителя: 0 — скрыт, 1 — линия, 2 — сегменты. 0/1 привязаны к NaConfig.hideDividers, 2 — только UI. */
    @JvmField
    val dividerStyle =
        addConfig("OEAppearanceDividerStyle", ConfigItem.configTypeInt, 1)

    /** Стиль стеклянного контура: 0 — блик, 1 — сплошной, 2 — скрыт. Только UI. */
    @JvmField
    val glassOutlineStyle =
        addConfig(object : ConfigItem("OEAppearanceGlassOutlineStyle", ConfigItem.configTypeInt, 0) {
            override fun setConfigInt(v: Int) {
                val changed = Int() != v
                super.setConfigInt(v)
                if (changed) GlassOutlineStyle.dispatchChange()
            }
        })

    /** Стеклянное меню сообщения. Дефолт true, как в exteraGram (BooleanPref(1)). */
    @JvmField
    val glassMessageMenu =
        addConfig("OEAppearanceGlassMessageMenu", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun glassMessageMenu(): Boolean {
        ensureLoaded()
        return glassMessageMenu.Bool()
    }

    @JvmStatic
    fun glassOutlineStyle(): Int {
        ensureLoaded()
        return glassOutlineStyle.Int()
    }

    // ---- Material Design 3 ----
    // switchStyle и sliderStyle уже есть у NagramX (NaConfig, дефолт 2 = MD3) — не дублируем.

    /** M3-индикаторы загрузки. Дефолт true, как в exteraGram. */
    @JvmField
    val newLoadingStyle =
        addConfig("OEAppearanceNewLoadingStyle", ConfigItem.configTypeBool, true)

    /** M3-шапка чата. Дефолт false, как в exteraGram. */
    @JvmField
    val newChatHeaderStyle =
        addConfig("OEAppearanceNewChatHeaderStyle", ConfigItem.configTypeBool, false)

    /** M3-нижняя панель вкладок. Дефолт false, как в exteraGram. */
    @JvmField
    val newNavigationBarStyle =
        addConfig("OEAppearanceNewNavigationBarStyle", ConfigItem.configTypeBool, false)

    @JvmField
    val m3ListItems =
        addConfig("OEAppearanceM3ListItems", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun m3ListItems(): Boolean {
        ensureLoaded()
        return m3ListItems.Bool()
    }

    @JvmStatic
    fun newLoadingStyle(): Boolean {
        ensureLoaded()
        return newLoadingStyle.Bool()
    }

    @JvmStatic
    fun newChatHeaderStyle(): Boolean {
        ensureLoaded()
        return newChatHeaderStyle.Bool()
    }

    @JvmStatic
    fun newNavigationBarStyle(): Boolean {
        ensureLoaded()
        return newNavigationBarStyle.Bool()
    }

    @JvmField
    val md3Player =
        addConfig("OEAppearanceMd3Player", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun md3Player(): Boolean {
        ensureLoaded()
        return md3Player.Bool()
    }

    @JvmField
    val md3MiniPlayer =
        addConfig("OEAppearanceMd3MiniPlayer", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun md3MiniPlayer(): Boolean {
        ensureLoaded()
        return md3MiniPlayer.Bool()
    }

    @JvmField
    val lrclibAllowed =
        addConfig("OEPlayerLrclibAllowed", ConfigItem.configTypeBool, false)

    @JvmField
    val playerCoverStyle = addConfig("OEPlayerCoverStyle", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerCoverSize = addConfig("OEPlayerCoverSize", ConfigItem.configTypeInt, 100)

    @JvmField
    val playerCoverRadius = addConfig("OEPlayerCoverRadius", ConfigItem.configTypeInt, 28)

    @JvmField
    val playerDiscSpin = addConfig("OEPlayerDiscSpin", ConfigItem.configTypeBool, true)

    @JvmField
    val playerCoverSwipe = addConfig("OEPlayerCoverSwipe", ConfigItem.configTypeBool, true)

    @JvmField
    val playerTitleAlign = addConfig("OEPlayerTitleAlign", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerTitleSize = addConfig("OEPlayerTitleSize", ConfigItem.configTypeInt, 1)

    @JvmField
    val playerTitleAbove = addConfig("OEPlayerTitleAbove", ConfigItem.configTypeBool, false)

    @JvmField
    val playerOrder = addConfig("OEPlayerOrder", ConfigItem.configTypeString, "")

    @JvmField
    val playerShowActions = addConfig("OEPlayerShowActions", ConfigItem.configTypeBool, true)

    @JvmField
    val playerShowModes = addConfig("OEPlayerShowModes", ConfigItem.configTypeBool, true)

    @JvmField
    val playerShowLike = addConfig("OEPlayerShowLike", ConfigItem.configTypeBool, true)

    @JvmField
    val playerShowSource = addConfig("OEPlayerShowSource", ConfigItem.configTypeBool, true)

    @JvmField
    val playerWavy = addConfig("OEPlayerWavy", ConfigItem.configTypeBool, true)

    @JvmField
    val playerControlsSize = addConfig("OEPlayerControlsSize", ConfigItem.configTypeInt, 1)

    @JvmField
    val playerActionsStyle = addConfig("OEPlayerActionsStyle", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerColors = addConfig("OEPlayerColors", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerBackground = addConfig("OEPlayerBackground", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerCoverExtend = addConfig("OEPlayerCoverExtend", ConfigItem.configTypeBool, true)

    @JvmField
    val playerMiniCover = addConfig("OEPlayerMiniCover", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerMiniShape = addConfig("OEPlayerMiniShape", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerMiniProgress = addConfig("OEPlayerMiniProgress", ConfigItem.configTypeInt, 0)

    @JvmField
    val playerMiniColors = addConfig("OEPlayerMiniColors", ConfigItem.configTypeInt, 1)

    @JvmField
    val playerMiniCompact = addConfig("OEPlayerMiniCompact", ConfigItem.configTypeBool, false)

    @JvmField
    val playerMiniArtist = addConfig("OEPlayerMiniArtist", ConfigItem.configTypeBool, true)

    @JvmField
    val playerMiniPrevious = addConfig("OEPlayerMiniPrevious", ConfigItem.configTypeBool, false)

    @JvmField
    val playerMiniNext = addConfig("OEPlayerMiniNext", ConfigItem.configTypeBool, true)

    @JvmField
    val playerMiniClose = addConfig("OEPlayerMiniClose", ConfigItem.configTypeBool, true)

    @JvmField
    val playerMiniTop = addConfig("OEPlayerMiniTop", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun playerStyleItems(): List<ConfigItem> {
        ensureLoaded()
        return listOf(
            playerCoverStyle, playerCoverSize, playerCoverRadius, playerDiscSpin, playerCoverSwipe,
            playerTitleAlign, playerTitleSize, playerTitleAbove, playerOrder, playerShowActions,
            playerShowModes, playerShowLike, playerShowSource, playerWavy, playerControlsSize, playerActionsStyle,
            playerColors, playerBackground, playerCoverExtend, playerMiniCover, playerMiniShape, playerMiniProgress,
            playerMiniColors, playerMiniCompact, playerMiniArtist, playerMiniPrevious, playerMiniNext,
            playerMiniClose
        )
    }

    /** Широкая нижняя панель как в Telegram iOS. Уступает M3-панели, если включены обе. */
    @JvmField
    val iosNavigationBarStyle =
        addConfig("OEAppearanceIosNavigationBarStyle", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun iosNavigationBarStyle(): Boolean {
        ensureLoaded()
        return iosNavigationBarStyle.Bool()
    }

    /** 0: legacy Android default, 1: iOS layout, 2: Android layout. */
    @JvmField
    val chatComposerStyle = addConfig("FreezgramChatComposerStyle", ConfigItem.configTypeInt, 0)

    @JvmStatic
    fun chatComposerStyle(): Int {
        ensureLoaded()
        return chatComposerStyle.Int().takeIf { it in 0..2 } ?: 0
    }

    @JvmField
    val composerLiquidGlass = addConfig("FreezgramComposerLiquidGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun composerLiquidGlass(): Boolean {
        ensureLoaded()
        return app.freezgram.ChatComposerGlass.resolveGlassPreference(chatComposerStyle(),
            getPreferences().contains(composerLiquidGlass.key), composerLiquidGlass.Bool())
    }

    @JvmField
    val iosLiquidGlassNavigation =
        addConfig("FreezgramIosLiquidGlassNavigation", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun iosLiquidGlassNavigation(): Boolean {
        ensureLoaded()
        return iosLiquidGlassNavigation.Bool()
    }

    @JvmField
    val composerContentInset = addConfig("FreezgramComposerContentInset", ConfigItem.configTypeInt, 16)
    @JvmStatic
    fun composerContentInset(): Int { ensureLoaded(); return composerContentInset.Int().coerceIn(0, 32) }

    @JvmField
    val composerSideInset = addConfig("FreezgramComposerSideInset", ConfigItem.configTypeInt, 0)
    @JvmStatic
    fun composerSideInset(): Int { ensureLoaded(); return composerSideInset.Int().coerceIn(0, 48) }

    @JvmField
    val composerBottomLift = addConfig("FreezgramComposerBottomLift", ConfigItem.configTypeInt, 0)
    @JvmStatic
    fun composerBottomLift(): Int { ensureLoaded(); return composerBottomLift.Int().coerceIn(0, 64) }

    @JvmField
    val navigationPanelWidth = addConfig("FreezgramNavigationPanelWidth", ConfigItem.configTypeInt, 100)
    @JvmField
    val navigationPanelLift = addConfig("FreezgramNavigationPanelLift", ConfigItem.configTypeInt, 12)
    @JvmStatic
    fun navigationPanelWidth(): Int { ensureLoaded(); return navigationPanelWidth.Int().coerceIn(80, 100) }
    @JvmStatic
    fun navigationPanelLift(): Int { ensureLoaded(); return navigationPanelLift.Int().coerceIn(0, 64) }

    @JvmField
    val transitionStyle = addConfig("FreezgramTransitionStyle", ConfigItem.configTypeInt, 1)
    @JvmStatic
    fun transitionStyle(): Int { ensureLoaded(); return transitionStyle.Int().coerceIn(0, 1) }

    @JvmField
    val snowStyle = addConfig("FreezgramSnowStyle", ConfigItem.configTypeInt, 0)
    @JvmStatic
    fun snowStyle(): Int { ensureLoaded(); return snowStyle.Int().coerceIn(0, 1) }

    @JvmField
    val glassDividerWeight = addConfig("FreezgramGlassDividerWeight", ConfigItem.configTypeInt, 1)
    @JvmStatic
    fun glassDividerWeight(): Int { ensureLoaded(); return glassDividerWeight.Int().coerceIn(1, 4) }
    @JvmField
    val glassDividerOpacity = addConfig("FreezgramGlassDividerOpacity", ConfigItem.configTypeInt, 18)
    @JvmStatic
    fun glassDividerOpacity(): Int { ensureLoaded(); return glassDividerOpacity.Int().coerceIn(0, 100) }
    @JvmField
    val glassDividerInset = addConfig("FreezgramGlassDividerInset", ConfigItem.configTypeInt, 16)
    @JvmStatic
    fun glassDividerInset(): Int { ensureLoaded(); return glassDividerInset.Int().coerceIn(0, 32) }

    @JvmField
    val glassMenuDividers = addConfig("FreezgramGlassMenuDividers", ConfigItem.configTypeBool, true)
    @JvmStatic
    fun glassMenuDividers(): Boolean { ensureLoaded(); return glassMenuDividers.Bool() }

    @JvmField
    val dialogsHeaderGlass = addConfig("FreezgramDialogsHeaderGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun dialogsHeaderGlass(): Boolean { ensureLoaded(); return dialogsHeaderGlass.Bool() }

    @JvmField
    val attachmentPanelsGlass = addConfig("FreezgramAttachmentPanelsGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun attachmentPanelsGlass(): Boolean { ensureLoaded(); return attachmentPanelsGlass.Bool() }

    @JvmField
    val giftGlassBlur = addConfig("FreezgramGiftGlassBlur", ConfigItem.configTypeInt, 24)
    @JvmStatic
    fun giftGlassBlur(): Int { ensureLoaded(); return giftGlassBlur.Int().coerceIn(0, 30) }

    @JvmField
    val giftGlassIntensity = addConfig("FreezgramGiftGlassIntensity", ConfigItem.configTypeInt, 70)
    @JvmStatic
    fun giftGlassIntensity(): Int { ensureLoaded(); return giftGlassIntensity.Int().coerceIn(0, 100) }

    @JvmField
    val giftGlassTransparency = addConfig("FreezgramGiftGlassTransparency", ConfigItem.configTypeInt, 75)
    @JvmStatic
    fun giftGlassTransparency(): Int { ensureLoaded(); return giftGlassTransparency.Int().coerceIn(0, 100) }

    @JvmField
    val giftOrdinaryOpacity = addConfig("FreezgramGiftOrdinaryOpacity", ConfigItem.configTypeInt, 100)
    @JvmStatic
    fun giftOrdinaryOpacity(): Int { ensureLoaded(); return giftOrdinaryOpacity.Int().coerceIn(0, 100) }

    @JvmField
    val giftCardCorners = addConfig("FreezgramGiftCardCorners", ConfigItem.configTypeInt, -1)
    @JvmStatic
    fun giftCardCorners(): Int { ensureLoaded(); return giftCardCorners.Int().coerceIn(-1, 40) }

    @JvmField
    val giftGradientOpacity = addConfig("FreezgramGiftGradientOpacity", ConfigItem.configTypeInt, 100)
    @JvmStatic
    fun giftGradientOpacity(): Int { ensureLoaded(); return giftGradientOpacity.Int().coerceIn(0, 100) }

    @JvmField
    val giftGradientStrength = addConfig("FreezgramGiftGradientStrength", ConfigItem.configTypeInt, 32)
    @JvmStatic
    fun giftGradientStrength(): Int { ensureLoaded(); return giftGradientStrength.Int().coerceIn(0, 100) }

    @JvmField
    val giftGradientAngle = addConfig("FreezgramGiftGradientAngle", ConfigItem.configTypeInt, 45)
    @JvmStatic
    fun giftGradientAngle(): Int { ensureLoaded(); return giftGradientAngle.Int().coerceIn(0, 360) }

    @JvmField
    val giftGradientOutline = addConfig("FreezgramGiftGradientOutline", ConfigItem.configTypeInt, 19)
    @JvmStatic
    fun giftGradientOutline(): Int { ensureLoaded(); return giftGradientOutline.Int().coerceIn(0, 100) }

    @JvmField
    val sameMessageSurfaces = addConfig("FreezgramSameMessageSurfaces", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun sameMessageSurfaces(): Boolean { ensureLoaded(); return sameMessageSurfaces.Bool() }

    @JvmField
    val incomingSurfaceStyle = addConfig("FreezgramIncomingSurfaceStyle", ConfigItem.configTypeInt, 0)
    @JvmStatic
    fun incomingSurfaceStyle(): Int { ensureLoaded(); return if (incomingSurfaceStyle.Int() == 1) 1 else 0 }

    @JvmField
    val incomingSurfaceOpacity = addConfig("FreezgramIncomingSurfaceOpacity", ConfigItem.configTypeInt, 80)
    @JvmStatic
    fun incomingSurfaceOpacity(): Int { ensureLoaded(); return incomingSurfaceOpacity.Int().coerceIn(0, 100) }

    @JvmField
    val incomingSurfaceBlur = addConfig("FreezgramIncomingSurfaceBlur", ConfigItem.configTypeInt, 24)
    @JvmStatic
    fun incomingSurfaceBlur(): Int { ensureLoaded(); return incomingSurfaceBlur.Int().coerceIn(0, 30) }

    @JvmField
    val incomingSurfaceIntensity = addConfig("FreezgramIncomingSurfaceIntensity", ConfigItem.configTypeInt, 70)
    @JvmStatic
    fun incomingSurfaceIntensity(): Int { ensureLoaded(); return incomingSurfaceIntensity.Int().coerceIn(0, 100) }

    @JvmField
    val outgoingSurfaceStyle = addConfig("FreezgramOutgoingSurfaceStyle", ConfigItem.configTypeInt, 0)
    @JvmStatic
    fun outgoingSurfaceStyle(): Int { ensureLoaded(); return if (outgoingSurfaceStyle.Int() == 1) 1 else 0 }

    @JvmField
    val outgoingSurfaceOpacity = addConfig("FreezgramOutgoingSurfaceOpacity", ConfigItem.configTypeInt, 80)
    @JvmStatic
    fun outgoingSurfaceOpacity(): Int { ensureLoaded(); return outgoingSurfaceOpacity.Int().coerceIn(0, 100) }

    @JvmField
    val outgoingSurfaceBlur = addConfig("FreezgramOutgoingSurfaceBlur", ConfigItem.configTypeInt, 24)
    @JvmStatic
    fun outgoingSurfaceBlur(): Int { ensureLoaded(); return outgoingSurfaceBlur.Int().coerceIn(0, 30) }

    @JvmField
    val outgoingSurfaceIntensity = addConfig("FreezgramOutgoingSurfaceIntensity", ConfigItem.configTypeInt, 70)
    @JvmStatic
    fun outgoingSurfaceIntensity(): Int { ensureLoaded(); return outgoingSurfaceIntensity.Int().coerceIn(0, 100) }

    @JvmField
    val giftCardStyle = addConfig("FreezgramGiftCardStyle", ConfigItem.configTypeInt, 1)
    @JvmStatic
    fun giftCardStyle(): Int { ensureLoaded(); return giftCardStyle.Int().coerceIn(0, 2) }

    @JvmField
    val giftCardsGlass = addConfig("FreezgramGiftCardsGlass", ConfigItem.configTypeBool, true)
    @JvmStatic
    fun giftCardsGlass(): Boolean { ensureLoaded(); return giftCardsGlass.Bool() }

    @JvmField
    val unreadCountersGlass = addConfig("FreezgramUnreadCountersGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun unreadCountersGlass(): Boolean { ensureLoaded(); return unreadCountersGlass.Bool() }

    @JvmField
    val messageDecorationsGlass = addConfig("FreezgramMessageDecorationsGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun messageDecorationsGlass(): Boolean { ensureLoaded(); return messageDecorationsGlass.Bool() }

    @JvmField
    val smallElementsGlass = addConfig("FreezgramSmallElementsGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun smallElementsGlass(): Boolean { ensureLoaded(); return smallElementsGlass.Bool() }

    @JvmField
    val foldersLiquidGlass = addConfig("FreezgramFoldersLiquidGlass", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun foldersLiquidGlass(): Boolean { ensureLoaded(); return foldersLiquidGlass.Bool() }

    @JvmField
    @JvmField val glassEnabled = addConfig("FreezgramGlassEnabled", ConfigItem.configTypeBool, true)
    @JvmStatic fun glassEnabled(): Boolean { ensureLoaded(); return glassEnabled.Bool() }

    @JvmField val profileHeaderStyle = addConfig("FreezgramProfileHeaderStyle", ConfigItem.configTypeInt, 0)
    @JvmStatic fun profileHeaderStyle(): Int { ensureLoaded(); return profileHeaderStyle.Int().coerceIn(0, 2) }

    @JvmField val chatActionButtonsGlass = addConfig("FreezgramChatActionButtonsGlass", ConfigItem.configTypeBool, true)
    @JvmStatic fun chatActionButtonsGlass(): Boolean { ensureLoaded(); return chatActionButtonsGlass.Bool() }

    val chatHeaderGlassDim = addConfig("FreezgramChatHeaderGlassDim", ConfigItem.configTypeBool, false)
    @JvmStatic
    fun chatHeaderGlassDim(): Boolean { ensureLoaded(); return chatHeaderGlassDim.Bool() }

    @JvmField
    val glassWallpaperDim = addConfig("FreezgramGlassWallpaperDim", ConfigItem.configTypeBool, true)
    @JvmStatic
    fun glassWallpaperDim(): Boolean { ensureLoaded(); return glassWallpaperDim.Bool() }

    @JvmField
    val glassEnhanceColors = addConfig("FreezgramGlassEnhanceColors", ConfigItem.configTypeBool, true)
    @JvmStatic
    fun glassEnhanceColors(): Boolean { ensureLoaded(); return glassEnhanceColors.Bool() }

    @JvmField
    val navigationGlassBlur = addConfig("FreezgramNavigationGlassBlur", ConfigItem.configTypeInt, 6)
    @JvmField
    val navigationGlassIntensity = addConfig("FreezgramNavigationGlassIntensity", ConfigItem.configTypeInt, 55)
    // -1 preserves the existing theme-dependent defaults (90% dark, 93% light).
    @JvmField
    val navigationGlassTransparency = addConfig("FreezgramNavigationGlassTransparency", ConfigItem.configTypeInt, -1)

    @JvmStatic
    fun navigationGlassBlur(): Int { ensureLoaded(); return navigationGlassBlur.Int().coerceIn(0, 30) }
    @JvmStatic
    fun navigationGlassIntensity(): Int { ensureLoaded(); return navigationGlassIntensity.Int().coerceIn(0, 100) }
    @JvmStatic
    fun navigationGlassTransparency(): Int { ensureLoaded(); return navigationGlassTransparency.Int().coerceIn(-1, 100) }

    @JvmField
    val profileMusicCard =
        addConfig("OEAppearanceProfileMusicCard", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun profileMusicCard(): Boolean {
        ensureLoaded()
        return profileMusicCard.Bool()
    }

    @JvmField
    val iosChatHeader =
        addConfig("OEAppearanceIosChatHeader", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun iosChatHeader(): Boolean {
        ensureLoaded()
        return iosChatHeader.Bool()
    }

    // ---- AI-функции Telegram ----

    /** Прячет кнопку AI-редактора в поле ввода, вложениях и подписи к медиа. */
    @JvmField
    val hideAiEditor =
        addConfig("OEAppearanceHideAiEditor", ConfigItem.configTypeBool, false)

    /** Прячет кнопку «саммари» на сообщении. */
    @JvmField
    val hideMessageSummary =
        addConfig("OEAppearanceHideMessageSummary", ConfigItem.configTypeBool, false)

    /** Прячет блок «Cocoon AI Summary» в Instant View. */
    @JvmField
    val hideIvSummary =
        addConfig("OEAppearanceHideIvSummary", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun hideAiEditor(): Boolean {
        ensureLoaded()
        return hideAiEditor.Bool()
    }

    @JvmStatic
    fun hideMessageSummary(): Boolean {
        ensureLoaded()
        return hideMessageSummary.Bool()
    }

    @JvmStatic
    fun hideIvSummary(): Boolean {
        ensureLoaded()
        return hideIvSummary.Bool()
    }

    // ---- Боковое меню ----

    /** Своя шторка бокового меню вместо стоковой. Дефолт false, как в exteraGram. */
    @JvmField
    val navigationDrawer =
        addConfig("OEAppearanceNavigationDrawer", ConfigItem.configTypeBool, false)

    /** Иммерсивная анимация открытия шторки. Дефолт false, как в exteraGram. */
    @JvmField
    val immersiveDrawerAnimation =
        addConfig("OEAppearanceImmersiveDrawer", ConfigItem.configTypeBool, false)

    /** Порядок и видимость пунктов бокового меню, сериализованный список id. */
    @JvmField
    val mainMenuLayout =
        addConfig("OEAppearanceMainMenuLayout", ConfigItem.configTypeString, "")

    // ---- Лента ----

    /**
     * Показывать ленту нижней вкладкой — на месте вкладки «Контакты».
     * Дефолт false, как в exteraGram (BooleanPref(0)).
     */
    @JvmField
    val showFeedTab = addConfig("OEAppearanceShowFeedTab", ConfigItem.configTypeBool, false)

    @JvmStatic
    fun showFeedTab(): Boolean {
        ensureLoaded()
        return showFeedTab.Bool()
    }

    /** Счётчик непрочитанных постов на вкладке ленты. */
    @JvmField
    val showFeedUnreadCounter =
        addConfig("OEAppearanceFeedUnreadCounter", ConfigItem.configTypeBool, true)

    @JvmStatic
    fun showFeedUnreadCounter(): Boolean {
        ensureLoaded()
        return showFeedUnreadCounter.Bool()
    }

    @JvmStatic
    fun navigationDrawer(): Boolean {
        ensureLoaded()
        return navigationDrawer.Bool()
    }

    @JvmStatic
    fun immersiveDrawerAnimation(): Boolean {
        ensureLoaded()
        return immersiveDrawerAnimation.Bool()
    }

    // ---- Геттеры для Java (в том числе для горячих мест отрисовки) ----

    @JvmStatic
    fun avatarCorners(): Int {
        ensureLoaded()
        return avatarCorners.Int()
    }

    @JvmStatic
    fun singleCornerRadius(): Boolean {
        ensureLoaded()
        return singleCornerRadius.Bool()
    }

    @JvmStatic
    fun senderMiniAvatars(): Boolean {
        ensureLoaded()
        return senderMiniAvatars.Bool()
    }

    /** Текст заголовка списка чатов: 0 — имя приложения, 1 — username, 2 — имя, 3 — «Чаты». */
    @JvmStatic
    fun titleText(): Int {
        ensureLoaded()
        return titleText.Int()
    }

    const val TITLE_TEXT_CUSTOM = 4

    private const val TITLE_TEXT_MIGRATED = "OEAppearanceTitleTextMigrated"

    @JvmStatic
    fun titleTextScrolls(): Boolean = when (titleText()) {
        1, 2, TITLE_TEXT_CUSTOM -> true
        else -> false
    }

    @JvmStatic
    fun folderNameAsTitle(): Boolean = NekoConfig.tabsTitleType.Int() == NekoXConfig.TITLE_TYPE_ICON

    /** Прятать ли эмодзи-статус в шапке списка чатов. */
    @JvmStatic
    fun hideActionBarStatus(): Boolean {
        ensureLoaded()
        return hideActionBarStatus.Bool()
    }

    /** true, если аватарки должны остаться обычными кругами — быстрый выход из хот-пути. */
    @JvmStatic
    fun avatarCornersDefault(): Boolean = avatarCorners() >= AVATAR_CORNERS_MAX

    /**
     * Квадратность аватарки: 0 — круг, 1 — квадрат.
     *
     * Обратная величина к [avatarCorners] и ровно то, что exteraGram зовёт
     * `getAvatarSquareness()`. Нужна там, где геометрия зависит от формы —
     * например, онлайн-точку на квадратной аватарке надо уводить в угол,
     * иначе она наползает на картинку.
     */
    @JvmStatic
    fun avatarSquareness(): Float =
        1f - (avatarCorners().coerceIn(0, AVATAR_CORNERS_MAX).toFloat() / AVATAR_CORNERS_MAX)

    /** Радиус кнопок и вкладок с учётом выбранной формы аватарок, dp. */
    @JvmStatic
    fun pillRadius(radiusDp: Float): Float =
        maxOf(minOf(radiusDp, 6f), radiusDp * (1f - avatarSquareness()))

    /**
     * Смещение онлайн-точки от края аватарки.
     * У круга — заданное значение, у квадрата — по диагонали от угла.
     */
    @JvmStatic
    fun onlineDotOffset(base: Float, radius: Float): Float =
        base + ((radius / Math.sqrt(2.0)).toFloat() - base) * avatarSquareness()

    // ---- Секции настроек ----

    /** Радиус скругления карточек-секций, dp. 0 — острые углы (как сток). */
    @JvmStatic
    fun sectionRadius(): Int {
        ensureLoaded()
        return sectionRadius.Int()
    }

    /** Отдельные заголовки секций (заголовок — своя карточка). */
    @JvmStatic
    fun separateHeaders(): Boolean {
        ensureLoaded()
        return separateHeaders.Bool()
    }

    /**
     * Выносить ли заголовки секций из карточки — с учётом стиля разделителя.
     *
     * В режиме «Сегменты» каждая строка сама по себе карточка, и заголовок
     * внутри неё выглядел бы чужеродно, поэтому exteraGram включает вынос
     * принудительно (ExteraConfig.getSectionsSeparatedHeaders: SEGMENTS || pref).
     */
    @JvmStatic
    fun sectionsSeparatedHeaders(): Boolean {
        return separateHeaders() || dividerStyle() == DIVIDER_SEGMENTS || m3ListItems()
    }

    @JvmStatic
    fun sectionsSeparatedHeadersForced(): Boolean {
        return dividerStyle() == DIVIDER_SEGMENTS || m3ListItems()
    }

    /** Стиль разделителя внутри карточки: 0 — скрыт, 1 — линия, 2 — сегменты. */
    @JvmStatic
    fun dividerStyle(): Int {
        ensureLoaded()
        return dividerStyle.Int()
    }

    const val DIVIDER_HIDDEN = 0
    const val DIVIDER_LINE = 1
    const val DIVIDER_SEGMENTS = 2

    /**
     * Кэш для [dividerHidden]. `Theme.getColor` — самый горячий путь отрисовки,
     * ходить туда через [ensureLoaded] на каждый вызов нельзя.
     * Обновляется из [invalidateDividerStyle] при смене настройки.
     */
    @Volatile
    private var dividerHiddenCache: Boolean? = null

    /**
     * Гасить ли `key_divider` целиком. У exteraGram прозрачный цвет отдаётся во всех режимах,
     * кроме [DIVIDER_LINE] — поэтому стоковые `drawLine(..., Theme.dividerPaint)`
     * перестают рисовать без правки самих ячеек
     */
    @JvmStatic
    fun dividerHidden(): Boolean {
        val cached = dividerHiddenCache
        if (cached != null) return cached
        val value = try {
            dividerStyle() != DIVIDER_LINE || m3ListItems()
        } catch (e: Exception) {
            false
        }
        dividerHiddenCache = value
        return value
    }

    /** Звать при смене стиля разделителя. */
    @JvmStatic
    fun invalidateDividerStyle() {
        dividerHiddenCache = null
    }

    /**
     * Радиус закругления для аватарки со стороной [size] пикселей.
     * При максимуме слайдера возвращает size / 2, то есть круг.
     */
    @JvmStatic
    @JvmOverloads
    fun getAvatarCorners(size: Float, cornerType: Int = CORNER_TYPE_DEFAULT, hasStories: Boolean = false): Int {
        val corners = avatarCorners()
        if (corners <= 0) return 0
        var value = size * corners / (AVATAR_CORNERS_MAX * 2.0)
        if (hasStories) {
            value -= AndroidUtilities.dpf2(2.5f)
        }
        if (!singleCornerRadius()) {
            when (cornerType) {
                CORNER_TYPE_FORUM -> value = value.toInt() * 42.0 / 64.0
                CORNER_TYPE_COMMUNITY -> value = value * 40.0 / 72.0
            }
        }
        if (value <= 0) return 0
        return Math.ceil(value).toInt()
    }

    const val CORNER_TYPE_DEFAULT = 0
    const val CORNER_TYPE_FORUM = 1
    const val CORNER_TYPE_COMMUNITY = 2

    private fun addConfig(key: String, type: Int, defaultValue: Any?): ConfigItem {
        val item = ConfigItem(key, type, defaultValue)
        configs.add(item)
        return item
    }

    private fun addConfig(item: ConfigItem): ConfigItem {
        configs.add(item)
        return item
    }

    @JvmStatic
    fun ensureLoaded() {
        if (!configLoaded) loadConfig(false)
    }

    @JvmStatic
    fun init() {
        loadConfig(false)
    }

    @JvmStatic
    fun loadConfig(force: Boolean) {
        synchronized(sync) {
            if (configLoaded && !force) return
            if (ApplicationLoader.applicationContext == null) return
            val preferences = getPreferences()
            for (item in configs) {
                try {
                    when (item.type) {
                        ConfigItem.configTypeBool ->
                            item.value = preferences.getBoolean(item.key, item.defaultValue as Boolean)

                        ConfigItem.configTypeInt ->
                            item.value = preferences.getInt(item.key, item.defaultValue as Int)

                        ConfigItem.configTypeLong ->
                            item.value = preferences.getLong(item.key, item.defaultValue as Long)

                        ConfigItem.configTypeFloat ->
                            item.value = preferences.getFloat(item.key, item.defaultValue as Float)

                        ConfigItem.configTypeString ->
                            item.value = preferences.getString(item.key, item.defaultValue as String?)
                    }
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            configLoaded = true
        }
        migrateLegacyKeys()
    }

    private fun migrateLegacyKeys() {
        val legacyHidden = getPreferences().getBoolean("HideDividers", false)
        if (legacyHidden && dividerStyle.Int() != DIVIDER_HIDDEN) {
            dividerStyle.setConfigInt(DIVIDER_HIDDEN)
        } else if (!legacyHidden && dividerStyle.Int() == DIVIDER_HIDDEN) {
            NaConfig.hideDividers.setConfigBool(true)
        }
        migrateLegacyTitleName()
        migrateCustomTitle()
        migrateCenterTitle()
        migrateIosChatHeader()
        migrateModernStyles()
        migrateDecorations()
    }

    private fun migrateCenterTitle() {
        if (centerTitle.Bool()) {
            setCenterTitle(true)
            centerTitle.setConfigBool(false)
        }
        val type = NaConfig.centerActionBarTitleType.Int()
        if (type == 2 || type == 3) {
            NaConfig.centerActionBarTitleType.setConfigInt(1)
        }
    }

    private fun migrateIosChatHeader() {
        if (getPreferences().contains(iosChatHeader.key)) return
        iosChatHeader.setConfigBool(NaConfig.centerActionBarTitle.Bool())
    }

    private fun migrateModernStyles() {
        if (NaConfig.switchStyle.Int() == 1) {
            NaConfig.switchStyle.setConfigInt(2)
        }
        if (NaConfig.sliderStyle.Int() == 1) {
            NaConfig.sliderStyle.setConfigInt(2)
        }
    }

    private fun migrateDecorations() {
        val actionBar = NekoConfig.actionBarDecoration.Int()
        val chat = NaConfig.chatDecoration.Int()
        val snow = if (actionBar == 1 || chat == 1) 1 else 0
        if (actionBar != snow) {
            NekoConfig.actionBarDecoration.setConfigInt(snow)
        }
        if (chat != snow) {
            NaConfig.chatDecoration.setConfigInt(snow)
        }
    }

    private const val LEGACY_DEFAULT_TITLE = "Nagram X"

    private fun migrateLegacyTitleName() {
        if (NaConfig.customTitle.String() != LEGACY_DEFAULT_TITLE) return
        NaConfig.customTitle.setConfigString(NaConfig.customTitle.defaultValue as String)
        if (titleText.Int() == TITLE_TEXT_CUSTOM) {
            titleText.setConfigInt(0)
        }
    }

    private fun migrateCustomTitle() {
        val preferences = getPreferences()
        if (preferences.getBoolean(TITLE_TEXT_MIGRATED, false)) return
        if (titleText.Int() == 0) {
            if (NaConfig.customTitleUserName.Bool()) {
                titleText.setConfigInt(2)
            } else if (NaConfig.customTitle.String() != NaConfig.customTitle.defaultValue) {
                titleText.setConfigInt(TITLE_TEXT_CUSTOM)
            }
        }
        NaConfig.customTitleUserName.setConfigBool(false)
        preferences.edit().putBoolean(TITLE_TEXT_MIGRATED, true).apply()
    }

    /** Сбрасывает настройки экрана Appearance к значениям по умолчанию. */
    @JvmStatic
    fun reset() {
        synchronized(sync) {
            val editor = getPreferences().edit()
            for (item in configs) {
                editor.remove(item.key)
                item.value = item.defaultValue
            }
            editor.apply()
        }
        GlassOutlineStyle.dispatchChange()
    }
}
