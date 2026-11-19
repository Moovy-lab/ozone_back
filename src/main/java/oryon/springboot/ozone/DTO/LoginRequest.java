package oryon.springboot.ozone.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'identifiant (téléphone ou e-mail) est obligatoire")
        @Schema(description = "Téléphone ou e-mail", example = "aminata@exemple.bf")
        String identifier,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Schema(example = "Ozone@2026")
        String password
) {}
