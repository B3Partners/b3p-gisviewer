create table cyclomedia_account (
    id serial not null,
    api_key varchar(255),
    account_id varchar(255),
    wachtwoord varchar(255),
	private_base64_key text,
	app_code varchar(255),
    primary key (id)
);