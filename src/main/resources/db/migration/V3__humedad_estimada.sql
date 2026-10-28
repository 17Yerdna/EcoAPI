ALTER TABLE pesaje ADD COLUMN humedad_estimada double precision NOT NULL DEFAULT 0 CHECK(humedad_estimada>=0 AND humedad_estimada<=100);
UPDATE pesaje SET humedad_estimada=12.5 WHERE material='PAPEL';
