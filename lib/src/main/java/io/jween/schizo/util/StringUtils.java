package io.jween.schizo.util;

import java.nio.charset.Charset;


/**
 * Created by Jwn on 2018/1/8.
 */

public class StringUtils {
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    public static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, DEFAULT_CHARSET);
    }

    public static String bytesToString(byte[] input, Charset charset) {
        return new String(input, charset);
    }

    public static byte[] stringToBytes(String str) {
        return stringToBytes(str, DEFAULT_CHARSET);
    }

    public static byte[] stringToBytes(String input, Charset charset) {
        return input.getBytes(charset);
    }
}
