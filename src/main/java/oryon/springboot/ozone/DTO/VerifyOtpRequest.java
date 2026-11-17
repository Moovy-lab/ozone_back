package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Schema(example = "+22670000101", description = "Numéro (espaces, points et tirets ignorés), 8 à 15 chiffres")
        String phone,

        @NotBlank(message = "Le code OTP est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir exactement 6 chiffres")
        @Schema(example = "123456")
        String code
) {}
