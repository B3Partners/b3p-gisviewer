DROP TABLE IF EXISTS cms_menu_menuitem;
DROP TABLE IF EXISTS cms_menuitem;
DROP TABLE IF EXISTS cms_menu;

-- set session authorization user;

create table cms_menu (
	id  serial not null,
	titel varchar(255) not null,
	cdate timestamp not null,
	primary key (id)
);

create table cms_menuitem (
	id  serial not null,
	titel varchar(255) not null,
	url varchar(255) not null,
	icon varchar(255),
	volgordenr integer,
	cdate timestamp not null,
	primary key (id)
);

create table cms_menu_menuitem (
	cms_menu_id integer not null,
	cms_menuitem_id integer not null,
	primary key (cms_menu_id, cms_menuitem_id)
);

ALTER TABLE cms_pagina ADD COLUMN cms_menu integer;
ALTER TABLE cms_pagina ADD COLUMN login_required boolean;
