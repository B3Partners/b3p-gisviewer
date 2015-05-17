create table redlining_object (
	id serial not null,
	groepnaam varchar(255),
	projectnaam varchar(255),
	ontwerp varchar(255),
	opmerking text,
	primary key (id)
);

SELECT AddGeometryColumn ('redlining_object','the_geom',28992,'GEOMETRY',2);