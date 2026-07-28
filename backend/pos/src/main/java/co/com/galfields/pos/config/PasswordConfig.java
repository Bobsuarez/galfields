package co.com.galfields.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Split out from the full SecurityFilterChain (spec 01-login-empleados-roles,
// step 7) so EmployeeService can hash passwords starting this step, without
// waiting on the rest of SecurityConfig.
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
