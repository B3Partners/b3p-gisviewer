create table applicatie (
    id  serial not null,
    naam varchar(255),
    code varchar(255),
    gebruikers_code varchar(255),
    parent int4,
    datum_gebruikt timestamp not null,
    primary key (id)
);