//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package eu.eidas.auth.commons;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public final class EidasStringUtil {
    private static final Pattern STRING_SPLITTER = Pattern.compile("[,;]");

    @NotNull
    public static byte[] decodeBytesFromBase64(@NotNull String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    @NotNull
    public static String decodeStringFromBase64(@NotNull String base64String) {
        return toString(decodeBytesFromBase64(base64String));
    }

    @NotNull
    public static String encodeToBase64(@NotNull byte[] bytes) {
        return bytes.length == 0 ? "" : Base64.getEncoder().encodeToString(bytes);
    }

    @NotNull
    public static String encodeToBase64(@NotNull String value) {
        return encodeToBase64(getBytes(value));
    }

    @NotNull
    public static byte[] getBytes(@NotNull String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    public static String toString(@NotNull byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static List<String> getTokens(String tokens) {
        return (StringUtils.isNotEmpty(tokens) ? Arrays.asList(STRING_SPLITTER.split(tokens)) : new ArrayList<>());
    }

    private EidasStringUtil() {
    }
}
