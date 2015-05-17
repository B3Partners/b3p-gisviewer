-- oracle
alter table configuratie add (propval_clob clob);
update configuratie set propval_clob = propval;
alter table configuratie drop column propval;
alter table configuratie rename column propval_clob to propval;
