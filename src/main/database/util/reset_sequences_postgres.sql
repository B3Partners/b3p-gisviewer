
select setval('bron_id_seq', (select max(id) from bron));
select setval('clusters_id_seq', (select max(id) from clusters));
select setval('configuratie_id_seq', (select max(id) from configuratie));
select setval('data_typen_id_seq', (select max(id) from data_typen));
select setval('resultaatveld_id_seq', (select max(id) from resultaatveld));
select setval('thema_data_id_seq', (select max(id) from thema_data));
select setval('themas_id_seq', (select max(id) from themas));
select setval('waarde_typen_id_seq', (select max(id) from waarde_typen));
select setval('zoekconfiguratie_id_seq', (select max(id) from zoekconfiguratie));
select setval('zoekveld_id_seq', (select max(id) from zoekveld));
select setval('cms_menu_id_seq', (select max(id) from cms_menu));
select setval('cms_menuitem_id_seq', (select max(id) from cms_menuitem));
select setval('cms_pagina_id_seq', (select max(id) from cms_pagina));
