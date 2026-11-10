package com.servitech.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.servitech.authservice.model.Rol;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.UsuarioRepository;
import com.servitech.authservice.security.JwtAuthFilter;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Sin sesiones en el servidor: cada petición trae su propio token
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sin token válido responde 401 (por defecto respondería 403)
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasAnyRole("ADMIN", "TECNICO", 
                                "CLIENTE")
                        .requestMatchers("/api/clientes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ordenes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ordenes/**").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").hasAnyRole("ADMIN", "TECNICO", 
                                "CLIENTE")
                        .requestMatchers("/api/catalogo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/registros/**").authenticated()
                        .requestMatchers("/api/registros/**").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers("/api/sync").hasAnyRole("ADMIN", "TECNICO")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Usuarios de prueba: solo si app.seed-users=true (nunca en producción)
    // Administrador inicial (ADMIN_USER / ADMIN_PASSWORD) y usuarios de prueba
    // (solo si app.seed-users=true)
    @Bean
    CommandLineRunner initUsuarios(UsuarioRepository repo, PasswordEncoder encoder,
            @Value("${app.seed-users:false}") boolean seed,
            @Value("${app.admin-username:}") String adminUser,
            @Value("${app.admin-password:}") String adminPass) {
        return args -> {
            if (!adminUser.isBlank() && adminPass.length() >= 8
                    && repo.findAll().stream().noneMatch(u -> u.getRol() == Rol.ADMIN)) {
                Usuario admin = new Usuario();
                admin.setUsername(adminUser);
                admin.setPasswordHash(encoder.encode(adminPass));
                admin.setNombre("Administrador");
                admin.setRol(Rol.ADMIN);
                repo.save(admin);
            }
            if (!seed) {
                return;
            }
            if (repo.findByUsername("admin").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("admin123"));
                admin.setRol(Rol.ADMIN);
                repo.save(admin);
            }
            if (repo.findByUsername("tecnico").isEmpty()) {
                Usuario t = new Usuario();
                t.setUsername("tecnico");
                t.setPasswordHash(encoder.encode("tecnico123"));
                t.setRol(Rol.TECNICO);
                repo.save(t);
            }
        };
    }
}