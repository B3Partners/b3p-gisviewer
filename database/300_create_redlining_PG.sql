create table redlining_object (
	id serial not null,
	groepnaam varchar(255),
	projectnaam varchar(255),
	fillcolor varchar(255),
	opmerking text,
	the_geom geometry,        
	primary key (id)
);