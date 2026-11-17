package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

public record SessionResponse(
        @Schema(description = "Jeton JWT à utiliser dans Authorize")
        String token,
        UserResponse user
) {}
