package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import oryon.springboot.ozone.enums.ComponentStatus;
import oryon.springboot.ozone.enums.ComponentStatusScheduler;
import oryon.springboot.ozone.enums.ReadyStatus;

public record ReadyResponse(
        @Schema(description = "Statut global de disponibilité")
        ReadyStatus status,

        @Schema(description = "État de la base de données")
        ComponentStatus database,

        @Schema(description = "État du planificateur de tâches")
        ComponentStatusScheduler scheduler,

        @JsonProperty("scheduler_last_run_seconds")
        @Schema(description = "Temps écoulé depuis la dernière exécution du scheduler en secondes", example = "12")
        long schedulerLastRunSeconds
) { }
