package pe.edu.unmsm.servicios.auth.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import pe.edu.unmsm.servicios.auth.dto.LoginRequest;
import pe.edu.unmsm.servicios.auth.dto.LoginResponse;
import pe.edu.unmsm.servicios.auth.dto.RegistroRequest;
import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.auth.service.AuthService;
import pe.edu.unmsm.servicios.auth.service.LoginRateLimiter;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "HU-01, HU-02, HU-03")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un token JWT (HU-01)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "400", description = "Campos requeridos ausentes o mal formados", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario inactivo (RN-19)", content = @Content),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos (Rate Limiter)", content = @Content)
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Use remoteAddr directly so untrusted X-Forwarded-For headers cannot spoof the limiter key;
        // when behind a trusted proxy, enable server.forward-headers-strategy so remoteAddr reflects it.
        String ip = httpRequest.getRemoteAddr();
        loginRateLimiter.verificarPermitido(ip, request.correo());
        try {
            LoginResponse response = authService.login(request);
            loginRateLimiter.registrarExito(ip, request.correo());
            return response;
        } catch (BusinessException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                loginRateLimiter.registrarFallo(ip, request.correo());
            }
            throw ex;
        }
    }

    @PostMapping("/registro")
    @Operation(summary = "Crea una cuenta con rol Solicitante (HU-02, RN-18)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado", content = @Content)
    })
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse usuario = authService.registrar(request);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(usuario);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado (HU-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.perfil(usuario.id());
    }
}
