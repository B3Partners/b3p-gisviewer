create table user_kaartgroep (
    id number(10,0) not null,
    code varchar2(255 char) not null,
    clusterid number(10,0) not null,
    default_on number(1,0),
    primary key (id)
);

create table user_kaartlaag (
    id number(10,0) not null,
    code varchar2(255 char) not null,
    themaid number(10,0) not null,
    default_on number(1,0),
    primary key (id)
);

create table user_layer (
    id number(10,0) not null,
    serviceid number(10,0),
    title varchar2(255 char),
    name varchar2(255 char),
    queryable number(1,0),
    scalehint_min varchar2(255 char),
    scalehint_max varchar2(255 char),
    use_style varchar2(255 char),
    sld_part clob,
	show number(1,0),
    default_on number(1,0),
	parent number(10,0),
    primary key (id)
);

create table user_layer_style (
    id number(10,0) not null,
    layerid number(10,0),
    name varchar2(255 char),
    primary key (id)
);

create table user_service (
    id number(10,0) not null,
    code varchar2(255 char) not null,
    url varchar2(255 char) not null,
    groupname varchar2(255 char) not null,
    sld_url varchar2(255 char),
	name varchar2(255 char),
	use_in_list number(1,0),
    primary key (id)
);

alter table user_layer
    add constraint FK72E7DFDD1B3B6F8A
    foreign key (serviceid)
    references user_service;

alter table user_layer_style
    add constraint FK4AAD46CF251B2302
    foreign key (layerid)
    references user_layer;
	
create sequence user_kaartgroep_id_seq;

create sequence user_kaartlaag_id_seq;

create sequence user_layer_id_seq;

create sequence user_layer_style_id_seq;

create sequence user_service_id_seq;