package kr.co.pennyway.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pennyway.api.common.security.handler.JwtAccessDeniedHandler;
import kr.co.pennyway.api.common.security.handler.JwtAuthenticationEntryPoint;
import kr.co.pennyway.infra.common.properties.ServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@ConditionalOnDefaultWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] publicReadOnlyPublicEndpoints = {
            "/favicon.ico",
            // Swagger
            "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger",
    };
    private final ObjectMapper objectMapper;
    private final JwtSecurityConfig jwtSecurityConfig;
    private final ServerProperties serverProperties;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new JwtAccessDeniedHandler(objectMapper);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(jwtSecurityConfig, Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "*").permitAll()
                                .requestMatchers(HttpMethod.GET, publicReadOnlyPublicEndpoints).permitAll()
                                .anyRequest().permitAll()
                )
                .exceptionHandling(
                        exception -> exception
                                .accessDeniedHandler(accessDeniedHandler())
                                .authenticationEntryPoint(authenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(serverProperties.getLocal(), serverProperties.getDev()));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.SET_COOKIE));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
