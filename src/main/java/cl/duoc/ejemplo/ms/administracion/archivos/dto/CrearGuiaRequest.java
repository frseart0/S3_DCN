package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearGuiaRequest {

	@NotBlank(message = "El transportista es obligatorio")
	private String transportista;

	@NotBlank(message = "El identificador del pedido es obligatorio")
	private String pedidoId;

	private String descripcion;
}
