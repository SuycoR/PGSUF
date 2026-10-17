package pe.edu.unmsm.servicios.usuario.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.repository.AreaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.RolRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.usuario.Correos;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

/** Single place where users are created, so every path applies the same rules. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            AreaRepository areaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario crear(String nombre, String correo, String password, String rolNombre, Integer areaId) {
        String correoNormalizado = Correos.normalizar(correo);
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw BusinessException.conflict("CORREO_DUPLICADO", "Correo ya registrado",
                    "El correo ya está registrado.");
        }

        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> BusinessException.unprocessable("ROL_INVALIDO", "Rol inválido",
                        "El rol '" + rolNombre + "' no existe."));
        Area area = resolverArea(rol, areaId);

        Usuario usuario = new Usuario(nombre.trim(), correoNormalizado, passwordEncoder.encode(password), rol, area);
        return usuarioRepository.save(usuario);
    }

    /** RN-10: area is mandatory for TECNICO and PERSONAL_AREA, and must be empty for the other roles. */
    private Area resolverArea(Rol rol, Integer areaId) {
        if (!rol.requiereArea()) {
            if (areaId != null) {
                throw BusinessException.unprocessable("AREA_NO_PERMITIDA", "Área no permitida",
                        "El rol " + rol.getNombre() + " no pertenece a un área.");
            }
            return null;
        }
        if (areaId == null) {
            throw BusinessException.unprocessable("AREA_OBLIGATORIA", "Área obligatoria",
                    "El rol " + rol.getNombre() + " requiere un área.");
        }
        return areaRepository.findById(areaId)
                .filter(Area::isActivo)
                .orElseThrow(() -> BusinessException.unprocessable("AREA_INVALIDA", "Área inválida",
                        "El área indicada no existe o está inactiva."));
    }
}
