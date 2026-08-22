package com.upn.gestion_clinica.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/", "/error", "/api/info/**").permitAll()
                

                .requestMatchers(HttpMethod.GET, "/api/especialidades/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "MEDICO")
                
                .requestMatchers(HttpMethod.GET, "/api/medicos/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "MEDICO")

                .requestMatchers("/api/pacientes/**").hasAnyRole("RECEPCIONISTA", "MEDICO")
                

                .requestMatchers(HttpMethod.GET, "/api/citas/**").hasAnyRole("RECEPCIONISTA", "MEDICO")
                .requestMatchers("/api/citas/**").hasRole("RECEPCIONISTA")
                .requestMatchers("/api/horarios-medicos/**").hasRole("RECEPCIONISTA")
                

                .requestMatchers("/api/atenciones/**").hasRole("MEDICO")
                .requestMatchers("/api/cie10/**").hasRole("MEDICO")
                .requestMatchers("/api/medicamentos/**").hasRole("MEDICO")
                

                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/medicos/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/roles/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/especialidades/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    String json = String.format("{\"mensaje\": \"No autorizado. Se requiere un token JWT válido.\", \"status\": 401, \"timestamp\": \"%s\"}", LocalDateTime.now().toString());
                    response.getWriter().write(json);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    String json = String.format("{\"mensaje\": \"Acceso denegado. No tienes los permisos necesarios.\", \"status\": 403, \"timestamp\": \"%s\"}", LocalDateTime.now().toString());
                    response.getWriter().write(json);
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://clinica-admin-portal.vercel.app", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
