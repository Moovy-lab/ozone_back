package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MfaConfirmResponse(
        String token,
        UserResponse user,
        @JsonProperty("recovery_codes")
        @Schema(description = "10 codes de récupération à usage unique, affichés une seule fois")
        List<String> recoveryCodes
) {}

