package util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Per-GlassFish-process throttle. Resets on restart; use shared storage for clustered deployments. */
public final class AuthRateLimiter {

    private static final int MAX_FAILURES = 8;
    private static final long WINDOW_MILLIS = 15L * 60L * 1000L;
    private static final ConcurrentHashMap<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final AtomicInteger OPERATIONS = new AtomicInteger();

    private AuthRateLimiter() { }

    public static boolean isBlocked(String key) {
        Entry entry = ENTRIES.get(key);
        if (entry == null) return false;
        synchronized (entry) {
            if (System.currentTimeMillis() - entry.windowStart >= WINDOW_MILLIS) {
                ENTRIES.remove(key, entry);
                return false;
            }
            return entry.failures.get() >= MAX_FAILURES;
        }
    }

    public static void recordFailure(String key) {
        ENTRIES.compute(key, (ignored, entry) -> {
            if (entry == null || System.currentTimeMillis() - entry.windowStart >= WINDOW_MILLIS) {
                entry = new Entry();
            }
            entry.failures.incrementAndGet();
            return entry;
        });
        cleanupOccasionally();
    }

    public static void recordSuccess(String key) {
        ENTRIES.remove(key);
        cleanupOccasionally();
    }

    private static void cleanupOccasionally() {
        if (OPERATIONS.incrementAndGet() % 128 != 0) return;
        long now = System.currentTimeMillis();
        ENTRIES.entrySet().removeIf(item -> now - item.getValue().windowStart >= WINDOW_MILLIS);
    }

    private static final class Entry {
        private final long windowStart = System.currentTimeMillis();
        private final AtomicInteger failures = new AtomicInteger();
    }
}
