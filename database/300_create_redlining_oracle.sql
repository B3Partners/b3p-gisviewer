create table redlining_object (
	id number(10,0) not null,
	groepnaam varchar2(255 char),
	projectnaam varchar2(255 char),
	ontwerp varchar2(255 char),
	opmerking clob,
	the_geom MDSYS.SDO_GEOMETRY,
	primary key (id)
);      

insert into user_sdo_geom_metadata values (
	'redlining_object',
    'the_geom',
    MDSYS.SDO_DIM_ARRAY(
MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.05),
MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.05)), 28992);
	
create sequence redlining_object_id_seq;

-- lukt alleen als SDO_OWNER in metadata tabel hetzelfde is als schema waarin redlining tabel staat
create index redlining_spatial_idx on redlining_object(the_geom) INDEXTYPE is MDSYS.SPATIAL_INDEX;