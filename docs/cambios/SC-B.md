# SC-B — AUDITOR

Migración V2 agrega rol AUDITOR y usuario auditor1/Eco2026* con PUNTO_LEER y PESAJE_LEER. No modifica filtro ni controlador: el servicio verifica los privilegios declarados en la base.

Prueba HTTP: lectura de puntos/pesajes=200, creación/cierre=403. Suite completa: 14 pruebas, 0 fallos (`sc-b-green.log`).
