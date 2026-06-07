package cl.duoc.ejemplo.ms.administracion.archivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import cl.duoc.ejemplo.ms.administracion.archivos.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class MsAdministracionArchivosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAdministracionArchivosApplication.class, args);
	}
}
