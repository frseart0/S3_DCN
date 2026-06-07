package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.ActualizarGuiaRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.CrearGuiaRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoResponse;
import cl.duoc.ejemplo.ms.administracion.archivos.service.GuiaDespachoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

	private final GuiaDespachoService guiaDespachoService;

	@PostMapping
	public ResponseEntity<GuiaDespachoResponse> crearGuia(@Valid @RequestBody CrearGuiaRequest request) {
		GuiaDespachoResponse response = guiaDespachoService.crearGuia(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/{id}/subir")
	public ResponseEntity<GuiaDespachoResponse> subirAS3(@PathVariable String id) {
		return ResponseEntity.ok(guiaDespachoService.subirAS3(id));
	}

	@GetMapping("/{id}/descargar")
	public ResponseEntity<byte[]> descargar(@PathVariable String id) {

		byte[] contenido = guiaDespachoService.descargar(id);
		String filename = "guia-" + id + ".pdf";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(contenido);
	}

	@PutMapping("/{id}")
	public ResponseEntity<GuiaDespachoResponse> actualizar(
			@PathVariable String id,
			@RequestBody ActualizarGuiaRequest request) {

		return ResponseEntity.ok(guiaDespachoService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable String id) {
		guiaDespachoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<GuiaDespachoResponse>> consultar(
			@RequestParam String transportista,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(defaultValue = "false") boolean incluirS3) {

		List<GuiaDespachoResponse> resultado = incluirS3
				? guiaDespachoService.consultarHistorialS3(transportista, fecha)
				: guiaDespachoService.consultar(transportista, fecha);

		return ResponseEntity.ok(resultado);
	}
}
