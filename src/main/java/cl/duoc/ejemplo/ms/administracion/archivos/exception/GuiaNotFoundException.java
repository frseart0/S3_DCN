package cl.duoc.ejemplo.ms.administracion.archivos.exception;

public class GuiaNotFoundException extends RuntimeException {

	public GuiaNotFoundException(String guiaId) {
		super("Guía de despacho no encontrada: " + guiaId);
	}
}
