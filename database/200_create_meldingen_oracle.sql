CREATE TABLE meldingen (
	id number(10,0) not null,
	naam_zender varchar2(255 char),
	adres_zender varchar2(255 char),
	email_zender varchar2(255 char),
	melding_type varchar2(255 char),
	melding_tekst clob,
	melding_status varchar2(255 char),
	melding_commentaar clob,
	naam_ontvanger varchar2(255 char),
	datum_ontvangst timestamp,
	datum_afhandeling timestamp,
	the_geom SDO_GEOMETRY,
	primary key (id)
);

create sequence meldingen_id_seq;