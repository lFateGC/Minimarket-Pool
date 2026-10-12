package com.minimarket.gestion.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minimarket.gestion.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .error("No autorizado")
                    .mensaje("Acceso denegado: Token JWT ausente, expirado o inválido.")
                    .path(request.getRequestURI())
                    .build();

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            response.getOutputStream().println(mapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .error("Acceso prohibido")
                    .mensaje("No cuentas con los permisos necesarios para acceder a este recurso (Rol insuficiente).")
                    .path(request.getRequestURI())
                    .build();

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            response.getOutputStream().println(mapper.writeValueAsString(error));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/frontend/**",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                .requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/registro-cliente").permitAll()
                .requestMatchers("/api/auth/perfil").authenticated()
                .requestMatchers("/api/auth/usuarios", "/api/auth/usuarios/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**").permitAll()
                .requestMatchers("/api/productos/**", "/api/categorias/**", "/api/proveedores/**").hasRole("ADMIN")

                .requestMatchers("/api/clientes/mi-perfil").hasAnyRole("CLIENTE", "ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("ADMIN")
                .requestMatchers("/api/clientes/**").hasAnyRole("ADMIN", "VENDEDOR")

                .requestMatchers(HttpMethod.GET, "/api/inventario/lotes/**", "/api/inventario/reportes/**").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers("/api/inventario/**").hasRole("ADMIN")

                .requestMatchers("/api/ventas/mi-compra", "/api/ventas/mis-compras").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers("/api/ventas/**").hasAnyRole("ADMIN", "VENDEDOR")

                .requestMatchers("/api/compras/**").hasRole("ADMIN")
                .requestMatchers("/api/caja/**").hasAnyRole("ADMIN", "VENDEDOR")

                .requestMatchers("/api/reportes/stock-bajo", "/api/reportes/por-vencer").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers("/api/reportes/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
