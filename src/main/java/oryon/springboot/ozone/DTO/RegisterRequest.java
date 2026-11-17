package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import oryon.springboot.ozone.enums.AccountType;

public record RegisterRequest(
        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, message = "Le prénom doit contenir au moins 2 caractères")
        @JsonProperty("first_name")
        @Schema(example = "Awa")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, message = "Le nom doit contenir au moins 2 caractères")
        @JsonProperty("last_name")
        @Schema(example = "Ouédraogo")
        String lastName,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Schema(example = "+22670000101", description = "Numéro (espaces, points et tirets ignorés), 8 à 15 chiffres")
        String phone,

        @Email(message = "Format d'email invalide")
        @Schema(example = "awa@exemple.bf")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Schema(example = "MotDePasse2026")
        String password,

        @Schema(example = "Ouagadougou")
        String city,

        @NotNull(message = "Vous devez accepter les conditions")
        @AssertTrue(message = "Vous devez accepter les conditions d'utilisation")
        @JsonProperty("accept_terms")
        @Schema(example = "true")
        Boolean acceptTerms,

        @JsonProperty("account_type")
        @Schema(defaultValue = "member")
        AccountType accountType,

        @Valid
        @Schema(description = "Obligatoire si account_type = supplier")
        CompanyRequest company
) {}
