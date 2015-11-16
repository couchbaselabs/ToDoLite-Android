package com.couchbase.todolite.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ToDoLitePreferences {

    private static final String PREF_VERSION_CODE = "VersionCode";
    private static final String PREF_GUEST_BOOLEAN = "GuestBoolean";
    private static final String PREF_CURRENT_LIST_ID = "CurrentListId";
    private static final String PREF_CURRENT_USER_ID = "CurrentUserId";
    private static final String PREF_CURRENT_USER_PASSWORD = "CurrentUserPassword";
    private static final String PREF_LAST_RCVD_FB_ACCESS_TOKEN = "LastReceivedFbAccessToken";

    private final SharedPreferences preferences;

    public ToDoLitePreferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setVersionCode(int versionCode) {
        if (versionCode != 0) {
            preferences.edit().putInt(PREF_VERSION_CODE, versionCode).apply();
        } else {
            preferences.edit().remove(PREF_VERSION_CODE).apply();
        }
    }

    public int getVersionCode() {
        return preferences.getInt(PREF_VERSION_CODE, 0);
    }

    public Boolean getGuestBoolean() {
        return preferences.getBoolean(PREF_GUEST_BOOLEAN, false);
    }

    public void setGuestBoolean(Boolean bool) {
        preferences.edit().putBoolean(PREF_GUEST_BOOLEAN, bool).apply();
    }

    public String getCurrentListId() {
        return preferences.getString(PREF_CURRENT_LIST_ID, null);
    }

    public void setCurrentListId(String id) {
        if (id != null) {
            preferences.edit().putString(PREF_CURRENT_LIST_ID, id).apply();
        } else {
            preferences.edit().remove(PREF_CURRENT_LIST_ID).apply();
        }
    }

    public String getCurrentUserId() {
        return preferences.getString(PREF_CURRENT_USER_ID, null);
    }

    public void setCurrentUserId(String id) {
        if (id != null) {
            preferences.edit().putString(PREF_CURRENT_USER_ID, id).apply();
        } else {
            preferences.edit().remove(PREF_CURRENT_USER_ID).apply();
        }
    }

    public void setCurrentUserPassword(String userNamePass) {
        if (userNamePass != null) {
            preferences.edit().putString(PREF_CURRENT_USER_PASSWORD, userNamePass).apply();
        } else {
            preferences.edit().remove(PREF_CURRENT_USER_PASSWORD).apply();
        }
    }

    public String getCurrentUserPassword() {
        String userId = preferences.getString(PREF_CURRENT_USER_PASSWORD, null);
        return userId;
    }

    public void setLastReceivedFbAccessToken(String fbAccessToken) {
        if (fbAccessToken != null) {
            preferences.edit().putString(PREF_LAST_RCVD_FB_ACCESS_TOKEN, fbAccessToken).apply();
        } else {
            preferences.edit().remove(PREF_LAST_RCVD_FB_ACCESS_TOKEN).apply();
        }
    }

    public String getLastReceivedFbAccessToken() {
        return preferences.getString(PREF_LAST_RCVD_FB_ACCESS_TOKEN, null);
    }
}
