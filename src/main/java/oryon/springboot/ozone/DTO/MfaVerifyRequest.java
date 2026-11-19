package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank(message = "Le mfa_token est obligatoire")
        @JsonProperty("mfa_token")
        String mfaToken,

        @Pattern(regexp = "^\\d{6}$", message = "Le code d'application doit contenir 6 chiffres")
        @Schema(example = "123456")
        String code,

        @JsonProperty("recovery_code")
        @Schema(example = "ABCD-EFGH-JKMN")
        String recoveryCode
) {}
