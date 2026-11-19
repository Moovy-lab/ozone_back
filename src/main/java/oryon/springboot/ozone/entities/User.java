package oryon.springboot.ozone.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import oryon.springboot.ozone.enums.AccountType;
import oryon.springboot.ozone.enums.UserRole;
import oryon.springboot.ozone.enums.UserStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String city;

    @Column(nullable = false)
    private boolean acceptTerms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType = AccountType.member;

    private boolean verified = false;
    private String otpCode;
    private Instant otpExpiresAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.pending_otp;

    private String address;

    private String mfaSecret;
    private boolean mfaEnabled = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recovery_codes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "code_hash")
    private List<String> recoveryCodeHashes = new ArrayList<>();

    private Long lastUsedTotpStep;

}
