package oryon.springboot.ozone.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import oryon.springboot.ozone.DTO.*;
import oryon.springboot.ozone.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Authentification et inscription")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé, OTP envoyé",
                    content = @Content(schema = @Schema(implementation = OtpSentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou paiement refusé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OtpSentResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpSentResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Vérification du code OTP et ouverture de session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou code expiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SessionResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        SessionResponse session = authService.verifyOtp(request);
        return ResponseEntity.ok(session);
    }

}

