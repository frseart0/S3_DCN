package cl.duoc.ejemplo.ms.administracion.archivos.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import cl.duoc.ejemplo.ms.administracion.archivos.config.AppProperties;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.GuiaAccessDeniedException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.GuiaNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.model.GuiaDespacho;
import cl.duoc.ejemplo.ms.administracion.archivos.repository.GuiaDespachoRepository;

class GuiaDespachoServiceTest {

	@TempDir
	Path tempDir;

	private GuiaDespachoRepository repository;
	private EfsService efsService;
	private StubAwsS3Service awsS3Service;
	private AppProperties appProperties;
	private GuiaDespachoService guiaDespachoService;

	@BeforeEach
	void setUp() throws IOException {
		repository = new GuiaDespachoRepository();
		efsService = new EfsService();
		ReflectionTestUtils.setField(efsService, "efsPath", tempDir.toString());
		awsS3Service = new StubAwsS3Service();
		appProperties = new AppProperties();
		appProperties.setS3Bucket("test-bucket");
		appProperties.setAdminTransportista("admin");
		guiaDespachoService = new GuiaDespachoService(repository, efsService, awsS3Service, appProperties);

		byte[] contenido = "contenido-guia".getBytes();
		String efsPath = "transportistaX/guia-abc12345.pdf";
		Files.createDirectories(tempDir.resolve("transportistaX"));
		Files.write(tempDir.resolve(efsPath), contenido);

		GuiaDespacho guia = GuiaDespacho.builder()
				.id("abc12345")
				.transportista("transportistaX")
				.pedidoId("PED-001")
				.fecha(LocalDate.now())
				.efsRelativePath(efsPath)
				.s3Key("2026-06-03/transportistaX/guia-abc12345.pdf")
				.subidaS3(false)
				.creadaEn(LocalDateTime.now())
				.actualizadaEn(LocalDateTime.now())
				.build();

		repository.save(guia);
	}

	@Test
	void debePermitirDescargaAlTransportistaPropietario() {
		byte[] resultado = guiaDespachoService.descargar("abc12345", "transportistaX");
		assertEquals("contenido-guia", new String(resultado));
	}

	@Test
	void debePermitirDescargaAlAdmin() {
		byte[] resultado = guiaDespachoService.descargar("abc12345", "admin");
		assertEquals("contenido-guia", new String(resultado));
	}

	@Test
	void debeRechazarDescargaSinPermisos() {
		assertThrows(GuiaAccessDeniedException.class,
				() -> guiaDespachoService.descargar("abc12345", "otroTransportista"));
	}

	@Test
	void debeLanzarExcepcionSiGuiaNoExiste() {
		assertThrows(GuiaNotFoundException.class,
				() -> guiaDespachoService.descargar("inexistente", "transportistaX"));
	}

	@Test
	void debeDescargarDesdeS3CuandoEstaSubida() {
		GuiaDespacho guiaS3 = GuiaDespacho.builder()
				.id("s3guia01")
				.transportista("transportistaX")
				.pedidoId("PED-002")
				.fecha(LocalDate.now())
				.efsRelativePath("transportistaX/guia-s3guia01.pdf")
				.s3Key("2026-06-03/transportistaX/guia-s3guia01.pdf")
				.subidaS3(true)
				.creadaEn(LocalDateTime.now())
				.actualizadaEn(LocalDateTime.now())
				.build();
		repository.save(guiaS3);
		awsS3Service.setContenido("desde-s3".getBytes());

		byte[] resultado = guiaDespachoService.descargar("s3guia01", "transportistaX");

		assertArrayEquals("desde-s3".getBytes(), resultado);
	}

	private static class StubAwsS3Service extends AwsS3Service {

		private byte[] contenido = new byte[0];

		StubAwsS3Service() {
			super(null);
		}

		void setContenido(byte[] contenido) {
			this.contenido = contenido;
		}

		@Override
		public byte[] downloadAsBytes(String bucket, String key) {
			return contenido;
		}
	}
}
