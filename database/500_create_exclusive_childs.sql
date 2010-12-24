ALTER TABLE clusters ADD COLUMN exclusive_childs boolean NOT NULL DEFAULT false;
ALTER TABLE clusters
   ALTER COLUMN exclusive_childs DROP DEFAULT;