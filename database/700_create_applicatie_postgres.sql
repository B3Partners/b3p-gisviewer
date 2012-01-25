create table applicatie (
    id  serial not null,
    naam varchar(255),
    code varchar(255),
    gebruikers_code varchar(255),
    parent integer,
    datum_gebruikt timestamp not null,
	read_only boolean NOT NULL DEFAULT true,
	user_copy boolean NOT NULL DEFAULT false,
	versie integer NOT NULL DEFAULT 1,
	default_app boolean NOT NULL DEFAULT false,
	email varchar(255),
    primary key (id)
);