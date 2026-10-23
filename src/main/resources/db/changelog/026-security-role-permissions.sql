--liquibase formatted sql
-- Permisos asignables a los 3 roles fijos (Gestion > Roles). Reemplaza el catalogo de "roles
-- personalizados" de la migracion 023 (creado en esta misma sesion de desarrollo, nunca usado por
-- nadie mas): en vez de un catalogo paralelo sin efecto, esto usa security.permission y
-- security.role_permission, que ya existian desde la migracion 002 pero ningun codigo los leia.
-- Los 3 roles (security.role) siguen fijos (ADR-010): aqui solo se les asignan permisos.

--changeset lavarapido:security-026-drop-custom-role
--comment: El catalogo de roles personalizados se reemplaza por permisos sobre los 3 roles fijos
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'custom_role'
DROP TABLE [security].custom_role;

--changeset lavarapido:security-026-seed-permission
--comment: Catalogo de permisos (los mismos 4 codigos que ya mostraba la pantalla de roles)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM [security].[permission]
INSERT INTO [security].[permission] (code, name, resource, [action]) VALUES
    (N'VIEW_PANELS',    N'Ver paneles',       N'panel',  N'view'),
    (N'CREATE_RECORDS', N'Crear registros',   N'record', N'create'),
    (N'EDIT_DATA',      N'Editar datos',      N'record', N'edit'),
    (N'DELETE',         N'Eliminar',          N'record', N'delete');

--changeset lavarapido:security-026-seed-role-permission
--comment: Permisos por defecto de cada rol fijo (el admin los puede cambiar despues)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM [security].role_permission
INSERT INTO [security].role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM [security].[role] r
CROSS JOIN [security].[permission] p
WHERE r.code = N'ADMIN';

INSERT INTO [security].role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM [security].[role] r
JOIN [security].[permission] p ON p.code IN (N'VIEW_PANELS', N'EDIT_DATA')
WHERE r.code = N'OPERATOR';

INSERT INTO [security].role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM [security].[role] r
JOIN [security].[permission] p ON p.code = N'VIEW_PANELS'
WHERE r.code = N'CLIENT';
