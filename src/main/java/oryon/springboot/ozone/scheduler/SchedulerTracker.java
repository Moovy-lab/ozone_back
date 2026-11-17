package oryon.springboot.ozone.scheduler;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SchedulerTracker {

    private final AtomicReference<Instant> lastRun = new AtomicReference<>(Instant.now());

    public void recordRun() {
        lastRun.set(Instant.now());
    }

    public long getSecondsSinceLastRun() {
        return Duration.between(lastRun.get(), Instant.now()).getSeconds();
    }
}
