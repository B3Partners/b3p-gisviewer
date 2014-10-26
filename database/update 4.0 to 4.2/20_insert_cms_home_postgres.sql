insert into cms_menuitem(id, titel, url, icon, volgordenr, cdate)
values
(1, 'Home', '/cms/1/home.htm', '', 10, now());

insert into cms_menu (id, titel, cdate)
VALUES
(1, 'Main menu', now());

insert into cms_menu_menuitem (cms_menu_id, cms_menuitem_id)
values
(1, 1);

insert into cms_pagina (id, titel, tekst, show_plain_map_btn, cdate, cms_menu, login_required)
values
(1, 'Home', '<vul hier uw tekst in>', false, now(), 1, false);
