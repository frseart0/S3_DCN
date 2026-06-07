package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	public File saveToEfs(String relativePath, MultipartFile multipartFile) throws IOException {
		File dest = resolveFile(relativePath);
		createParentDirs(dest);
		multipartFile.transferTo(dest);
		log.info("Archivo guardado en EFS: {}", dest.getAbsolutePath());
		return dest;
	}

	public File saveBytes(String relativePath, byte[] content) throws IOException {
		File dest = resolveFile(relativePath);
		createParentDirs(dest);
		Files.write(dest.toPath(), content);
		log.info("Contenido guardado en EFS: {}", dest.getAbsolutePath());
		return dest;
	}

	public byte[] readBytes(String relativePath) throws IOException {
		Path path = resolveFile(relativePath).toPath();
		if (!Files.exists(path)) {
			throw new IOException("Archivo no encontrado en EFS: " + relativePath);
		}
		return Files.readAllBytes(path);
	}

	public void delete(String relativePath) throws IOException {
		Path path = resolveFile(relativePath).toPath();
		if (Files.exists(path)) {
			Files.delete(path);
			log.info("Archivo eliminado de EFS: {}", path);
		}
	}

	public String buildRelativePath(String transportista, String guiaId) {
		return transportista.replaceAll("[\\\\/]+", "-") + "/guia-" + guiaId + ".pdf";
	}

	private File resolveFile(String relativePath) {
		return new File(efsPath, relativePath);
	}

	private void createParentDirs(File dest) {
		File parentDir = dest.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
	}
}
