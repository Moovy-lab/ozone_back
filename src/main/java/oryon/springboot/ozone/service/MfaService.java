package oryon.springboot.ozone.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oryon.springboot.ozone.DTO.*;
import oryon.springboot.ozone.entities.User;
import oryon.springboot.ozone.exception.TooManyAttemptsException;
import oryon.springboot.ozone.exception.UnauthorizedException;
import oryon.springboot.ozone.repository.UserRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MfaService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.env:dev}")
    private String appEnv;

    public MfaService(UserRepository userRepository, LoginAttemptService loginAttemptService,
                      JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MfaSetupResponse setupMfa(User user) {

        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        String secret = new Base32().encodeToString(buffer).replace("=", "");

        String issuer = "Ozone";
        String accountLabel = URLEncoder.encode(issuer + ":" + user.getPhone(), StandardCharsets.UTF_8);
        String otpauthUrl = String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                accountLabel, secret, issuer
        );

        String qrSvg = generateQrSvg(otpauthUrl, 220, 220);

        String otpCode = String.format("%06d", new SecureRandom().nextInt(999999));
        user.setMfaSecret(secret);
        user.setOtpCode(otpCode);
        user.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);

        String phoneHint = maskPhone(user.getPhone());
        Map<String, String> otpPayload = "dev".equalsIgnoreCase(appEnv)
                ? Map.of("devCode", otpCode)
                : null;

        OtpSentResponse otpSent = new OtpSentResponse(
                user.getPhone(),
                "Code OTP envoyé pour activer la double authentification",
                otpPayload
        );

        return new MfaSetupResponse(secret, otpauthUrl, qrSvg, phoneHint, otpSent);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 6) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    private String generateQrSvg(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, Map.of(EncodeHintType.MARGIN, 1));

            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns=\"http://w3.org\" viewBox=\"0 0 ")
                    .append(width).append(" ").append(height)
                    .append("\" width=\"").append(width).append("\" height=\"").append(height).append("\">")
                    .append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>")
                    .append("<path fill=\"#000000\" d=\"");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        svg.append("M").append(x).append(",").append(y).append("h1v1h-1z");
                    }
                }
            }
            svg.append("\"/></svg>");
            return svg.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du QR Code SVG", e);
        }
    }

    @Transactional
    public MfaConfirmResponse confirmMfa(User currentUser, MfaConfirmRequest req, String clientIp) {
        String ipKey = "mfa_ip:" + clientIp;
        String userKey = "mfa_user:" + currentUser.getId();

        if (loginAttemptService.isBlocked(ipKey) || loginAttemptService.isBlocked(userKey)) {
            throw new TooManyAttemptsException("MFA_LOCKED", "Trop de tentatives MFA échouées. Réessayez dans 15 minutes.");
        }

        UUID tokenUserId = jwtService.validateMfaToken(req.mfaToken());
        if (!tokenUserId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("Ce mfa_token n'appartient pas à l'utilisateur connecté");
        }

        if (currentUser.getOtpCode() == null || !currentUser.getOtpCode().equals(req.smsCode())
                || currentUser.getOtpExpiresAt() == null || currentUser.getOtpExpiresAt().isBefore(Instant.now())) {
            loginAttemptService.recordFailure(ipKey);
            loginAttemptService.recordFailure(userKey);
            throw new IllegalArgumentException("Code SMS invalide ou expiré");
        }

        if (currentUser.getMfaSecret() == null || !verifyTotpCode(currentUser.getMfaSecret(), req.code())) {
            loginAttemptService.recordFailure(ipKey);
            loginAttemptService.recordFailure(userKey);
            throw new IllegalArgumentException("Code d'authentification (application) invalide");
        }

        List<String> plainRecoveryCodes = generateRecoveryCodes(10);
        List<String> hashedRecoveryCodes = plainRecoveryCodes.stream()
                .map(passwordEncoder::encode)
                .toList();

        currentUser.setMfaEnabled(true);
        currentUser.setOtpCode(null);
        currentUser.setOtpExpiresAt(null);
        currentUser.setRecoveryCodeHashes(new ArrayList<>(hashedRecoveryCodes));
        userRepository.save(currentUser);

        loginAttemptService.reset(ipKey);
        loginAttemptService.reset(userKey);

        String newSessionToken = jwtService.generateToken(currentUser);
        return new MfaConfirmResponse(newSessionToken, UserResponse.fromEntity(currentUser), plainRecoveryCodes);
    }

    private List<String> generateRecoveryCodes(int count) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                if (j == 4) sb.append('-');
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    private boolean verifyTotpCode(String base32Secret, String code) {
        try {
            byte[] key = new Base32().decode(base32Secret);
            long currentInterval = Instant.now().getEpochSecond() / 30;
            // Tolérance de -1, 0, +1 fenêtre de 30 secondes
            for (int i = -1; i <= 1; i++) {
                String candidate = generateTotpForInterval(key, currentInterval + i);
                if (candidate.equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateTotpForInterval(byte[] key, long interval) throws Exception {
        byte[] data = java.nio.ByteBuffer.allocate(8).putLong(interval).array();
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    @Transactional
    public SessionResponse verifyMfa(MfaVerifyRequest req, String clientIp) {

        UUID userId = jwtService.validateMfaToken(req.mfaToken(), "mfa_login");

        String ipKey = "mfa_verify_ip:" + clientIp;
        String userKey = "mfa_verify_user:" + userId;

        if (loginAttemptService.isBlocked(ipKey) || loginAttemptService.isBlocked(userKey)) {
            throw new TooManyAttemptsException("MFA_LOCKED", "Trop de tentatives MFA échouées. Réessayez dans 15 minutes.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));

        boolean hasCode = req.code() != null && !req.code().isBlank();
        boolean hasRecovery = req.recoveryCode() != null && !req.recoveryCode().isBlank();

        if (!hasCode && !hasRecovery) {
            throw new IllegalArgumentException("Veuillez fournir un code d'application ou un code de récupération");
        }

        boolean verified = false;

        if (hasCode) {
            Long matchedStep = findMatchingTotpStep(user.getMfaSecret(), req.code());
            if (matchedStep != null && (user.getLastUsedTotpStep() == null || matchedStep > user.getLastUsedTotpStep())) {
                user.setLastUsedTotpStep(matchedStep);
                verified = true;
            }
        }

        else {
            String normalizedRecovery = req.recoveryCode().trim().toUpperCase();
            List<String> hashes = user.getRecoveryCodeHashes();
            String matchedHash = null;

            for (String hash : hashes) {
                if (passwordEncoder.matches(normalizedRecovery, hash)) {
                    matchedHash = hash;
                    break;
                }
            }
            if (matchedHash != null) {
                hashes.remove(matchedHash);
                user.setRecoveryCodeHashes(hashes);
                verified = true;
            }
        }

        if (!verified) {
            loginAttemptService.recordFailure(ipKey);
            loginAttemptService.recordFailure(userKey);
            throw new IllegalArgumentException("Code MFA ou code de récupération invalide");
        }

        userRepository.save(user);
        loginAttemptService.reset(ipKey);
        loginAttemptService.reset(userKey);

        String sessionToken = jwtService.generateToken(user);
        return new SessionResponse(sessionToken, UserResponse.fromEntity(user));
    }

    private Long findMatchingTotpStep(String base32Secret, String code) {
        if (base32Secret == null) return null;
        try {
            byte[] key = new Base32().decode(base32Secret);
            long currentInterval = Instant.now().getEpochSecond() / 30;
            for (int i = -1; i <= 1; i++) {
                long step = currentInterval + i;
                if (generateTotpForInterval(key, step).equals(code)) {
                    return step;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }


}
