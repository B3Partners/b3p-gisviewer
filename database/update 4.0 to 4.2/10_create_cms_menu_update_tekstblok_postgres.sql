-- drops
DROP TABLE IF EXISTS cms_menu_menuitem;
DROP TABLE IF EXISTS cms_menuitem;
DROP TABLE IF EXISTS cms_menu;
DROP TABLE IF EXISTS cms_pagina;

-- set session authorization user;

-- creates
create table cms_menu (
    id  serial not null,
    titel varchar(255) not null,
    cdate timestamp not null,
    primary key (id)
);

create table cms_menu_menuitem (
    cms_menu_id int4 not null,
    cms_menuitem_id int4 not null,
    primary key (cms_menuitem_id, cms_menu_id)
);

create table cms_menuitem (
    id  serial not null,
    titel varchar(255) not null,
    url varchar(255) not null,
    icon varchar(255),
    volgordenr int4,
    cdate timestamp not null,
    primary key (id)
);

create table cms_pagina (
    id  serial not null,
    titel varchar(255) not null,
    tekst text,
    thema varchar(255),
    show_plain_map_btn bool not null,
    cdate timestamp not null,
    cms_menu int4,
    login_required bool,
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

-- alters
ALTER TABLE tekstblok DROP COLUMN cms_pagina;
ALTER TABLE tekstblok ADD COLUMN cms_pagina integer;
ALTER TABLE tekstblok ALTER COLUMN pagina DROP NOT NULL;
