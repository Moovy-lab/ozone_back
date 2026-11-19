package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaConfirmRequest(
        @NotBlank(message = "Le mfa_token est obligatoire")
        @JsonProperty("mfa_token")
        String mfaToken,

        @NotBlank(message = "Le code SMS est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code SMS doit contenir exactement 6 chiffres")
        @JsonProperty("sms_code")
        @Schema(example = "123456")
        String smsCode,

        @NotBlank(message = "Le code d'application est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code TOTP doit contenir exactement 6 chiffres")
        @Schema(example = "654321")
        String code
) {}

