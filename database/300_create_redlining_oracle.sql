create table redlining_object (
	id number(10,0) not null,
	groepnaam varchar2(255 char),
	projectnaam varchar2(255 char),
	fillcolor varchar2(255 char),
	opmerking clob,
	the_geom SDO_GEOMETRY,
	primary key (id)
);      
		
create sequence redlining_object_id_seq;