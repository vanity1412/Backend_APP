package com.utetea.backend.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HMACUtil {
    
    public static final String HMACMD5 = "HmacMD5";
    public static final String HMACSHA1 = "HmacSHA1";
    public static final String HMACSHA256 = "HmacSHA256";
    public static final String HMACSHA512 = "HmacSHA512";
    public static final Charset UTF8CHARSET = StandardCharsets.UTF_8;
    
    private static byte[] hmacEncode(final String algorithm, final String key, final String data) {
        try {
            Mac macGenerator = Mac.getInstance(algorithm);
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            macGenerator.init(signingKey);
            return macGenerator.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static String hmacBase64Encode(final String algorithm, final String key, final String data) {
        byte[] hmacEncodeBytes = hmacEncode(algorithm, key, data);
        if (hmacEncodeBytes == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(hmacEncodeBytes);
    }
    
    public static String hmacHexStringEncode(final String algorithm, final String key, final String data) {
        byte[] hmacEncodeBytes = hmacEncode(algorithm, key, data);
        if (hmacEncodeBytes == null) {
            return null;
        }
        return byteArrayToHexString(hmacEncodeBytes);
    }
    
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
