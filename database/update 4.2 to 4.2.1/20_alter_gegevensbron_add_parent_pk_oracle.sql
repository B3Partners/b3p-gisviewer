--TODO code nog niet aangepast
ALTER TABLE gegevensbron ADD (parent_pk varchar2(255 char));
update gegevensbron set parent_pk = admin_pk;
