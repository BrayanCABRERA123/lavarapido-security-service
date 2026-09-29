--liquibase formatted sql
-- Mismo DDL de lavarapido-6-services-sqlserver.sql, sección 1 (06-data/models.md, 01 security / 02 audit).

--changeset lavarapido:security-002-person
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'person'
CREATE TABLE [security].person (
    person_id       BIGINT       IDENTITY(1,1) NOT NULL,
    document_number NVARCHAR(20) NOT NULL,
    first_name      NVARCHAR(60) NOT NULL,
    last_name       NVARCHAR(60) NOT NULL,
    phone           NVARCHAR(20)  NULL,
    email           NVARCHAR(120) NULL,
    CONSTRAINT pk_person PRIMARY KEY (person_id),
    CONSTRAINT uq_person_document UNIQUE (document_number)
);

--changeset lavarapido:security-002-app-user
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'app_user'
CREATE TABLE [security].app_user (
    user_id       BIGINT        IDENTITY(1,1) NOT NULL,
    person_id     BIGINT        NOT NULL,
    username      NVARCHAR(60)  NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    is_active     BIT           NOT NULL CONSTRAINT df_app_user_active DEFAULT 1,
    last_login    DATETIME2(3)  NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (user_id),
    CONSTRAINT uq_app_user_person UNIQUE (person_id),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT fk_app_user_person FOREIGN KEY (person_id) REFERENCES [security].person(person_id)
);

--changeset lavarapido:security-002-role
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'role'
CREATE TABLE [security].[role] (
    role_id SMALLINT     IDENTITY(1,1) NOT NULL,
    code    NVARCHAR(30) NOT NULL,
    name    NVARCHAR(60) NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (role_id),
    CONSTRAINT uq_role_code UNIQUE (code)
);

--changeset lavarapido:security-002-permission
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'permission'
CREATE TABLE [security].[permission] (
    permission_id SMALLINT     IDENTITY(1,1) NOT NULL,
    code          NVARCHAR(60) NOT NULL,
    name          NVARCHAR(80) NOT NULL,
    resource      NVARCHAR(60) NOT NULL,
    [action]      NVARCHAR(30) NOT NULL,
    CONSTRAINT pk_permission PRIMARY KEY (permission_id),
    CONSTRAINT uq_permission_code UNIQUE (code)
);

--changeset lavarapido:security-002-role-permission
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'role_permission'
CREATE TABLE [security].role_permission (
    role_permission_id BIGINT   IDENTITY(1,1) NOT NULL,
    role_id            SMALLINT NOT NULL,
    permission_id      SMALLINT NOT NULL,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_permission_id),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES [security].[role](role_id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES [security].[permission](permission_id)
);

--changeset lavarapido:security-002-user-role
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'user_role'
CREATE TABLE [security].user_role (
    user_role_id BIGINT   IDENTITY(1,1) NOT NULL,
    user_id      BIGINT   NOT NULL,
    role_id      SMALLINT NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_role_id),
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES [security].app_user(user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES [security].[role](role_id)
);

--changeset lavarapido:security-002-user-session
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'user_session'
CREATE TABLE [security].user_session (
    user_session_id    BIGINT        IDENTITY(1,1) NOT NULL,
    user_id            BIGINT        NOT NULL,
    refresh_token_hash NVARCHAR(255) NULL,
    started_at         DATETIME2(3)  NOT NULL CONSTRAINT df_session_started DEFAULT SYSUTCDATETIME(),
    expires_at         DATETIME2(3)  NOT NULL,
    revoked_at         DATETIME2(3)  NULL,
    ip_address         NVARCHAR(45)  NULL,
    user_agent         NVARCHAR(300) NULL,
    CONSTRAINT pk_user_session PRIMARY KEY (user_session_id),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES [security].app_user(user_id)
);
CREATE NONCLUSTERED INDEX ix_user_session_user ON [security].user_session (user_id);

--changeset lavarapido:security-002-password-reset-token
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'password_reset_token'
CREATE TABLE [security].password_reset_token (
    password_reset_token_id BIGINT        IDENTITY(1,1) NOT NULL,
    user_id                 BIGINT        NOT NULL,
    token_hash              NVARCHAR(255) NOT NULL,
    requested_at            DATETIME2(3)  NOT NULL CONSTRAINT df_prt_requested DEFAULT SYSUTCDATETIME(),
    expires_at              DATETIME2(3)  NOT NULL,
    used_at                 DATETIME2(3)  NULL,
    ip_address              NVARCHAR(45)  NULL,
    CONSTRAINT pk_password_reset_token PRIMARY KEY (password_reset_token_id),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES [security].app_user(user_id),
    CONSTRAINT ck_password_reset_token_range CHECK (expires_at > requested_at)
);
CREATE NONCLUSTERED INDEX ix_password_reset_token_user ON [security].password_reset_token (user_id);
CREATE NONCLUSTERED INDEX ix_password_reset_token_expires ON [security].password_reset_token (expires_at);

--changeset lavarapido:security-002-audit-log
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'audit' AND t.name = 'audit_log'
CREATE TABLE [audit].audit_log (
    audit_log_id  BIGINT         IDENTITY(1,1) NOT NULL,
    entity        NVARCHAR(60)   NOT NULL,
    entity_id     BIGINT         NOT NULL,
    [action]      NVARCHAR(20)   NOT NULL,
    previous_data NVARCHAR(MAX)  NULL,
    new_data      NVARCHAR(MAX)  NULL,
    changed_by    BIGINT         NULL,
    changed_at    DATETIME2(3)   NOT NULL CONSTRAINT df_audit_log_changed_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_audit_log PRIMARY KEY (audit_log_id)
);
CREATE NONCLUSTERED INDEX ix_audit_log_entity ON [audit].audit_log (entity, entity_id);
CREATE NONCLUSTERED INDEX ix_audit_log_date ON [audit].audit_log (changed_at);
