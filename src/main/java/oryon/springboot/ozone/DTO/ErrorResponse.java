package oryon.springboot.ozone.DTO;

import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        Map<String, Object> details
) {}
