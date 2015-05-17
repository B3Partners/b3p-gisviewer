create table redlining_object (
	id number(10,0) not null,
	groepnaam varchar2(255 char),
	projectnaam varchar2(255 char),
	ontwerp varchar2(255 char),
	opmerking clob,
	the_geom MDSYS.SDO_GEOMETRY,
	primary key (id)
);      

-- in de metatabel wordt voor SDO_OWNER de user gebruikt waarmee je de verbinding hebt gemaakt
-- als deze hetzelfde is als het schema waar de redlining tabel in staat is het goed
insert into user_sdo_geom_metadata values (
        'redlining_object',
        'the_geom',
        MDSYS.SDO_DIM_ARRAY(
            MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.05),
            MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.05)),
        28992);
	
create sequence redlining_object_id_seq;

-- lukt alleen als SDO_OWNER in metadata tabel hetzelfde is als schema waarin redlining tabel staat
-- controleren of hier hets chemanaam staat waarin de redlining tabel geplaatst is.
create index redlining_spatial_idx on redlining_object(the_geom) INDEXTYPE is MDSYS.SPATIAL_INDEX;