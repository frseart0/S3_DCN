package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaDespachoResponse {

	private String id;
	private String transportista;
	private String pedidoId;
	private String descripcion;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate fecha;

	private String efsRelativePath;
	private String s3Key;
	private boolean subidaS3;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime creadaEn;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime actualizadaEn;
}
