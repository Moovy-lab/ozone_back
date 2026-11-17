package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record HealthResponse(
        @Schema(example = "ok")
        String status,

        @Schema(type = "string", format = "date-time")
        Instant time
) {}
