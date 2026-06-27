package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.ejemplo.ms.administracion.archivos.config.AppProperties;
import cl.duoc.ejemplo.ms.administracion.archivos.config.JwtRoleConverter;
import cl.duoc.ejemplo.ms.administracion.archivos.config.SecurityConfig;
import cl.duoc.ejemplo.ms.administracion.archivos.config.SecurityRoles;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.CrearGuiaRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoResponse;
import cl.duoc.ejemplo.ms.administracion.archivos.repository.GuiaDespachoRepository;
import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;
import cl.duoc.ejemplo.ms.administracion.archivos.service.GuiaDespachoService;

@SpringBootTest(classes = {
		GuiaDespachoController.class,
		SecurityConfig.class,
		JwtRoleConverter.class,
		WebMvcAutoConfiguration.class,
		GuiaDespachoControllerSecurityTest.TestConfig.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GuiaDespachoControllerSecurityTest.TestConfig.class)
class GuiaDespachoControllerSecurityTest {

	static final String TOKEN_DESCARGAR = "token-descargar";
	static final String TOKEN_GESTION = "token-gestion";

	@Autowired
	private MockMvc mockMvc;

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		GuiaDespachoService guiaDespachoService() {
			return new StubGuiaDespachoService();
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

	static class StubGuiaDespachoService extends GuiaDespachoService {

		StubGuiaDespachoService() {
			super(new GuiaDespachoRepository(), createEfsService(), new NoOpAwsS3Service(), createAppProperties());
		}

		@Override
		public byte[] descargar(String id) {
			return "pdf".getBytes();
		}

		@Override
		public GuiaDespachoResponse crearGuia(CrearGuiaRequest request) {
			return GuiaDespachoResponse.builder().id("abc12345").build();
		}

		private static EfsService createEfsService() {
			EfsService efsService = new EfsService();
			ReflectionTestUtils.setField(efsService, "efsPath", Path.of(System.getProperty("java.io.tmpdir")).toString());
			return efsService;
		}

		private static AppProperties createAppProperties() {
			AppProperties props = new AppProperties();
			props.setS3Bucket("test-bucket");
			return props;
		}
	}

	static class NoOpAwsS3Service extends AwsS3Service {

		NoOpAwsS3Service() {
			super(null);
		}
	}

	@BeforeEach
	void setUp() {
		// context loaded per test class
	}

	@Test
	void descargar_sinToken_retorna401() throws Exception {
		mockMvc.perform(get("/guias/abc12345/descargar"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void descargar_conRolDescargar_retorna200() throws Exception {
		mockMvc.perform(get("/guias/abc12345/descargar")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_DESCARGAR))
				.andExpect(status().isOk());
	}

	@Test
	void descargar_conRolGestion_retorna403() throws Exception {
		mockMvc.perform(get("/guias/abc12345/descargar")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_GESTION))
				.andExpect(status().isForbidden());
	}

	@Test
	void crearGuia_sinToken_retorna401() throws Exception {
		mockMvc.perform(post("/guias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"transportista\":\"TransX\",\"pedidoId\":\"PED-001\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void crearGuia_conRolDescargar_retorna403() throws Exception {
		mockMvc.perform(post("/guias")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_DESCARGAR)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"transportista\":\"TransX\",\"pedidoId\":\"PED-001\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void crearGuia_conRolGestion_retorna201() throws Exception {
		mockMvc.perform(post("/guias")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_GESTION)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"transportista\":\"TransX\",\"pedidoId\":\"PED-001\"}"))
				.andExpect(status().isCreated());
	}

	private static Jwt jwtWithRoles(String role) {
		return Jwt.withTokenValue("test")
				.header("alg", "none")
				.claim("roles", List.of(role))
				.claim("sub", "test-user")
				.build();
	}
}
