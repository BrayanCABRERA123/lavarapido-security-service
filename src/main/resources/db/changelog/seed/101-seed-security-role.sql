--liquibase formatted sql
-- Los tres roles del sistema. Idempotente: solo inserta los que falten.

--changeset lavarapido:security-101-seed-roles
INSERT INTO [security].[role] (code, name)
SELECT N'ADMIN', N'Administrador'
WHERE NOT EXISTS (SELECT 1 FROM [security].[role] WHERE code = N'ADMIN');

INSERT INTO [security].[role] (code, name)
SELECT N'OPERATOR', N'Operario'
WHERE NOT EXISTS (SELECT 1 FROM [security].[role] WHERE code = N'OPERATOR');

INSERT INTO [security].[role] (code, name)
SELECT N'CLIENT', N'Cliente'
WHERE NOT EXISTS (SELECT 1 FROM [security].[role] WHERE code = N'CLIENT');
