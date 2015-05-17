-- Oracle voorbeeld mutaties tabel
CREATE TABLE mutaties (
	id number(10,0) not null,	
	soort varchar2(255 char),
	status varchar2(255 char),
	naam_melder varchar2(255 char),
	email_melder varchar2(255 char),
	opmerking clob,
	the_geom MDSYS.SDO_GEOMETRY,
	primary key (id)
);

-- Aanmaken metadata
INSERT INTO mdsys.user_sdo_geom_metadata VALUES (
    'mutaties',
    'the_geom',
    MDSYS.SDO_DIM_ARRAY(
MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.05),
MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.05)), 90112);

-- Aanmaken sequence
create sequence mutaties_id_seq;

-- Aanmaken spatial index
CREATE INDEX schemanaam.spatial_idx_mutaties ON schemanaam.mutaties (the_geom)
INDEXTYPE IS "MDSYS"."SPATIAL_INDEX" PARAMETERS ('LAYER_GTYPE=POLYGON');
