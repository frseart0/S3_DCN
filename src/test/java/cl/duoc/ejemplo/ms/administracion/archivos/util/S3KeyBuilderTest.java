package cl.duoc.ejemplo.ms.administracion.archivos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class S3KeyBuilderTest {

	@Test
	void debeConstruirClaveConFechaYTransportista() {
		LocalDate fecha = LocalDate.of(2026, 6, 3);
		String key = S3KeyBuilder.buildKey(fecha, "transportistaX", "abc12345");

		assertEquals("2026-06-03/transportistaX/guia-abc12345.pdf", key);
	}

	@Test
	void debeSanitizarTransportistaConBarras() {
		LocalDate fecha = LocalDate.of(2026, 6, 3);
		String key = S3KeyBuilder.buildKey(fecha, "trans/portista", "001");

		assertEquals("2026-06-03/trans-portista/guia-001.pdf", key);
	}

	@Test
	void debeConstruirPrefijoParaConsultaHistorial() {
		LocalDate fecha = LocalDate.of(2026, 6, 3);
		String prefix = S3KeyBuilder.buildPrefix(fecha, "transportistaX");

		assertEquals("2026-06-03/transportistaX/", prefix);
	}
}
