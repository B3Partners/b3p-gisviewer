create table applicatie (
    id number(10,0) not null,
    naam varchar2(255 char),
    code varchar2(255 char),
    gebruikers_code varchar2(255 char),
    parent number(10,0),
    datum_gebruikt timestamp not null,
    read_only number(1) default 1 not null ,
    user_copy number(1) default 0 not null,
    versie number(10,0) default 1 not null,
    default_app number(1) default 0 not null,
	email varchar2(255 char),
    primary key (id)
);

create sequence applicatie_id_seq;