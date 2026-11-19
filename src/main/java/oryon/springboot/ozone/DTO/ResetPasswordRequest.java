package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Schema(example = "+22670000101", description = "Numéro (espaces, points et tirets ignorés), 8 à 15 chiffres")
        String phone,

        @NotBlank(message = "Le code OTP est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir exactement 6 chiffres")
        @Schema(example = "123456")
        String code,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Schema(example = "NouveauMotDePasse2026")
        String password
) {}