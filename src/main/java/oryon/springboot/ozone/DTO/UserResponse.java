package oryon.springboot.ozone.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import oryon.springboot.ozone.entities.User;
import oryon.springboot.ozone.enums.UserRole;
import oryon.springboot.ozone.enums.UserStatus;
import java.util.UUID;

public record UserResponse(
        UUID id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String phone,
        String email,
        UserRole role,
        UserStatus status,
        String city,
        String address
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCity(),
                user.getAddress()
        );
    }
}
