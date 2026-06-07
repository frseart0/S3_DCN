package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.duoc.ejemplo.ms.administracion.archivos.config.AppProperties;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.ActualizarGuiaRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.CrearGuiaRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoResponse;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.GuiaNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.model.GuiaDespacho;
import cl.duoc.ejemplo.ms.administracion.archivos.repository.GuiaDespachoRepository;
import cl.duoc.ejemplo.ms.administracion.archivos.util.S3KeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final GuiaDespachoRepository repository;
	private final EfsService efsService;
	private final AwsS3Service awsS3Service;
	private final AppProperties appProperties;

	public GuiaDespachoResponse crearGuia(CrearGuiaRequest request) {
		String id = UUID.randomUUID().toString().substring(0, 8);
		LocalDateTime now = LocalDateTime.now();
		String efsRelativePath = efsService.buildRelativePath(request.getTransportista(), id);

		byte[] contenido = generarContenidoGuia(id, request);
		try {
			efsService.saveBytes(efsRelativePath, contenido);
		} catch (IOException e) {
			throw new RuntimeException("Error al guardar guía en EFS: " + e.getMessage(), e);
		}

		GuiaDespacho guia = GuiaDespacho.builder()
				.id(id)
				.transportista(request.getTransportista().trim())
				.pedidoId(request.getPedidoId().trim())
				.descripcion(request.getDescripcion())
				.fecha(now.toLocalDate())
				.efsRelativePath(efsRelativePath)
				.s3Key(S3KeyBuilder.buildKey(now.toLocalDate(), request.getTransportista(), id))
				.subidaS3(false)
				.creadaEn(now)
				.actualizadaEn(now)
				.build();

		repository.save(guia);
		log.info("Guía creada: {} para transportista {}", id, request.getTransportista());
		return toResponse(guia);
	}

	public GuiaDespachoResponse subirAS3(String id) {
		GuiaDespacho guia = findGuiaOrThrow(id);

		byte[] contenido;
		try {
			contenido = efsService.readBytes(guia.getEfsRelativePath());
		} catch (IOException e) {
			throw new RuntimeException("Error al leer guía desde EFS: " + e.getMessage(), e);
		}

		awsS3Service.uploadBytes(appProperties.getS3Bucket(), guia.getS3Key(), contenido, "application/pdf");

		guia.setSubidaS3(true);
		guia.setActualizadaEn(LocalDateTime.now());
		repository.save(guia);

		log.info("Guía {} subida a S3 con clave {}", id, guia.getS3Key());
		return toResponse(guia);
	}

	public byte[] descargar(String id) {
		GuiaDespacho guia = findGuiaOrThrow(id);

		if (!guia.isSubidaS3()) {
			try {
				return efsService.readBytes(guia.getEfsRelativePath());
			} catch (IOException e) {
				throw new RuntimeException("Error al leer guía desde EFS: " + e.getMessage(), e);
			}
		}

		return awsS3Service.downloadAsBytes(appProperties.getS3Bucket(), guia.getS3Key());
	}

	public GuiaDespachoResponse actualizar(String id, ActualizarGuiaRequest request) {
		GuiaDespacho guia = findGuiaOrThrow(id);
		LocalDateTime now = LocalDateTime.now();

		if (request.getTransportista() != null && !request.getTransportista().isBlank()) {
			guia.setTransportista(request.getTransportista().trim());
		}
		if (request.getPedidoId() != null && !request.getPedidoId().isBlank()) {
			guia.setPedidoId(request.getPedidoId().trim());
		}
		if (request.getDescripcion() != null) {
			guia.setDescripcion(request.getDescripcion());
		}

		guia.setS3Key(S3KeyBuilder.buildKey(guia.getFecha(), guia.getTransportista(), guia.getId()));
		guia.setEfsRelativePath(efsService.buildRelativePath(guia.getTransportista(), guia.getId()));
		guia.setActualizadaEn(now);

		CrearGuiaRequest contenidoRequest = new CrearGuiaRequest(
				guia.getTransportista(), guia.getPedidoId(), guia.getDescripcion());
		byte[] contenido = generarContenidoGuia(guia.getId(), contenidoRequest);

		try {
			efsService.saveBytes(guia.getEfsRelativePath(), contenido);
		} catch (IOException e) {
			throw new RuntimeException("Error al actualizar guía en EFS: " + e.getMessage(), e);
		}

		if (guia.isSubidaS3()) {
			awsS3Service.uploadBytes(appProperties.getS3Bucket(), guia.getS3Key(), contenido, "application/pdf");
		}

		repository.save(guia);
		log.info("Guía {} actualizada", id);
		return toResponse(guia);
	}

	public void eliminar(String id) {
		GuiaDespacho guia = findGuiaOrThrow(id);

		if (guia.isSubidaS3()) {
			awsS3Service.deleteObject(appProperties.getS3Bucket(), guia.getS3Key());
		}

		try {
			efsService.delete(guia.getEfsRelativePath());
		} catch (IOException e) {
			log.warn("No se pudo eliminar archivo EFS para guía {}: {}", id, e.getMessage());
		}

		repository.deleteById(id);
		log.info("Guía {} eliminada", id);
	}

	public List<GuiaDespachoResponse> consultar(String transportista, java.time.LocalDate fecha) {
		return repository.findByTransportistaAndFecha(transportista, fecha).stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<GuiaDespachoResponse> consultarHistorialS3(String transportista, java.time.LocalDate fecha) {
		String prefix = S3KeyBuilder.buildPrefix(fecha, transportista);
		return awsS3Service.listObjectsByPrefix(appProperties.getS3Bucket(), prefix).stream()
				.map(obj -> GuiaDespachoResponse.builder()
						.transportista(transportista)
						.fecha(fecha)
						.s3Key(obj.getKey())
						.subidaS3(true)
						.descripcion("Objeto encontrado en S3")
						.build())
				.collect(Collectors.toList());
	}

	private GuiaDespacho findGuiaOrThrow(String id) {
		return repository.findById(id).orElseThrow(() -> new GuiaNotFoundException(id));
	}

	private byte[] generarContenidoGuia(String id, CrearGuiaRequest request) {
		String contenido = String.format(
				"GUIA DE DESPACHO%n" +
				"=================%n" +
				"ID Guia      : %s%n" +
				"Pedido       : %s%n" +
				"Transportista: %s%n" +
				"Descripcion  : %s%n" +
				"Generada     : %s%n",
				id,
				request.getPedidoId(),
				request.getTransportista(),
				request.getDescripcion() != null ? request.getDescripcion() : "Sin descripcion",
				LocalDateTime.now().format(DATETIME_FORMAT));

		return contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	private GuiaDespachoResponse toResponse(GuiaDespacho guia) {
		return GuiaDespachoResponse.builder()
				.id(guia.getId())
				.transportista(guia.getTransportista())
				.pedidoId(guia.getPedidoId())
				.descripcion(guia.getDescripcion())
				.fecha(guia.getFecha())
				.efsRelativePath(guia.getEfsRelativePath())
				.s3Key(guia.getS3Key())
				.subidaS3(guia.isSubidaS3())
				.creadaEn(guia.getCreadaEn())
				.actualizadaEn(guia.getActualizadaEn())
				.build();
	}
}
