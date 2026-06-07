package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String s3Bucket = "ms-guias-bucket";
}
