# SC-C — Humedad estimada

Migración V3 agrega humedad_estimada con rango 0..100 y actualiza los datos semilla. Entidad, request, respuesta y corrección conservan el campo. El servicio sigue calculando el neto sin aceptar pesoNeto del cliente.

Prueba HTTP: registrar 23.5%, comprobar respuesta y columna PostgreSQL, rechazar 101% con 400. Suite: 15 tests sin fallos (`sc-c-green.log`).
