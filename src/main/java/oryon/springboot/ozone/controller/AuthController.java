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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import oryon.springboot.ozone.DTO.*;
import oryon.springboot.ozone.entities.User;
import oryon.springboot.ozone.service.AuthService;
import oryon.springboot.ozone.service.MfaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Authentification et inscription")
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;

    public AuthController(AuthService authService, MfaService mfaService) {
        this.authService = authService;
        this.mfaService = mfaService;
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

    @PostMapping("/resend-otp")
    @Operation(summary = "Renvoyer un nouveau code OTP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau code envoyé",
                    content = @Content(schema = @Schema(implementation = OtpSentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou compte déjà vérifié",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OtpSentResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        OtpSentResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur (téléphone ou e-mail)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte — copiez le token dans Authorize",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou compte non vérifié",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Identifiant ou mot de passe incorrect",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        Object result = authService.login(request, clientIp);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/mfa/setup")
    @Operation(summary = "Initialiser la configuration MFA (clé TOTP, QR code SVG et OTP)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clé et QR code",
                    content = @Content(schema = @Schema(implementation = MfaSetupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié (jeton manquant ou expiré)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal User currentUser) {
        MfaSetupResponse response = mfaService.setupMfa(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/confirm")
    @Operation(summary = "Confirmer l'activation MFA et obtenir 10 codes de récupération")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte et codes de récupération",
                    content = @Content(schema = @Schema(implementation = MfaConfirmResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou code expiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié (jeton manquant ou expiré)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Trop de requêtes (MFA_LOCKED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MfaConfirmResponse> confirmMfa(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody MfaConfirmRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        MfaConfirmResponse response = mfaService.confirmMfa(currentUser, request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Vérifier le code MFA ou un code de récupération lors de la connexion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Code invalide ou déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Jeton MFA manquant ou expiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Trop de requêtes (MFA_LOCKED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SessionResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        SessionResponse session = mfaService.verifyMfa(request, clientIp);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion utilisateur",
            description = "Avec l’en-tête X-Ozone-Client. Sans effet pour l’application mobile, qui efface simplement son jeton."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cookie effacé")
    })
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "X-Ozone-Client", required = false) String clientHeader) {

        boolean isMobile = clientHeader != null && clientHeader.toLowerCase().contains("mobile");

        if (!isMobile) {
            ResponseCookie clearCookie = ResponseCookie.from("ozone_token", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(0)
                    .sameSite("None")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of());
        }

        return ResponseEntity.ok(Map.of());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander un code OTP de réinitialisation de mot de passe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code envoyé si le numéro existe",
                    content = @Content(schema = @Schema(implementation = OtpSentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Format de numéro invalide",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OtpSentResponse> forgotPassword(@Valid @RequestBody ResendOtpRequest request) {
        OtpSentResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe avec un code OTP et ouvrir une session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou code OTP expiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SessionResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        SessionResponse session = authService.resetPassword(request);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/login-otp/request")
    @Operation(summary = "Demander un code OTP par SMS pour se connecter sans mot de passe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code envoyé si le numéro existe",
                    content = @Content(schema = @Schema(implementation = OtpSentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Format de numéro invalide",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OtpSentResponse> requestLoginOtp(@Valid @RequestBody ResendOtpRequest request) {
        OtpSentResponse response = authService.requestLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-otp/verify")
    @Operation(summary = "Vérifier le code OTP de connexion et ouvrir une session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ouverte",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou code expiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> verifyLoginOtp(@Valid @RequestBody VerifyOtpRequest request) {
        Object result = authService.verifyLoginOtp(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié (jeton manquant ou expiré)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(new MeResponse(UserResponse.fromEntity(currentUser)));
    }

}

