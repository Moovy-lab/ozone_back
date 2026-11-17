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
import oryon.springboot.ozone.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

}

