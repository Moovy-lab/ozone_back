package oryon.springboot.ozone.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private record AttemptInfo(int count, Instant lockedUntil) {}

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;
        if (info.lockedUntil() != null && Instant.now().isBefore(info.lockedUntil())) {
            return true;
        }
        if (info.lockedUntil() != null && Instant.now().isAfter(info.lockedUntil())) {
            attempts.remove(key);
        }
        return false;
    }

    public void recordFailure(String key) {
        AttemptInfo current = attempts.getOrDefault(key, new AttemptInfo(0, null));
        int newCount = current.count() + 1;
        Instant lockUntil = newCount >= MAX_ATTEMPTS
                ? Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES)
                : null;
        attempts.put(key, new AttemptInfo(newCount, lockUntil));
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
