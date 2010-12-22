create table tekstblok (
	id number(10,0) not null,
	titel varchar2(255 char) not null,
	tekst clob,
	url varchar2(255 char),
	toonurl number(1),
	pagina varchar2(255 char) not null,
	volgordenr number(10,0),
	auteur varchar2(255 char),
	cdate TIMESTAMP not null,
	primary key (id)
);      
		
create sequence tekstblok_id_seq;