package org.example;

import org.example.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generatedTokenCanBeValidatedAndRead() {
        JwtUtil jwtUtil = new JwtUtil();
        org.example.service.AccountTokenStateService accounts = org.mockito.Mockito.mock(org.example.service.AccountTokenStateService.class);
        org.mockito.Mockito.when(accounts.state("user-1", "student")).thenReturn("fixture-credential");
        ReflectionTestUtils.setField(jwtUtil, "accountTokenStateService", accounts);
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
        assertNull(jwtUtil.readInitialPasswordToken(token));
    }

    @Test
    void initialPasswordTokenHasSeparatePurposeAndCredentialVersion() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secretKey", "test-only-secret-key-that-is-longer-than-thirty-two-bytes");
        Date passwordExpiry = new Date(System.currentTimeMillis() + 60_000);
        String token = jwt.generateInitialPasswordToken("student-account", "old-hash", passwordExpiry);
        var grant = jwt.readInitialPasswordToken(token);
        assertNotNull(grant);
        assertEquals("student-account", grant.getSubject());
        assertTrue(grant.getExpiration().getTime() <= passwordExpiry.getTime());
        assertTrue(jwt.matchesInitialPasswordVersion(grant, "old-hash"));
        assertFalse(jwt.matchesInitialPasswordVersion(grant, "reset-hash"));
        assertFalse(jwt.validateToken(token));
        assertNull(jwt.readInitialPasswordToken(token + "tampered"));
        assertNull(jwt.readInitialPasswordToken(jwt.generateInitialPasswordToken("student-account", "old-hash", new Date(0))));
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "too-short");

        assertThrows(IllegalStateException.class, jwtUtil::validateConfiguration);
    }
}
