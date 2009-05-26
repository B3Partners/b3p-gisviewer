ALTER TABLE clusters ADD COLUMN callable boolean NOT NULL DEFAULT false;
ALTER TABLE clusters ADD COLUMN default_visible boolean NOT NULL DEFAULT false;
ALTER TABLE clusters ADD COLUMN metadatalink text;