package org.example;

import org.example.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generatedTokenCanBeValidatedAndRead() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey",
                "test-only-secret-key-that-is-longer-than-thirty-two-bytes");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 60_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshThreshold", 10_000L);
        jwtUtil.validateConfiguration();

        String token = jwtUtil.generateToken("user-1", "student", "张同学");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user-1", jwtUtil.getUserIdFromToken(token));
        assertEquals("student", jwtUtil.getUserTypeFromToken(token));
        assertEquals("张同学", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "too-short");

        assertThrows(IllegalStateException.class, jwtUtil::validateConfiguration);
    }
}
