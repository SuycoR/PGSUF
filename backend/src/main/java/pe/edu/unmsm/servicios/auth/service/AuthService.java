package pe.edu.unmsm.servicios.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.auth.dto.LoginRequest;
import pe.edu.unmsm.servicios.auth.dto.LoginResponse;
import pe.edu.unmsm.servicios.auth.dto.RegistroRequest;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.usuario.Correos;
import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // Compared against when the email does not exist, so both failures take similar time
    // and the response time does not reveal which emails are registered.
    private final String hashSenuelo;

    public AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashSenuelo = passwordEncoder.encode("decoy-password-for-timing");
    }

    /** HU-01. Same error for unknown email, wrong password and inactive user (RN-19). */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(Correos.normalizar(request.correo())).orElse(null);

        String hash = usuario == null ? hashSenuelo : usuario.getPasswordHash();
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (usuario == null || !passwordOk || !usuario.isActivo()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                    "Credenciales inválidas", "Correo o contraseña incorrectos.");
        }
        return new LoginResponse(jwtService.generar(usuario), UsuarioResponse.de(usuario));
    }

    /** HU-02. Public sign-up always creates a SOLICITANTE (RN-18). */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        Usuario usuario = usuarioService.crear(request.nombre(), request.correo(), request.password(), Rol.SOLICITANTE, null);
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> BusinessException.notFound("Usuario no encontrado", "El usuario ya no existe."));
    }
}
