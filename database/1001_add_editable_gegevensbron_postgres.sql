ALTER TABLE gegevensbron ADD COLUMN editable boolean;
update gegevensbron set editable = false;
ALTER TABLE gegevensbron
   ALTER COLUMN editable SET NOT NULL;
   
   
ALTER TABLE thema_data ADD COLUMN default_values text;

ALTER TABLE gegevensbron ADD COLUMN geometryeditable boolean;
update gegevensbron set geometryeditable = false;
ALTER TABLE gegevensbron
   ALTER COLUMN geometryeditable SET NOT NULL;
   
   