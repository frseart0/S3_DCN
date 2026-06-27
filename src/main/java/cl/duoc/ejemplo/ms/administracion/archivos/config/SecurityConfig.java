package cl.duoc.ejemplo.ms.administracion.archivos.config;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!local")
public class SecurityConfig {

	private final JwtRoleConverter jwtRoleConverter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
						.requestMatchers(new AntPathRequestMatcher("/guias/*/descargar", "GET"))
								.hasAuthority(SecurityRoles.ROLE_GUIA_DESCARGAR)
						.requestMatchers(new AntPathRequestMatcher("/guias", "GET"))
								.hasAuthority(SecurityRoles.ROLE_GUIA_GESTION)
						.requestMatchers(new AntPathRequestMatcher("/guias/**"))
								.hasAuthority(SecurityRoles.ROLE_GUIA_GESTION)
						.requestMatchers(new AntPathRequestMatcher("/s3/**"))
								.hasAuthority(SecurityRoles.ROLE_GUIA_GESTION)
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(this::handleUnauthorized)
						.accessDeniedHandler(accessDeniedHandler()));

		return http.build();
	}

	private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
				"Se requiere un token JWT valido", request.getRequestURI());
	}

	private AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) -> writeErrorResponse(response,
				HttpServletResponse.SC_FORBIDDEN, "Forbidden",
				"No tiene permisos para acceder a este recurso", request.getRequestURI());
	}

	private void writeErrorResponse(HttpServletResponse response, int status, String error,
			String message, String path) throws IOException {
		ErrorResponse body = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(status)
				.error(error)
				.message(message)
				.path(path)
				.build();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(response.getOutputStream(), body);
	}
}
