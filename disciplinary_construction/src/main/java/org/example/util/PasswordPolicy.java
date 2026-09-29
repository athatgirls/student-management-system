package org.example.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordPolicy {
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordPolicy() { }
    public static String temporaryPassword() {
        byte[] bytes = new byte[24]; RANDOM.nextBytes(bytes);
        return "Zt9!" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !password.matches(".*[0-9].*") || !password.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("密码至少8位，包含字母和数字，UTF-8编码不超过72字节");
        }
    }
    public static String legacyPassword(String studentId) {
        if (studentId == null) return "";
        return "Hbut_" + studentId.substring(Math.max(0, studentId.length() - 6));
    }
}
