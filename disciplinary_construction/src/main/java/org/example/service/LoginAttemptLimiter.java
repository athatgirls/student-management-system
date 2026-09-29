package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

/** Bounded, per-account limiter, complementary to the gateway's per-IP limiter. */
@Service
public class LoginAttemptLimiter {
    private final Map<String, long[]> windows = new LinkedHashMap<>();
    public synchronized void check(String realm, String account) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue()[0] >= 60000);
        String key = realm + ":" + (account == null ? "" : account.trim().toLowerCase(Locale.ROOT));
        if (key.length() > 256 || (windows.size() >= 10000 && !windows.containsKey(key)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请稍后重试");
        long[] window = windows.computeIfAbsent(key, k -> new long[]{now, 0});
        if (++window[1] > 10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请稍后重试");
    }
}
