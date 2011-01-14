create table redlining_object (
	id serial not null,
	groepnaam varchar(255),
	projectnaam varchar(255),
	fillcolor varchar(255),
	opmerking text,
	primary key (id)
);

SELECT AddGeometryColumn ('the_geom',28992,'GEOMETRY',2);

