package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaRequiredResponse(
        @JsonProperty("mfa_required") boolean mfaRequired,
        @JsonProperty("mfa_token") String mfaToken
) {}
