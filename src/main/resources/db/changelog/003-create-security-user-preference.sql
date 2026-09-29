--liquibase formatted sql
-- Movida de `settings` a `security` por ADR-009.

--changeset lavarapido:security-003-user-preference
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'user_preference'
CREATE TABLE [security].user_preference (
    user_preference_id    BIGINT       IDENTITY(1,1) NOT NULL,
    user_id               BIGINT       NOT NULL,
    theme                 NVARCHAR(20) NOT NULL CONSTRAINT df_pref_theme DEFAULT N'light',
    [language]            NVARCHAR(5)  NOT NULL CONSTRAINT df_pref_lang DEFAULT N'es',
    notifications_enabled BIT          NOT NULL CONSTRAINT df_pref_notif DEFAULT 1,
    created_at            DATETIME2(3) NOT NULL CONSTRAINT df_pref_created DEFAULT SYSUTCDATETIME(),
    created_by            BIGINT       NULL,
    updated_at            DATETIME2(3) NULL,
    updated_by            BIGINT       NULL,
    deleted_at            DATETIME2(3) NULL,
    deleted_by            BIGINT       NULL,
    row_version           INT          NOT NULL CONSTRAINT df_pref_rv DEFAULT 1,
    CONSTRAINT pk_user_preference PRIMARY KEY (user_preference_id),
    CONSTRAINT uq_user_preference_user UNIQUE (user_id),
    CONSTRAINT fk_user_preference_user FOREIGN KEY (user_id) REFERENCES [security].app_user(user_id),
    CONSTRAINT ck_pref_theme CHECK (theme IN (N'light', N'dark')),
    CONSTRAINT ck_pref_language CHECK ([language] IN (N'es', N'en', N'fr', N'pt'))
);

--changeset lavarapido:security-003-tr-touch-user-preference splitStatements:false
--comment: updated_at y row_version nunca los escribe el código de la aplicación (modeling-conventions.md)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.triggers WHERE name = 'tr_touch_user_preference'
CREATE TRIGGER [security].tr_touch_user_preference
ON [security].user_preference
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;

    UPDATE p
       SET updated_at  = SYSUTCDATETIME(),
           row_version = p.row_version + 1
      FROM [security].user_preference p
      JOIN inserted i ON i.user_preference_id = p.user_preference_id;
END
