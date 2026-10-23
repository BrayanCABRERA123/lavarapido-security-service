--liquibase formatted sql
-- La web tiene cuatro paletas (green-light, green-dark, pink, pink-dark) y el tema debe quedar
-- guardado por cuenta (RF-019). El CHECK original solo aceptaba light/dark.

--changeset lavarapido:security-020-user-preference-palettes
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.check_constraints WHERE name = 'ck_pref_theme' AND definition LIKE '%pink%'
ALTER TABLE [security].user_preference DROP CONSTRAINT ck_pref_theme;
ALTER TABLE [security].user_preference DROP CONSTRAINT df_pref_theme;
UPDATE [security].user_preference SET theme = N'green-light' WHERE theme = N'light';
UPDATE [security].user_preference SET theme = N'green-dark' WHERE theme = N'dark';
ALTER TABLE [security].user_preference ADD CONSTRAINT df_pref_theme DEFAULT N'green-light' FOR theme;
ALTER TABLE [security].user_preference ADD CONSTRAINT ck_pref_theme
    CHECK (theme IN (N'green-light', N'green-dark', N'pink', N'pink-dark'));
