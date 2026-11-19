package oryon.springboot.ozone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import oryon.springboot.ozone.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailIgnoreCase(String email);
}