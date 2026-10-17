# E1 — Autenticación y acceso

Permitir que los usuarios ingresen de forma segura y solo accedan a las funciones de su rol.

Reglas relacionadas: RN-18, RN-19, RN-20 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-01. Iniciar sesión

**Como** usuario registrado, **quiero** iniciar sesión con mi correo y contraseña **para** acceder a las funciones del sistema.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Login correcto | que el usuario está registrado y activo | ingresa correo y contraseña válidos | el sistema genera un token JWT y lo redirige a su pantalla principal |
| 2. Credenciales inválidas | que el usuario ingresa datos incorrectos | intenta iniciar sesión | el sistema muestra "Credenciales inválidas" y no permite el acceso |
| 3. Campos vacíos | que el formulario de login está abierto | deja el correo o la contraseña vacíos | el sistema muestra un mensaje de validación y no envía la petición |
| 4. Usuario inactivo | que el administrador desactivó al usuario | intenta iniciar sesión con datos correctos | el sistema muestra "Credenciales inválidas" y no permite el acceso |

| ID | Capa | Tarea |
|---|---|---|
| T-01 | BD | Crear tablas `rol` y `usuario` (migración V1) |
| T-02 | Backend | Crear endpoint `POST /api/auth/login` y generación de JWT |
| T-03 | Backend | Validar credenciales y devolver 401 con ProblemDetail |
| T-04 | Frontend | Crear pantalla de login con validaciones |
| T-05 | Frontend | Guardar el token en `sessionStorage` y redirigir según rol |

**Notas de implementación**
- El mensaje de error es el mismo para correo inexistente, contraseña incorrecta y usuario inactivo: así no se revela qué correos existen (OWASP).
- El token incluye `sub` (id del usuario), `rol` y `exp`. La duración se configura con `JWT_EXPIRATION_MINUTES`.

---

## HU-02. Registrarme en el sistema

**Como** estudiante, docente o administrativo, **quiero** crear una cuenta **para** poder registrar mis solicitudes.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Registro correcto | que la persona completa nombre, correo y contraseña válidos | envía el formulario | el sistema crea la cuenta con rol `SOLICITANTE` y muestra una confirmación |
| 2. Correo duplicado | que ya existe una cuenta con ese correo | intenta registrarse con él | el sistema muestra "El correo ya está registrado" y no crea la cuenta |
| 3. Contraseña débil | que la persona ingresa una contraseña de menos de 8 caracteres | envía el formulario | el sistema muestra los requisitos de contraseña y no registra |

| ID | Capa | Tarea |
|---|---|---|
| T-06 | Backend | Crear endpoint `POST /api/auth/registro` con DTO y Bean Validation |
| T-07 | Backend | Validar correo único y cifrar la contraseña con BCrypt |
| T-08 | Frontend | Crear formulario de registro con validaciones en JS |

**Notas de implementación**
- El registro público **siempre** asigna `SOLICITANTE` (RN-18). Los demás roles solo los asigna el administrador.

---

## HU-03. Acceder solo a lo que mi rol permite

**Como** administrador del sistema, **quiero** que cada rol acceda solo a sus funciones **para** proteger la información y el flujo de atención.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Acceso permitido | que el usuario tiene el rol requerido | accede a una funcionalidad de su rol | el sistema se la muestra normalmente |
| 2. Acceso denegado | que un solicitante está autenticado | intenta asignar solicitudes (función de supervisor) | el sistema responde 403 y no ejecuta la acción |
| 3. Sin sesión | que el usuario no tiene un token válido | intenta entrar a una ruta protegida | el sistema responde 401 y el frontend lo redirige al login |

| ID | Capa | Tarea |
|---|---|---|
| T-09 | Backend | Configurar filtro JWT y autorización por rol en los endpoints |
| T-10 | Frontend | Crear rutas protegidas y menú según rol |

**Notas de implementación**
- La autorización por rol (`@PreAuthorize`) no alcanza para todo: la validación de propiedad (por ejemplo, "esta solicitud es mía") va en el servicio (RN-06).
