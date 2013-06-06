create table cms_pagina (
        id  serial not null,
        titel varchar(255) not null,
        tekst text,
        thema varchar(255),
        cdate timestamp not null,
        primary key (id)
    );
