package cl.duoc.ejemplo.ms.administracion.archivos.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class S3KeyBuilder {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private S3KeyBuilder() {
	}

	public static String buildKey(LocalDate fecha, String transportista, String guiaId) {
		String fechaSegment = fecha.format(DATE_FORMAT);
		String transportistaSegment = sanitizeSegment(transportista);
		return fechaSegment + "/" + transportistaSegment + "/guia-" + guiaId + ".pdf";
	}

	public static String buildPrefix(LocalDate fecha, String transportista) {
		String fechaSegment = fecha.format(DATE_FORMAT);
		String transportistaSegment = sanitizeSegment(transportista);
		return fechaSegment + "/" + transportistaSegment + "/";
	}

	private static String sanitizeSegment(String value) {
		return value.trim().replaceAll("[\\\\/]+", "-");
	}
}
