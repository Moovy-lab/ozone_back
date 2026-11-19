package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Schema(example = "+22670000101", description = "Numéro (espaces, points et tirets ignorés), 8 à 15 chiffres")
        String phone
) {}
