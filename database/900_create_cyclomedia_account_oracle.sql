create table cyclomedia_account (
    id number(10,0) not null,
    api_key varchar2(255 char),
    account_id varchar2(255 char),
    wachtwoord varchar2(255 char),
    private_base64_key clob,
	app_code varchar2(255 char),
    primary key (id)
);

create sequence cyclomedia_account_id_seq;