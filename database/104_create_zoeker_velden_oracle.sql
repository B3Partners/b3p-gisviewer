alter table zoekveld add inputtype number(10,0);
alter table zoekveld add inputsize number(10,0);
alter table zoekveld add inputzoekconfiguratie number(10,0);

ALTER TABLE zoekconfiguratie ADD (resultlistdynamic NUMBER(1,0));
UPDATE zoekconfiguratie SET resultlistdynamic = 1;
ALTER TABLE zoekconfiguratie MODIFY (resultlistdynamic NOT NULL);
