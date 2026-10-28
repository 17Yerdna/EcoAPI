# EcoAPI — EcoRecicla

Servicio Spring Boot/Kotlin del Examen Unidad 2, Versión B. PostgreSQL real, JWT firmado, privilegios por operación y reglas transaccionales RN01–RN04.

## Ejecución

Requisitos: Docker Desktop iniciado. Copiar `.env.example` a `.env` y asignar `JWT_SECRET` aleatorio de al menos 64 caracteres. Nunca publicar `.env`.

```powershell
Copy-Item .env.example .env
# Editar .env: JWT_SECRET debe contener un secreto aleatorio de 64+ caracteres.
docker compose up --build
```

API: `http://localhost:8080/api/v1`. Salud: `/actuator/health`. Swagger: `/swagger-ui/index.html`. Contrato: `/v3/api-docs`.

El contenedor API corre como usuario sin privilegios. PostgreSQL usa volumen persistente; el puerto de desarrollo es 5434. Para reiniciar SOLO los datos de demostración: `docker compose down -v` (borra el volumen de este proyecto).

## Usuarios semilla

| Usuario | Contraseña demostración | Rol |
|---|---|---|
| operario1 | Eco2026* | OPERARIO |
| supervisor1 | Eco2026* | SUPERVISOR |
| admin | Eco2026* | ADMINISTRADOR |
| auditor1 | Eco2026* | AUDITOR (SC-B, solo lectura) |

Son credenciales públicas del examen, no de producción. Las contraseñas se guardan como BCrypt. Semilla: 12 puntos/3 zonas, 2 rutas, 15 pesajes; la ruta de hoy tiene 6 puntos y está abierta. Las fechas de semilla se fijan al primer arranque de la base, zona America/Lima.

## Contrato

Login: `POST /auth/login` con `usuario` y `contrasena`. Devuelve `token`, `usuario`, `rol` y `privilegios`. Las demás operaciones requieren `Authorization: Bearer <token>`; expira en una hora.

| Método | Ruta | Privilegio |
|---|---|---|
| GET | /rutas/dia?fecha=YYYY-MM-DD | PUNTO_LEER |
| GET | /puntos?pagina=0&tamanio=20&zona=Centro | PUNTO_LEER |
| POST | /puntos | PUNTO_GESTIONAR |
| POST | /rutas/{rutaId}/puntos/{puntoId} | PUNTO_GESTIONAR |
| GET | /puntos/{id}/pesajes | PESAJE_LEER |
| POST | /pesajes | PESAJE_CREAR |
| PUT | /pesajes/{id} | PESAJE_EDITAR |
| DELETE | /pesajes/{id}?motivo=... | PESAJE_ANULAR |
| PUT | /rutas/{rutaId}/puntos/{puntoId}/omision | PESAJE_CREAR |
| POST | /rutas/{id}/cierre | RUTA_CERRAR |
| GET | /usuarios/me | Autenticado |

Crear pesaje: `puntoId`, `material`, `pesoBruto`, `tara`, `observacion` opcional y `claveCliente` UUID opcional para reintentos idempotentes. Nunca enviar `pesoNeto`, `operario` ni fecha. Corrección: material/bruto/tara/observación. Omisión: `{ "motivo": "Empresa cerrada" }`.

Los servicios validan privilegios, pertenencia a ruta y reglas. RN01: ruta cerrada=409. RN02: operario edita propios/mismo día=403 si incumple. RN03: neto calculado por servidor y positivo=400 por campo. RN04: cierre incompleto=409 con cantidad. Anulación conserva la fila y devuelve 204 vacío.

Paginación: `contenido,pagina,tamanio,totalElementos,totalPaginas,ultima`. Error: `timestamp,status,error,message,path,validationErrors`. No se exponen stack traces.

## Pruebas

Requisitos para tests locales: Java21+ y PostgreSQL Docker iniciado.

```powershell
docker compose up -d db
.\mvnw.cmd test
```

En este equipo usar `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` si el Java Oracle del PATH no responde. Los tests HTTP usan puerto aleatorio y una clave JWT exclusiva de pruebas; no requieren `.env`. La suite trabaja contra la base local de demostración y crea pesajes adicionales.

Importar `docs/api/EcoAPI.postman_collection.json` en Postman y ejecutar en orden contra una semilla de hoy. Tokens se guardan en variables durante el run; no se exportan credenciales privadas.

## Arquitectura y continuidad

`web` delega en `servicio`; JPA persiste en PostgreSQL y Flyway controla esquema/semillas. La autorización reside en el servicio y JWT valida firma/expiración. Bloqueo de ruta serializa alta/cierre. Los planes están en `docs/superpowers`. En la carpeta compartida del proyecto: `CONTEXTO_PARA_IA.md` y `tasks/handoff.md` registran cómo retomar.

No se retrofechan commits. SC-B agrega AUDITOR; SC-C agrega humedadEstimada (0..100) al contrato y PostgreSQL. Los cambios y sus pruebas están en docs/cambios. La entrega se identifica mediante v1.0-unidad2.
