create table user_kaartgroep (
    id  serial not null,
    code varchar(255) not null,
    clusterid int4 not null,
    default_on bool,
    primary key (id)
);

create table user_kaartlaag (
    id  serial not null,
    code varchar(255) not null,
    themaid int4 not null,
    default_on bool,
    primary key (id)
);

create table user_service (
    id  serial not null,
    code varchar(255) not null,
    url varchar(255) not null,
    groupname varchar(255) not null,
    sld_url varchar(255),
    primary key (id)
);

create table user_layer (
    id  serial not null,
    serviceid int4,
    title varchar(255),
    name varchar(255),
    queryable bool,
    scalehint_min varchar(255),
    scalehint_max varchar(255),
    use_style varchar(255),
    sld_part text,
	show bool,
    default_on bool,
	parent int4,
    primary key (id)
);

create table user_layer_style (
    id  serial not null,
    layerid int4,
    name varchar(255),
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