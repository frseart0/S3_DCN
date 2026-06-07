package cl.duoc.ejemplo.ms.administracion.archivos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import cl.duoc.ejemplo.ms.administracion.archivos.model.GuiaDespacho;

@Repository
public class GuiaDespachoRepository {

	private final Map<String, GuiaDespacho> guias = new ConcurrentHashMap<>();

	public GuiaDespacho save(GuiaDespacho guia) {
		guias.put(guia.getId(), guia);
		return guia;
	}

	public Optional<GuiaDespacho> findById(String id) {
		return Optional.ofNullable(guias.get(id));
	}

	public List<GuiaDespacho> findByTransportistaAndFecha(String transportista, LocalDate fecha) {
		return guias.values().stream()
				.filter(g -> g.getTransportista().equalsIgnoreCase(transportista))
				.filter(g -> g.getFecha().equals(fecha))
				.collect(Collectors.toList());
	}

	public void deleteById(String id) {
		guias.remove(id);
	}
}
