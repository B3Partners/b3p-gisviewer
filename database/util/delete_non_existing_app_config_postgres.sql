delete from configuratie c where setting NOT IN
(SELECT code FROM applicatie a where c.setting = a.code limit 1);