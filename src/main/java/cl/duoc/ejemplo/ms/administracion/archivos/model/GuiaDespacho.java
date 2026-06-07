package cl.duoc.ejemplo.ms.administracion.archivos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaDespacho {

	private String id;
	private String transportista;
	private String pedidoId;
	private String descripcion;
	private LocalDate fecha;
	private String efsRelativePath;
	private String s3Key;
	private boolean subidaS3;
	private LocalDateTime creadaEn;
	private LocalDateTime actualizadaEn;
}
