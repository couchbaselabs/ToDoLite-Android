package com.couchbase.todolite.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtil {
    public static String MD5(String string) {
        if (string == null)
            return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] inputBytes = string.getBytes();
            byte[] hashBytes = digest.digest(inputBytes);
            return byteArrayToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) { }

        return null;
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
