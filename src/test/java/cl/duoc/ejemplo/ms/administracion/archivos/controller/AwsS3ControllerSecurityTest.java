package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.ejemplo.ms.administracion.archivos.config.JwtRoleConverter;
import cl.duoc.ejemplo.ms.administracion.archivos.config.SecurityConfig;
import cl.duoc.ejemplo.ms.administracion.archivos.config.SecurityRoles;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;

@SpringBootTest(classes = {
		AwsS3Controller.class,
		SecurityConfig.class,
		JwtRoleConverter.class,
		WebMvcAutoConfiguration.class,
		AwsS3ControllerSecurityTest.TestConfig.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AwsS3ControllerSecurityTest.TestConfig.class)
class AwsS3ControllerSecurityTest {

	static final String TOKEN_DESCARGAR = "token-descargar";
	static final String TOKEN_GESTION = "token-gestion";

	@Autowired
	private MockMvc mockMvc;

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		AwsS3Service awsS3Service() {
			return new StubAwsS3Service();
		}

		@Bean
		EfsService efsService() {
			EfsService efsService = new EfsService();
			ReflectionTestUtils.setField(efsService, "efsPath", System.getProperty("java.io.tmpdir"));
			return efsService;
		}

		@Bean
		@Primary
		JwtDecoder jwtDecoder() {
			return token -> switch (token) {
				case TOKEN_DESCARGAR -> jwtWithRoles(SecurityRoles.GUIA_DESCARGAR);
				case TOKEN_GESTION -> jwtWithRoles(SecurityRoles.GUIA_GESTION);
				default -> throw new JwtException("Token invalido");
			};
		}
	}

	static class StubAwsS3Service extends AwsS3Service {

		StubAwsS3Service() {
			super(null);
		}

		@Override
		public List<S3ObjectDto> listObjects(String bucket) {
			return List.of();
		}
	}

	@Test
	void listObjects_sinToken_retorna401() throws Exception {
		mockMvc.perform(get("/s3/test-bucket/objects"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listObjects_conRolDescargar_retorna403() throws Exception {
		mockMvc.perform(get("/s3/test-bucket/objects")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_DESCARGAR))
				.andExpect(status().isForbidden());
	}

	@Test
	void listObjects_conRolGestion_retorna200() throws Exception {
		mockMvc.perform(get("/s3/test-bucket/objects")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_GESTION))
				.andExpect(status().isOk());
	}

	private static Jwt jwtWithRoles(String role) {
		return Jwt.withTokenValue("test")
				.header("alg", "none")
				.claim("roles", List.of(role))
				.claim("sub", "test-user")
				.build();
	}
}
