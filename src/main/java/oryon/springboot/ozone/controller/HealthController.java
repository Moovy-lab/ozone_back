package oryon.springboot.ozone.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import oryon.springboot.ozone.DTO.HealthResponse;
import oryon.springboot.ozone.DTO.ReadyResponse;
import oryon.springboot.ozone.enums.ComponentStatus;
import oryon.springboot.ozone.enums.ComponentStatusScheduler;
import oryon.springboot.ozone.enums.ReadyStatus;
import oryon.springboot.ozone.scheduler.SchedulerTracker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

@RestController
@RequestMapping("/api/health")
@Tag(name = "health", description = "Vérification de l'état de l'application")
public class HealthController {

    private final DataSource dataSource;
    private final SchedulerTracker schedulerTracker;

    public HealthController(DataSource dataSource, SchedulerTracker schedulerTracker) {
        this.dataSource = dataSource;
        this.schedulerTracker = schedulerTracker;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("ok", Instant.now()));
    }

    @GetMapping("/ready")
    public ResponseEntity<ReadyResponse> ready() {
        boolean isDbUp = checkDatabase();
        long secondsSinceLastRun = schedulerTracker.getSecondsSinceLastRun();

        boolean isSchedulerUp = secondsSinceLastRun < 300;

        ComponentStatus dbStatus = isDbUp ? ComponentStatus.ok : ComponentStatus.unavailable;
        ComponentStatusScheduler schedulerStatus = isSchedulerUp
                ? ComponentStatusScheduler.ok
                : ComponentStatusScheduler.late;

        boolean isReady = isDbUp && isSchedulerUp;

        ReadyResponse response = new ReadyResponse(
                isReady ? ReadyStatus.ok : ReadyStatus.degraded,
                dbStatus,
                schedulerStatus,
                secondsSinceLastRun
        );

        HttpStatus httpStatus = isReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
