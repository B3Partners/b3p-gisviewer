-- set session authorization user;

DROP TABLE cms_menu_menuitem;
DROP TABLE cms_menuitem;
DROP TABLE cms_menu;

ALTER TABLE cms_pagina DROP COLUMN cms_menu;

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
