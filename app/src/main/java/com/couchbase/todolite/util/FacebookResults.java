package com.couchbase.todolite.util;

import com.facebook.login.LoginResult;

/**
 * @author Moss
 */
public interface FacebookResults {
    void onFbLoginSuccess(String token, String userId, String name);
}
