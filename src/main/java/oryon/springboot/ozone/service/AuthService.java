package oryon.springboot.ozone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oryon.springboot.ozone.DTO.*;
import oryon.springboot.ozone.entities.Company;
import oryon.springboot.ozone.entities.User;
import oryon.springboot.ozone.enums.AccountType;
import oryon.springboot.ozone.enums.UserStatus;
import oryon.springboot.ozone.exception.TooManyAttemptsException;
import oryon.springboot.ozone.exception.UnauthorizedException;
import oryon.springboot.ozone.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.env:dev}")
    private String appEnv;

    @Transactional
    public OtpSentResponse register(RegisterRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        if (userRepository.existsByPhone(cleanedPhone)) {
            throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé");
        }
        if (req.email() != null && !req.email().isBlank() && userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        AccountType type = req.accountType() != null ? req.accountType() : AccountType.member;
        if (type == AccountType.supplier && req.company() == null) {
            throw new IllegalArgumentException("Les informations de l'entreprise (company) sont obligatoires pour un compte supplier");
        }

        String otpCode = String.format("%06d", new SecureRandom().nextInt(999999));

        User user = new User();
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setPhone(cleanedPhone);
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setCity(req.city());
        user.setAcceptTerms(req.acceptTerms());
        user.setAccountType(type);
        user.setOtpCode(otpCode);
        user.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        if (type == AccountType.supplier) {
            Company company = new Company();
            company.setCompanyName(req.company().companyName());
            company.setIfu(req.company().ifu());
            company.setRccm(req.company().rccm());
            company.setDescription(req.company().description());
            company.setUser(user);
            user.setCompany(company);
        }

        userRepository.save(user);

        Map<String, String> otpPayload = "dev".equalsIgnoreCase(appEnv)
                ? Map.of("devCode", otpCode)
                : null;

        return new OtpSentResponse(cleanedPhone, "Compte créé, OTP envoyé", otpPayload);
    }

    @Transactional
    public SessionResponse verifyOtp(VerifyOtpRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        User user = userRepository.findByPhone(cleanedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé avec ce numéro"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.code())) {
            throw new IllegalArgumentException("Code OTP invalide");
        }
        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Le code OTP a expiré");
        }

        user.setStatus(UserStatus.active);
        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new SessionResponse(token, UserResponse.fromEntity(user));
    }

    @Transactional
    public OtpSentResponse resendOtp(ResendOtpRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        User user = userRepository.findByPhone(cleanedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé avec ce numéro"));


        if (user.isVerified() || user.getStatus() == UserStatus.active) {
            throw new IllegalArgumentException("Ce compte est déjà vérifié");
        }

        String newOtpCode = String.format("%06d", new SecureRandom().nextInt(999999));
        user.setOtpCode(newOtpCode);
        user.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);

        Map<String, String> otpPayload = "dev".equalsIgnoreCase(appEnv)
                ? Map.of("devCode", newOtpCode)
                : null;

        return new OtpSentResponse(cleanedPhone, "Nouveau code envoyé", otpPayload);
    }

    public Object login(LoginRequest req, String clientIp) {
        String rawIdentifier = req.identifier().trim();
        boolean isEmail = rawIdentifier.contains("@");

        String normalizedId = isEmail
                ? rawIdentifier.toLowerCase()
                : rawIdentifier.replaceAll("[\\s.\\-]", "");

        String accountKey = "acc:" + normalizedId;
        String ipKey = "ip:" + clientIp;

        if (loginAttemptService.isBlocked(ipKey) || loginAttemptService.isBlocked(accountKey)) {
            throw new TooManyAttemptsException("TOO_MANY_ATTEMPTS", "Trop de tentatives de connexion. Réessayez dans 15 minutes.");
        }

        Optional<User> userOpt = isEmail
                ? userRepository.findByEmailIgnoreCase(normalizedId)
                : userRepository.findByPhone(normalizedId);

        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPassword())) {
            loginAttemptService.recordFailure(ipKey);
            loginAttemptService.recordFailure(accountKey);
            throw new UnauthorizedException("Identifiant ou mot de passe incorrect");
        }

        User user = userOpt.get();

        if (user.getStatus() == UserStatus.pending_otp || !user.isVerified()) {
            throw new IllegalArgumentException("Compte non vérifié. Veuillez valider votre code OTP.");
        }
        if (user.getStatus() == UserStatus.suspended || user.getStatus() == UserStatus.banned) {
            throw new UnauthorizedException("Votre compte est suspendu ou banni.");
        }

        loginAttemptService.reset(ipKey);
        loginAttemptService.reset(accountKey);

        if (user.isMfaEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user, "mfa_login");
            return new MfaRequiredResponse(true, mfaToken);
        }

        String token = jwtService.generateToken(user);
        return new SessionResponse(token, UserResponse.fromEntity(user));
    }

    @Transactional
    public OtpSentResponse forgotPassword(ResendOtpRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        Optional<User> userOpt = userRepository.findByPhone(cleanedPhone);
        Map<String, String> otpPayload = null;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetOtp = String.format("%06d", new SecureRandom().nextInt(999999));
            user.setOtpCode(resetOtp);
            user.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            userRepository.save(user);

            if ("dev".equalsIgnoreCase(appEnv)) {
                otpPayload = Map.of("devCode", resetOtp);
            }
        }

        return new OtpSentResponse(
                cleanedPhone,
                "Code envoyé si le numéro existe",
                otpPayload
        );
    }

    @Transactional
    public SessionResponse resetPassword(ResetPasswordRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        User user = userRepository.findByPhone(cleanedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Code OTP invalide ou expiré"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.code())
                || user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Code OTP invalide ou expiré");
        }

        user.setPassword(passwordEncoder.encode(req.password()));
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);

        if (user.getStatus() == UserStatus.pending_otp) {
            user.setStatus(UserStatus.active);
            user.setVerified(true);
        }
        userRepository.save(user);

        loginAttemptService.reset("acc:" + cleanedPhone);
        if (user.getEmail() != null) {
            loginAttemptService.reset("acc:" + user.getEmail().toLowerCase());
        }

        String token = jwtService.generateToken(user);
        return new SessionResponse(token, UserResponse.fromEntity(user));
    }

    @Transactional
    public OtpSentResponse requestLoginOtp(ResendOtpRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        Optional<User> userOpt = userRepository.findByPhone(cleanedPhone);
        Map<String, String> otpPayload = null;

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getStatus() != UserStatus.suspended && user.getStatus() != UserStatus.banned) {
                String loginOtp = String.format("%06d", new SecureRandom().nextInt(999999));
                user.setOtpCode(loginOtp);
                user.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
                userRepository.save(user);

                if ("dev".equalsIgnoreCase(appEnv)) {
                    otpPayload = Map.of("devCode", loginOtp);
                }
            }
        }

        return new OtpSentResponse(
                cleanedPhone,
                "Code envoyé si le numéro existe",
                otpPayload
        );
    }

    @Transactional
    public Object verifyLoginOtp(VerifyOtpRequest req) {

        String cleanedPhone = req.phone().replaceAll("[\\s.\\-]", "");
        if (!cleanedPhone.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Numéro de téléphone invalide (8 à 15 chiffres requis)");
        }

        User user = userRepository.findByPhone(cleanedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Code OTP invalide ou expiré"));

        if (user.getStatus() == UserStatus.suspended || user.getStatus() == UserStatus.banned) {
            throw new IllegalArgumentException("Votre compte est suspendu ou banni");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.code())
                || user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Code OTP invalide ou expiré");
        }

        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        if (user.getStatus() == UserStatus.pending_otp) {
            user.setStatus(UserStatus.active);
            user.setVerified(true);
        }
        userRepository.save(user);

        if (user.isMfaEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user, "mfa_login");
            return new MfaRequiredResponse(true, mfaToken);
        }

        String token = jwtService.generateToken(user);
        return new SessionResponse(token, UserResponse.fromEntity(user));
    }
    
}

