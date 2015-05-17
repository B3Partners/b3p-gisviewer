ALTER TABLE thema_data ADD (editable number(1,0));
UPDATE thema_data SET editable = 0;
ALTER TABLE thema_data MODIFY (editable NOT NULL);

ALTER TABLE thema_data ADD (default_values varchar2(255 char));

ALTER TABLE gegevensbron ADD (editable number(1,0));
UPDATE gegevensbron SET editable = 0;
ALTER TABLE gegevensbron MODIFY (editable NOT NULL);
   
ALTER TABLE gegevensbron ADD (geometryeditable number(1,0));
UPDATE gegevensbron set geometryeditable = 0;
ALTER TABLE gegevensbron MODIFY (geometryeditable NOT NULL);