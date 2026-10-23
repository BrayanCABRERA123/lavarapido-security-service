--liquibase formatted sql
-- Cuándo la persona aceptó los Términos y Condiciones y la Política de Tratamiento de Datos al
-- registrarse (Ley 1581 de 2012: el responsable debe poder probar la autorización). Antes la web y la
-- app pedían las dos casillas pero el servidor no guardaba nada. Cuentas anteriores y cuentas creadas
-- por un administrador quedan en NULL.

--changeset lavarapido:security-039-app-user-legal-consent
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('security.app_user') AND name = 'terms_accepted_at'
ALTER TABLE [security].app_user ADD
    terms_accepted_at       DATETIME2(3) NULL,
    data_policy_accepted_at DATETIME2(3) NULL;
--rollback ALTER TABLE [security].app_user DROP COLUMN terms_accepted_at, data_policy_accepted_at;
