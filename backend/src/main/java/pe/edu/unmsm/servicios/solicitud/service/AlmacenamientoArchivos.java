package pe.edu.unmsm.servicios.solicitud.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.config.AppProperties;

/**
 * RN-12: evidence files on disk under UPLOAD_DIR.
 * The type is detected from the first bytes of the content, never from the name or the browser's Content-Type,
 * and files are stored under generated names so user input never reaches the file system path.
 */
@Component
public class AlmacenamientoArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoArchivos.class);

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    /** Allowed formats and their signatures (magic bytes). */
    enum TipoArchivo {
        PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
        PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
        JPG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        final String mime;
        final String extension;
        final byte[] firma;

        TipoArchivo(String mime, String extension, byte[] firma) {
            this.mime = mime;
            this.extension = extension;
            this.firma = firma;
        }

        static TipoArchivo detectar(byte[] cabecera) {
            for (TipoArchivo tipo : values()) {
                if (cabecera.length >= tipo.firma.length
                        && Arrays.equals(Arrays.copyOf(cabecera, tipo.firma.length), tipo.firma)) {
                    return tipo;
                }
            }
            return null;
        }
    }

    /** A file that passed validation, ready to be written. */
    public record ArchivoValidado(MultipartFile archivo, String nombreOriginal, String tipoMime, String extension) {
    }

    /** A file written to disk; {@code ruta} is relative to the upload directory. */
    public record ArchivoGuardado(String ruta, String nombreOriginal, String tipoMime, int tamanoBytes) {
    }

    private final Path base;

    public AlmacenamientoArchivos(AppProperties properties) {
        this.base = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    }

    public ArchivoValidado validar(MultipartFile archivo) {
        String nombre = nombreSeguro(archivo.getOriginalFilename());
        if (archivo.isEmpty()) {
            throw invalido(nombre, "está vacío");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw invalido(nombre, "supera los 5 MB");
        }
        TipoArchivo tipo;
        try (InputStream in = archivo.getInputStream()) {
            tipo = TipoArchivo.detectar(in.readNBytes(8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (tipo == null) {
            throw invalido(nombre, "no es un PDF, JPG o PNG válido");
        }
        return new ArchivoValidado(archivo, nombre, tipo.mime, tipo.extension);
    }

    public ArchivoGuardado guardar(Long solicitudId, ArchivoValidado validado) {
        String ruta = "solicitudes/" + solicitudId + "/" + UUID.randomUUID() + "." + validado.extension();
        Path destino = resolver(ruta);
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = validado.archivo().getInputStream()) {
                Files.copy(in, destino);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store evidence file", e);
        }
        return new ArchivoGuardado(ruta, validado.nombreOriginal(), validado.tipoMime(), (int) validado.archivo().getSize());
    }

    public Resource leer(String ruta) {
        return new PathResource(resolver(ruta));
    }

    public void borrar(String ruta) {
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException e) {
            // Best effort cleanup: a leftover file is harmless and the log shows it.
            log.warn("Could not delete evidence file {}", ruta, e);
        }
    }

    /** Defense in depth: even generated paths must stay inside the upload directory. */
    private Path resolver(String ruta) {
        Path destino = base.resolve(ruta).normalize();
        if (!destino.startsWith(base)) {
            throw new IllegalArgumentException("Path outside upload directory");
        }
        return destino;
    }

    /** Keeps only the last path segment and drops control characters; used for display only. */
    static String nombreSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "archivo";
        }
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (nombre.isEmpty()) {
            return "archivo";
        }
        return nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre;
    }

    private static BusinessException invalido(String nombre, String motivo) {
        return BusinessException.unprocessable("ARCHIVO_INVALIDO", "Archivo no permitido",
                "El archivo '" + nombre + "' " + motivo + ". Se aceptan PDF, JPG o PNG de hasta 5 MB.");
    }
}
