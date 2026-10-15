--liquibase formatted sql
-- Fecha de registro de cada cuenta (Gestión > Usuarios). Antes no se guardaba y la pantalla mostraba
-- el último ingreso. Las cuentas nuevas la toman al crearse. Para las que ya existían no hay un dato
-- exacto (audit_log no tiene registros): se usa su primera sesión, o la fecha de esta migración si
-- nunca entraron.

--changeset lavarapido:security-036-app-user-created-at
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('security.app_user') AND name = 'created_at'
ALTER TABLE [security].app_user ADD
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_app_user_created_at DEFAULT SYSUTCDATETIME();
--rollback ALTER TABLE [security].app_user DROP CONSTRAINT df_app_user_created_at; ALTER TABLE [security].app_user DROP COLUMN created_at;

--changeset lavarapido:security-036-app-user-created-at-backfill
UPDATE u SET created_at = s.first_session
FROM [security].app_user u
JOIN (SELECT user_id, MIN(started_at) AS first_session FROM [security].user_session GROUP BY user_id) s
    ON s.user_id = u.user_id
WHERE s.first_session < u.created_at;
--rollback SELECT 1;
