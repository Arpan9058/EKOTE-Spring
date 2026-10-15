-- Reconcile duplicate non-null emails before upgrading a legacy database.
ALTER TABLE register ADD CONSTRAINT uk_register_mail UNIQUE (mail);
