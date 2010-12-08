create table redlining_object (
	id serial not null,
	projectid int4,
	fillcolor varchar(255),
	opmerking text,
	the_geom geometry,        
	primary key (id)
);