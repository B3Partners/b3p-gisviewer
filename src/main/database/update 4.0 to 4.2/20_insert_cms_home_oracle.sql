insert into cms_menuitem(id, titel, url, icon, volgordenr, cdate)
values
(1, 'Home', '/cms/1/home.htm', '', 10, SYSDATE);

insert into cms_menu (id, titel, cdate)
VALUES
(1, 'Main menu', SYSDATE);

insert into cms_menu_menuitem (cms_menu_id, cms_menuitem_id)
values
(1, 1);

insert into cms_pagina (id, titel, tekst, show_plain_map_btn, cdate, cms_menu, login_required)
values
(1, 'Home', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam tincidunt ultrices est vitae vehicula. Morbi nulla lectus, posuere varius viverra vitae, sodales eget justo. Vestibulum fermentum mollis odio, ut sollicitudin elit luctus dictum. Nunc pellentesque a odio non pellentesque. Curabitur volutpat nisl sed semper cursus. Aliquam nibh est, pulvinar vitae neque et, aliquam gravida nisi. Sed ante est, ullamcorper vitae tincidunt vitae, interdum ac nisi.', 0, SYSDATE, 1, 0);

-- re create sequences
drop sequence CMS_MENUITEM_ID_SEQ;
create sequence CMS_MENUITEM_ID_SEQ INCREMENT BY 1 START WITH 2;

drop sequence CMS_MENU_ID_SEQ;
create sequence CMS_MENU_ID_SEQ INCREMENT BY 1 START WITH 2;

drop sequence CMS_PAGINA_ID_SEQ;
create sequence CMS_PAGINA_ID_SEQ INCREMENT BY 1 START WITH 2;
