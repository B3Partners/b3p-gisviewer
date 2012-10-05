ALTER TABLE thema_data ADD COLUMN editable number(1,0);
update thema_data set editable = 0;
ALTER TABLE thema_data
   ALTER COLUMN editable SET NOT NULL;

ALTER TABLE thema_data
   ADD COLUMN default_values character varying(255);
