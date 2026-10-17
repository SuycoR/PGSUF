package pe.edu.unmsm.servicios.solicitud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoGuardado;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoValidado;

class AlmacenamientoArchivosTest {

    static final byte[] PDF = "%PDF-1.4\n%contenido".getBytes();
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};

    @TempDir
    Path base;

    private AlmacenamientoArchivos almacenamiento;

    @BeforeEach
    void setUp() {
        almacenamiento = new AlmacenamientoArchivos(new AppProperties(new AppProperties.Jwt("x", 60),
                new AppProperties.Cors(List.of()), base.toString(), new AppProperties.Admin("", "")));
    }

    private static MockMultipartFile archivo(String nombre, String contentType, byte[] contenido) {
        return new MockMultipartFile("archivos", nombre, contentType, contenido);
    }

    @Test
    void detectsPdfPngAndJpgByContent() {
        assertThat(almacenamiento.validar(archivo("a.pdf", "application/pdf", PDF)).tipoMime()).isEqualTo("application/pdf");
        assertThat(almacenamiento.validar(archivo("b.png", "image/png", PNG)).tipoMime()).isEqualTo("image/png");
        assertThat(almacenamiento.validar(archivo("c.jpg", "image/jpeg", JPG)).tipoMime()).isEqualTo("image/jpeg");
    }

    @Test
    void theTypeComesFromTheContentNotFromTheNameOrBrowser() {
        ArchivoValidado validado = almacenamiento.validar(archivo("foto.pdf", "application/pdf", PNG));

        assertThat(validado.tipoMime()).isEqualTo("image/png");
        assertThat(validado.extension()).isEqualTo("png");
    }

    @Test
    void rejectsAnExecutableRenamedAsPdf() {
        assertThatThrownBy(() -> almacenamiento.validar(archivo("virus.pdf", "application/pdf", EXE)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("ARCHIVO_INVALIDO");
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        byte[] grande = Arrays.copyOf(PDF, (int) AlmacenamientoArchivos.TAMANO_MAXIMO + 1);

        assertThatThrownBy(() -> almacenamiento.validar(archivo("vacio.pdf", "application/pdf", new byte[0])))
                .hasMessageContaining("vacío");
        assertThatThrownBy(() -> almacenamiento.validar(archivo("grande.pdf", "application/pdf", grande)))
                .hasMessageContaining("5 MB");
    }

    @Test
    void storesUnderAGeneratedNameAndCanDeleteIt() throws Exception {
        ArchivoValidado validado = almacenamiento.validar(archivo("../../etc/passwd.pdf", "application/pdf", PDF));

        ArchivoGuardado guardado = almacenamiento.guardar(15L, validado);

        assertThat(guardado.ruta()).matches("solicitudes/15/[0-9a-f-]{36}\\.pdf");
        assertThat(guardado.nombreOriginal()).isEqualTo("passwd.pdf");
        Path enDisco = base.resolve(guardado.ruta());
        assertThat(Files.readAllBytes(enDisco)).isEqualTo(PDF);

        almacenamiento.borrar(guardado.ruta());
        assertThat(enDisco).doesNotExist();
    }

    @Test
    void refusesToReadOutsideTheUploadDirectory() {
        assertThatThrownBy(() -> almacenamiento.leer("../fuera.txt")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safeNameKeepsOnlyTheLastSegment() {
        assertThat(AlmacenamientoArchivos.nombreSeguro("C:\\Users\\ana\\nota.pdf")).isEqualTo("nota.pdf");
        assertThat(AlmacenamientoArchivos.nombreSeguro("  ")).isEqualTo("archivo");
        assertThat(AlmacenamientoArchivos.nombreSeguro("a\u0000b.pdf")).isEqualTo("ab.pdf");
    }
}
