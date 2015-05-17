-- delete non existing settings from configuratie table
delete from configuratie c where setting NOT IN
(SELECT code FROM applicatie a where c.setting = a.code limit 1);

-- set parent to null to fix bug where a copied Applicatie 
-- also gets deleted when deleting original
update applicatie set parent = null;