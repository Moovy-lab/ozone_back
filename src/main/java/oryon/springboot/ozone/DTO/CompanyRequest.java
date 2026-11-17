package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Le nom de l'entreprise est obligatoire")
        @JsonProperty("company_name")
        String companyName,

        String ifu,
        String rccm,
        String description
) {}
