--TODO code nog niet aangepast
ALTER TABLE gegevensbron ADD COLUMN parent_pk CHARACTER VARYING(255);
update gegevensbron set parent_pk = admin_pk;
