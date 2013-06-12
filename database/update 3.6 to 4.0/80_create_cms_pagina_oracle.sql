create table cms_pagina (
    id number(19,0) not null,
    titel varchar2(255 char),
    tekst clob,
	thema varchar2(255 char),
    show_plain_map_btn number(1),
    cdate varchar2(255 char),
    primary key (id)
);

ALTER TABLE tekstblok ADD (cms_pagina integer);
ALTER TABLE tekstblok DROP (pagina NOT NULL);