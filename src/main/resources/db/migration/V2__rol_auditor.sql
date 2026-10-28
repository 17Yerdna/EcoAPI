INSERT INTO rol(nombre) VALUES ('AUDITOR');
INSERT INTO rol_privilegio(rol,privilegio) VALUES ('AUDITOR','PUNTO_LEER'),('AUDITOR','PESAJE_LEER');
INSERT INTO usuario(usuario,contrasena,rol) VALUES ('auditor1',crypt('Eco2026*',gen_salt('bf',10)),'AUDITOR');
INSERT INTO ruta_usuario(ruta_id,usuario_id) SELECT r.id,u.id FROM ruta r CROSS JOIN usuario u WHERE u.usuario='auditor1';
