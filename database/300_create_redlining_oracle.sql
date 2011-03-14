create table redlining_object (
	id number(10,0) not null,
	groepnaam varchar2(255 char),
	projectnaam varchar2(255 char),
	fillcolor varchar2(255 char),
	opmerking clob,
	the_geom MDSYS.SDO_GEOMETRY,
	primary key (id)
);      

INSERT INTO user_sdo_geom_metadata VALUES (
    'redlining_object',
    'the_geom',
    MDSYS.SDO_DIM_ARRAY(
MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.05),
MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.05)), 28992);
	
create sequence redlining_object_id_seq;

CREATE INDEX redlining_spatial_idx ON REDLINING_OBJECT(THE_GEOM) INDEXTYPE IS MDSYS.SPATIAL_INDEX;