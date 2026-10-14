--liquibase formatted sql
-- RF-004b: 5 intentos fallidos de inicio de sesión dentro de 15 minutos bloquean la cuenta 15
-- minutos. Una fila por cuenta con fallos recientes o bloqueada; se borra al iniciar sesión bien o
-- al restablecer la contraseña. Va aparte de app_user para no tocar la cuenta en cada intento.

--changeset lavarapido:security-034-login-lockout
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'login_lockout'
CREATE TABLE [security].login_lockout (
    user_id         BIGINT       NOT NULL,
    failed_count    INT          NOT NULL CONSTRAINT df_login_lockout_count DEFAULT 0,
    first_failed_at DATETIME2(3) NULL,
    locked_until    DATETIME2(3) NULL,
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_login_lockout_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_login_lockout PRIMARY KEY (user_id),
    CONSTRAINT fk_login_lockout_user FOREIGN KEY (user_id) REFERENCES [security].app_user(user_id),
    CONSTRAINT ck_login_lockout_count CHECK (failed_count >= 0 AND failed_count < 5)
);
--rollback DROP TABLE [security].login_lockout;
