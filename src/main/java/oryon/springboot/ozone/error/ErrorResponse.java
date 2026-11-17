package oryon.springboot.ozone.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime errorDate;
    private HttpStatus status;
    private String message; // message d’erreur
    private String debugMessage; //message utile pour comprendre l’erreur par le développeur
}