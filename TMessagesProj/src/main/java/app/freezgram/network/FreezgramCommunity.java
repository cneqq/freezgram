package app.freezgram.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class FreezgramCommunity {
    private static final String API = "https://gift.cneqq.com/freezgram/api/";
    private static final String EXTERA_PROFILES_URL = "https://api.exteragram.app/api/v1/profiles";
    private static final long MAX_EXTERA_PROFILES_BYTES = 4 * 1024 * 1024;
    private static final String ACTIVITY_ENABLED = "activity_opt_in";
    private static final String BADGES = "badges";
    private static final long MAX_ID = (1L << 52) - 1;
    private static final long MAX_BADGE_BYTES = 256 * 1024;
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .followRedirects(false).followSslRedirects(false)
            .callTimeout(12, TimeUnit.SECONDS).build();
    private static final Map<Long, Long> lastActivity = new HashMap<>();
    private static BadgeList badges;
    private static long nextBadgeFetch;
    private static long nextExteraFetch;
    private static volatile Map<Long, Long> exteraSupporterBadges = Collections.emptyMap();
    private static final Map<Long, Call> activityCalls = new HashMap<>();

    private FreezgramCommunity() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("freezgram_community", Context.MODE_PRIVATE);
    }

    public static boolean isActivityEnabled() {
        return prefs().getBoolean(ACTIVITY_ENABLED, true);
    }

    public static synchronized void setActivityEnabled(boolean enabled, int account) {
        prefs().edit().putBoolean(ACTIVITY_ENABLED, enabled).apply();
        if (enabled) {
            reportActivity(account);
        } else {
            for (Call call : activityCalls.values()) call.cancel();
        }
    }

    public static synchronized boolean hasBadge(long id, boolean channel) {
        if (badges == null) {
            try {
                badges = new BadgeList(prefs().getString(BADGES, "{}"));
            } catch (Exception ignored) {
                badges = new BadgeList();
            }
        }
        return (channel ? badges.channels : badges.users).contains(id);
    }

    public static synchronized void refreshBadges() {
        long now = SystemClock.elapsedRealtime();
        if (now < nextBadgeFetch) return;
        nextBadgeFetch = now + TimeUnit.MINUTES.toMillis(1);
        HTTP.newCall(new Request.Builder().url(API + "badges").build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {}

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeable = response) {
                    if (!response.isSuccessful() || response.body() == null) return;
                    String json = response.peekBody(MAX_BADGE_BYTES + 1).string();
                    if (json.length() > MAX_BADGE_BYTES) return;
                    BadgeList updated = new BadgeList(json);
                    synchronized (FreezgramCommunity.class) {
                        boolean changed = badges == null || !badges.users.equals(updated.users)
                                || !badges.channels.equals(updated.channels);
                        if (changed) AndroidUtilities.runOnUIThread(FreezgramCommunity::updateBadges);
                        prefs().edit().putString(BADGES, json).apply();
                        badges = updated;
                        nextBadgeFetch = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(15);
                    }
                } catch (Exception ignored) {
                    // Keep the last valid list on network or format errors.
                }
            }
        });
    }

    public static long getExteraSupporterBadge(long userId) {
        Long documentId = exteraSupporterBadges.get(userId);
        return documentId == null ? 0 : documentId;
    }

    public static synchronized void refreshExteraSupporterBadges() {
        long now = SystemClock.elapsedRealtime();
        if (now < nextExteraFetch) return;
        nextExteraFetch = now + TimeUnit.MINUTES.toMillis(1);
        HTTP.newCall(new Request.Builder().url(EXTERA_PROFILES_URL).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {}

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeable = response) {
                    if (!response.isSuccessful() || response.body() == null) return;
                    byte[] payload = response.peekBody(MAX_EXTERA_PROFILES_BYTES + 1).bytes();
                    if (payload.length > MAX_EXTERA_PROFILES_BYTES) return;
                    JSONArray profiles = new JSONArray(new String(payload, java.nio.charset.StandardCharsets.UTF_8));
                    Map<Long, Long> updated = new HashMap<>();
                    for (int i = 0; i < profiles.length(); i++) {
                        JSONObject profile = profiles.optJSONObject(i);
                        if (profile == null || !"USER".equals(profile.optString("type"))
                                || !"SUPPORTER".equals(profile.optString("status"))) continue;
                        JSONObject badge = profile.optJSONObject("badge");
                        long id = profile.optLong("id");
                        long documentId = badge == null ? 0 : badge.optLong("documentId");
                        if (id > 0 && id <= MAX_ID && documentId != 0) {
                            updated.put(id, documentId);
                        }
                    }
                    boolean changed;
                    synchronized (FreezgramCommunity.class) {
                        changed = !exteraSupporterBadges.equals(updated);
                        exteraSupporterBadges = updated;
                        nextExteraFetch = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(15);
                    }
                    if (changed) AndroidUtilities.runOnUIThread(FreezgramCommunity::updateBadges);
                } catch (Exception ignored) {
                    // Keep the last valid list on network or format errors.
                }
            }
        });
    }

    private static void updateBadges() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                NotificationCenter.getInstance(i).postNotificationName(NotificationCenter.updateInterfaces,
                        MessagesController.UPDATE_MASK_NAME);
            }
        }
    }

    public static synchronized void reportActivity(int account) {
        if (!UserConfig.isValidAccount(account) || !isActivityEnabled()) return;
        long id = UserConfig.getInstance(account).getClientUserId();
        if (id <= 0 || id > MAX_ID || activityCalls.containsKey(id)) return;
        long now = SystemClock.elapsedRealtime();
        Long last = lastActivity.get(id);
        if (last != null && now - last < TimeUnit.MINUTES.toMillis(30)) return;
        lastActivity.put(id, now);
        Call activityCall = HTTP.newCall(new Request.Builder().url(API + "activity")
                .post(RequestBody.create("{\"id\":" + id + "}", MediaType.get("application/json")))
                .build());
        activityCalls.put(id, activityCall);
        activityCall.enqueue(new Callback() {
            private void finish(boolean success) {
                synchronized (FreezgramCommunity.class) {
                    if (!success) {
                        lastActivity.put(id, SystemClock.elapsedRealtime()
                                - TimeUnit.MINUTES.toMillis(29));
                    }
                    activityCalls.remove(id);
                }
            }

            @Override
            public void onFailure(Call call, IOException error) {
                finish(false);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeable = response) {
                    finish(response.code() == 204);
                }
            }
        });
    }

    private static final class BadgeList {
        final Set<Long> users;
        final Set<Long> channels;

        BadgeList() {
            users = channels = Collections.emptySet();
        }

        BadgeList(String json) throws Exception {
            JSONObject data = new JSONObject(json);
            users = read(data.getJSONArray("users"));
            channels = read(data.getJSONArray("channels"));
        }

        private static Set<Long> read(JSONArray list) throws Exception {
            if (list.length() > 5000) throw new IOException("Too many badge recipients");
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < list.length(); i++) {
                Object value = list.get(i);
                if (!(value instanceof Long || value instanceof Integer)) {
                    throw new IOException("Invalid badge recipient");
                }
                long id = ((Number) value).longValue();
                if (id <= 0 || id > MAX_ID) throw new IOException("Invalid badge recipient");
                ids.add(id);
            }
            return ids;
        }
    }
}
