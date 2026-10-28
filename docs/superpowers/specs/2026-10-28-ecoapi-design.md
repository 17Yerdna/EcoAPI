# Diseño de EcoAPI para el Examen de Unidad 2

## Intención

EcoAPI será el servicio propio del caso EcoRecicla. Custodiará usuarios, roles, privilegios, puntos de acopio, rutas y pesajes; aplicará las reglas RN-01 a RN-04; y será la única autoridad para autenticación, autorización y cálculo del peso neto.

## Objetivos

- Cumplir RF-B01 a RF-B07 y el contrato común de paginación y errores de la Versión B.
- Levantar servicio y PostgreSQL desde el repositorio con un único comando.
- Proteger todas las rutas salvo login con JWT firmado durante una hora y authorities por privilegio.
- Persistir datos reales en PostgreSQL, crear el esquema con Flyway y cargar todos los datos semilla del anexo.
- Mantener controladores delgados; reglas y transacciones viven en `servicio`.

## No objetivos

- Renovación de JWT, despliegue público, panel web o microservicios.
- CRUD de usuarios desde EcoRutas. Sí se implementan la creación de puntos y su asignación a rutas porque son el contrato explícito de `PUNTO_GESTIONAR`.

## Enfoques considerados

1. **Spring Boot + PostgreSQL con Docker Compose (elegido).** `docker compose up --build` levanta ambos componentes y satisface el arranque con un comando.
2. Spring Boot en host + PostgreSQL en Docker. Es cómodo durante desarrollo, pero requiere dos procesos para la demostración.
3. H2. Se descarta porque la entrega exige una base relacional del caso y PostgreSQL ya está disponible mediante Docker.

## Tecnologías fijadas

- Kotlin/JVM 2.4.10, Java 21, Maven Wrapper y Spring Boot 4.0.7, siguiendo el material local del curso.
- Spring Web MVC, Validation, Data JPA, Security y Flyway.
- PostgreSQL 17 Alpine y driver JDBC PostgreSQL.
- JJWT 0.12.6 y springdoc OpenAPI 3.0.2.
- Testcontainers PostgreSQL, Spring Boot Test y MockMvc.

## Contrato REST elegido

Todas las rutas viven bajo `/api/v1` y todo error usa `timestamp`, `status`, `error`, `message`, `path` y `validationErrors`.

| Operación | Ruta | Privilegio | Respuestas |
|---|---|---|---|
| Login | `POST /auth/login` | Público | 200, 401 |
| Ruta del día | `GET /rutas/dia?fecha=YYYY-MM-DD` | `PUNTO_LEER` | 200, 401, 403, 404 |
| Puntos de acopio | `GET /puntos?pagina=0&tamanio=20&zona=` | `PUNTO_LEER` | 200, 401, 403 |
| Crear punto | `POST /puntos` | `PUNTO_GESTIONAR` | 201, 400, 401, 403, 409 |
| Asignar punto a ruta | `POST /rutas/{rutaId}/puntos/{puntoId}` | `PUNTO_GESTIONAR` | 201, 401, 403, 404, 409 |
| Pesajes del punto | `GET /puntos/{id}/pesajes?pagina=0&tamanio=20` | `PESAJE_LEER` | 200, 401, 403, 404 |
| Registrar pesaje | `POST /pesajes` | `PESAJE_CREAR` | 201, 400, 401, 403, 404, 409 |
| Corregir pesaje | `PUT /pesajes/{id}` | `PESAJE_EDITAR` | 200, 400, 401, 403, 404, 409 |
| Anular pesaje | `DELETE /pesajes/{id}?motivo=...` | `PESAJE_ANULAR` | 204, 400, 401, 403, 404 |
| Cerrar ruta | `POST /rutas/{id}/cierre` | `RUTA_CERRAR` | 200, 401, 403, 404, 409 |
| Registrar omisión | `PUT /rutas/{rutaId}/puntos/{puntoId}/omision` | `PESAJE_CREAR` | 200, 400, 401, 403, 404, 409 |
| Usuario actual | `GET /usuarios/me` | Autenticado | 200, 401 |

Los listados usan exactamente `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y `ultima`. `CrearPesajeRequest` contiene `puntoId`, `material`, `pesoBruto`, `tara` y `observacion`; nunca acepta `pesoNeto`, `operario`, estado ni fecha. `ActualizarPesajeRequest` contiene `material`, `pesoBruto`, `tara` y `observacion`.

## Dominio y persistencia

El esquema relacional contiene `usuario`, `rol`, `privilegio`, `usuario_rol`, `rol_privilegio`, `punto_acopio`, `ruta`, `ruta_usuario`, `ruta_punto` y `pesaje`. `ruta_usuario` asigna operarios y supervisores a una ruta; `GET /rutas/dia` siempre resuelve por usuario autenticado y fecha. Al registrar un pesaje, el servicio obtiene la ruta mediante la combinación usuario-fecha-punto, sin confiar en un `rutaId` enviado por el cliente. `ruta_punto` guarda la secuencia y un motivo de omisión opcional, modificable por la operación de omisión. `pesaje` conserva filas anuladas mediante `anulado`, `motivo_anulacion`, `anulado_por` y `anulado_en`.

Flyway carga:

- 12 puntos en 3 zonas.
- Ruta abierta del día con 6 puntos y ruta cerrada del día anterior.
- 15 pesajes: uno anulado, uno de otro operario y puntos sin pesar en la ruta abierta.
- `operario1`, `supervisor1` y `admin` con contraseña demostrable `Eco2026*`, persistida solo como BCrypt.

## Seguridad y reglas

- El JWT transporta ID de usuario, username y privilegios, dura 3600 segundos y se firma con secreto de entorno de al menos 64 bytes.
- El filtro rechaza token ausente, alterado o vencido con 401; `@PreAuthorize`/authorities rechaza falta de privilegio con 403.
- RN-01 impide registrar o corregir en ruta cerrada con 409.
- RN-02 permite al operario corregir solo pesajes propios del mismo día; supervisor corrige todos; la violación devuelve 403.
- RN-03 calcula `pesoNeto = pesoBruto - tara` en el servicio y exige valor positivo; devuelve 400 en `pesoBruto` o `tara` según corresponda.
- RN-04 impide cerrar mientras un punto no tenga pesaje válido o motivo de omisión; devuelve 409 con la cantidad pendiente.
- Ningún controlador compara nombres de rol ni calcula reglas.
- La zona de negocio es `America/Lima`: el mismo día de RN-02 y la ruta del día se calculan allí. PostgreSQL persiste instantes en UTC y el API devuelve fecha/hora ISO-8601 con desplazamiento.

## Ejecución y documentación

`docker compose up --build` es el comando oficial de demostración. El contenedor de servicio espera el healthcheck de PostgreSQL, ejecuta Flyway y expone el puerto 8080. El README documenta rutas, DTO, códigos, roles, privilegios, variables de entorno, Swagger, usuarios semilla y comandos de pruebas.

## Pruebas de aceptación

- Login correcto e incorrecto; token ausente, alterado y vencido.
- 403 real para cada operación sensible con un usuario sin privilegio.
- Paginación y filtro por zona.
- RN-01 a RN-04 en servicios y por HTTP.
- Creación 201, corrección 200 y anulación lógica 204 sin cuerpo.
- Error 400 por campo, 404 sin reintento en cliente, 409 por reglas y 500 sin filtrar stack trace.
- Seeds completos y arranque limpio desde una base vacía.

## Trazabilidad

El repositorio usa `main`, `develop` y ramas `feature/base-postgresql`, `feature/seguridad-jwt`, `feature/api-pesajes`, `feature/reglas-rutas` y `feature/pruebas-documentacion`. Cada rama tiene issue, commits atómicos, PR a `develop` y fusión `--no-ff`. Al cierre se fusiona `develop` a `main` y se etiqueta `v1.0-unidad2`.

