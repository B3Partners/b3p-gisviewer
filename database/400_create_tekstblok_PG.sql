create table tekstblok (
	id serial not null,
	titel varchar(255) not null,
	tekst text,
	url varchar(255),
	toonurl boolean,
	pagina varchar(255) not null,
	volgordenr integer,
	auteur varchar(255),	
	cdate timestamp not null,     
	primary key (id)
);