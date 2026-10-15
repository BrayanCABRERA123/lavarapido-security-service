--liquibase formatted sql
-- ADR-010: la dirección del usuario (dato de contacto del perfil; el servicio es en la sede). Va en
-- su propia tabla para no tocar person. Una fila por persona; borrar la dirección borra la fila.

--changeset lavarapido:security-035-person-address
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'security' AND t.name = 'person_address'
CREATE TABLE [security].person_address (
    person_id  BIGINT        NOT NULL,
    address    NVARCHAR(200) NOT NULL,
    updated_at DATETIME2(3)  NOT NULL CONSTRAINT df_person_address_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_person_address PRIMARY KEY (person_id),
    CONSTRAINT fk_person_address_person FOREIGN KEY (person_id) REFERENCES [security].person(person_id),
    CONSTRAINT ck_person_address_not_blank CHECK (LEN(LTRIM(RTRIM(address))) > 0)
);
--rollback DROP TABLE [security].person_address;
