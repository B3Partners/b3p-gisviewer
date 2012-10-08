ALTER TABLE thema_data ADD COLUMN editable number(1,0);
update thema_data set editable = 0;
ALTER TABLE thema_data
   ALTER COLUMN editable SET NOT NULL;

ALTER TABLE thema_data
   ADD COLUMN default_values character varying(255);

   ALTER TABLE gegevensbron ADD COLUMN editable number(1,0);
update gegevensbron set editable = 0;
ALTER TABLE gegevensbron
   ALTER COLUMN editable SET NOT NULL;

   ALTER TABLE thema_data ADD COLUMN default_values varchar2(255 char);
   
   
   ALTER TABLE gegevensbron ADD COLUMN geometryeditable number(1,0);
update gegevensbron set geometryeditable = 0;
ALTER TABLE gegevensbron
   ALTER COLUMN geometryeditable SET NOT NULL;
   