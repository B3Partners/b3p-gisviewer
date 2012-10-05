ALTER TABLE thema_data ADD COLUMN editable boolean;
update thema_data set editable = false;
ALTER TABLE thema_data
   ALTER COLUMN editable SET NOT NULL;
