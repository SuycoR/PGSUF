package pe.edu.unmsm.servicios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// The in-memory default user is excluded: authentication is handled with JWT (see config/SecurityConfig).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ServiciosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiciosApplication.class, args);
    }
}
