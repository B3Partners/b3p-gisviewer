-- creates
create table cms_menu (
    id number(10,0) not null,
    titel varchar2(255 char) not null,
    cdate timestamp not null,
    primary key (id)
);

create table cms_menu_menuitem (
    cms_menu_id number(10,0) not null,
    cms_menuitem_id number(10,0) not null,
    primary key (cms_menuitem_id, cms_menu_id)
);

create table cms_menuitem (
    id number(10,0) not null,
    titel varchar2(255 char) not null,
    url varchar2(255 char) not null,
    icon varchar2(255 char),
    volgordenr number(10,0),
    cdate timestamp not null,
    primary key (id)
);

create table cms_pagina (
    id number(10,0) not null,
    titel varchar2(255 char) not null,
    tekst clob,
    thema varchar2(255 char),
    show_plain_map_btn number(1,0) not null,
    cdate timestamp not null,
    cms_menu number(10,0),
    login_required number(1,0),
    primary key (id)
);

-- fks
alter table cms_menu_menuitem 
    add constraint FK83D25F5C21F8779D 
    foreign key (cms_menuitem_id) 
    references cms_menuitem;

alter table cms_menu_menuitem 
    add constraint FK83D25F5CD45309DD 
    foreign key (cms_menu_id) 
    references cms_menu;

-- sequences
create sequence cms_menu_id_seq;
create sequence cms_menuitem_id_seq;
create sequence cms_pagina_id_seq;

-- alters
ALTER TABLE tekstblok DROP (cms_pagina);
ALTER TABLE tekstblok ADD (cms_pagina number(10,0));
ALTER TABLE tekstblok MODIFY (pagina NULL);
