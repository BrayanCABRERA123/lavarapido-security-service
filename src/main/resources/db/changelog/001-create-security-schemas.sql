--liquibase formatted sql

--changeset lavarapido:security-001-schema-security
--comment: Esquema del security-service (ADR-009)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.schemas WHERE name = 'security'
CREATE SCHEMA [security];

--changeset lavarapido:security-001-schema-audit
--comment: Bitácora de cambios transversal, solo lectura para security-service (la escriben triggers)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.schemas WHERE name = 'audit'
CREATE SCHEMA [audit];
