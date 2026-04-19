package com.shiro.ordermanagementsystem.mail;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VerificationCodeStore {

    public static final Duration TTL = Duration.ofMinutes(3);

    private static final SecureRandom RNG = new SecureRandom();
    private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();

    private VerificationCodeStore() {}

    public static String generateCode() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    public static Instant save(String email, String code) {
        Instant expiresAt = Instant.now().plus(TTL);
        STORE.put(key(email), new Entry(code, expiresAt));
        return expiresAt;
    }

    public static boolean verify(String email, String code) {
        Entry entry = STORE.get(key(email));
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt)) {
            STORE.remove(key(email));
            return false;
        }
        boolean ok = entry.code.equals(code);
        if (ok) STORE.remove(key(email));
        return ok;
    }

    public static Instant getExpiresAt(String email) {
        Entry entry = STORE.get(key(email));
        return entry != null ? entry.expiresAt : null;
    }

    public static void clear(String email) {
        STORE.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record Entry(String code, Instant expiresAt) {}
}
