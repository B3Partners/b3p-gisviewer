ALTER TABLE zoekveld ADD COLUMN inputtype integer;
ALTER TABLE zoekveld ADD COLUMN inputsize integer;
ALTER TABLE zoekveld ADD COLUMN inputzoekconfiguratie integer;

ALTER TABLE zoekconfiguratie ADD COLUMN resultlistdynamic BOOLEAN;
UPDATE zoekconfiguratie SET resultlistdynamic = true;
ALTER TABLE zoekconfiguratie ALTER COLUMN resultlistdynamic SET NOT NULL;