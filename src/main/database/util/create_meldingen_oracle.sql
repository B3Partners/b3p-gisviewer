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
	the_geom MDSYS.SDO_GEOMETRY,
	primary key (id)
);

INSERT INTO user_sdo_geom_metadata VALUES (
    'meldingen',
    'the_geom',
    MDSYS.SDO_DIM_ARRAY(
MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.05),
MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.05)), 28992);

create sequence meldingen_id_seq;

create index meldingen_spatial_idx on meldingen(the_geom) INDEXTYPE is MDSYS.SPATIAL_INDEX;