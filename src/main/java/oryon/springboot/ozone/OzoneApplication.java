package oryon.springboot.ozone;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT"
)
@OpenAPIDefinition(
		info = @Info(
				title = "Ozone API",
				version = "1.0.0",
				description = "Documentation de l'API Ozone"
		),
		security = @SecurityRequirement(name = "bearerAuth")
)
@SpringBootApplication
public class OzoneApplication {

	public static void main(String[] args) {
		SpringApplication.run(OzoneApplication.class, args);
	}

}
