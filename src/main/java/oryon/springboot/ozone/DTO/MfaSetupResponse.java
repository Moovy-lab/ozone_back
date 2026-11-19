package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record MfaSetupResponse(
        @Schema(description = "Clé secrète TOTP en Base32", example = "JBSWY3DPEHPK3PXP")
        String secret,

        @JsonProperty("otpauth_url")
        @Schema(description = "URL standard pour Google Authenticator / Authy", example = "otpauth://totp/Ozone:+22670000101?secret=JBSWY3DPEHPK3PXP&issuer=Ozone")
        String otpauthUrl,

        @JsonProperty("qr_svg")
        @Schema(description = "Code SVG vectoriel du QR Code")
        String qrSvg,

        @JsonProperty("phone_hint")
        @Schema(description = "Numéro de téléphone masqué", example = "+226****0101")
        String phoneHint,

        @Schema(description = "Confirmation d'envoi du code OTP de vérification")
        OtpSentResponse otp
) {}
