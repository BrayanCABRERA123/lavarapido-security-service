--liquibase formatted sql
-- Interruptores de notificaciones por canal (Configuración > Notificaciones, RF-020). notifications_enabled
-- ya existía y es el de las notificaciones push; estas dos columnas son el correo de los recordatorios de
-- reserva y las promociones (cupones desbloqueados). notification-service las lee con el contacto interno
-- (ADR-011). Arrancan en 1 para que nadie deje de recibir lo que ya recibía.

--changeset lavarapido:security-030-user-preference-notification-channels
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('security.user_preference') AND name = 'email_reminders_enabled'
ALTER TABLE [security].user_preference ADD
    email_reminders_enabled BIT NOT NULL CONSTRAINT df_pref_email_reminders DEFAULT 1,
    promotions_enabled      BIT NOT NULL CONSTRAINT df_pref_promotions DEFAULT 1;
--rollback ALTER TABLE [security].user_preference DROP CONSTRAINT df_pref_email_reminders, df_pref_promotions; ALTER TABLE [security].user_preference DROP COLUMN email_reminders_enabled, promotions_enabled;
