--liquibase formatted sql
-- Roles personalizados con permisos (admin, Gestion > Roles). Distintos de security.role (los 3
-- roles fijos ADMIN/OPERATOR/CLIENT de cada cuenta, migracion 002): esto es un catalogo propio
-- que el admin arma a su gusto, sin ningun efecto sobre la autorizacion real todavia.

--changeset lavarapido:security-023-create-custom-role
--comment: Catalogo de roles personalizados
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'custom_role'
CREATE TABLE [security].custom_role (
    custom_role_id SMALLINT     IDENTITY(1,1) NOT NULL,
    name           NVARCHAR(100) NOT NULL,
    description    NVARCHAR(300) NULL,
    -- codigos separados por coma (view_panels, create_records, edit_data, delete); es solo texto
    -- para mostrar, no se usa en ninguna verificacion de autorizacion
    permissions    NVARCHAR(200) NULL,
    created_at     DATETIME2(3) NOT NULL CONSTRAINT df_custom_role_created DEFAULT SYSUTCDATETIME(),
    created_by     BIGINT       NULL,
    updated_at     DATETIME2(3) NULL,
    updated_by     BIGINT       NULL,
    deleted_at     DATETIME2(3) NULL,
    deleted_by     BIGINT       NULL,
    row_version    INT          NOT NULL CONSTRAINT df_custom_role_rv DEFAULT 1,
    CONSTRAINT pk_custom_role PRIMARY KEY (custom_role_id)
);
--rollback DROP TABLE [security].custom_role;
