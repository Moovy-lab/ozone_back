package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record OtpSentResponse(
        String phone,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> otp
) {}
