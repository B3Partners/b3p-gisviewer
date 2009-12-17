ALTER TABLE clusters ADD COLUMN belangnr integer;
update clusters set belangnr=0;
ALTER TABLE clusters ALTER COLUMN belangnr SET NOT NULL;
ALTER TABLE clusters ALTER COLUMN belangnr SET DEFAULT 0;