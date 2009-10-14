
    create table clusters (
        id  serial not null,
        naam varchar(255),
        omschrijving varchar(255),
        metadatalink varchar(255),
        default_cluster bool not null,
        hide_legend bool not null,
        hide_tree bool not null,
        background_cluster bool not null,
        extra_level bool not null,
        callable bool not null,
        default_visible bool not null,
        parent int4,
        primary key (id)
    );

    create table connecties (
        id  serial not null,
        naam varchar(255),
        connectie_url varchar(255),
        gebruikersnaam varchar(255),
        wachtwoord varchar(255),
        primary key (id)
    );

    create table data_typen (
        id  serial not null,
        naam varchar(255),
        primary key (id)
    );

    create table thema_data (
        id  serial not null,
        label varchar(255),
        eenheid varchar(255),
        omschrijving varchar(255),
        basisregel bool not null,
        voorbeelden varchar(255),
        kolombreedte int4 not null,
        thema int4,
        waarde_type int4,
        data_type int4,
        commando varchar(255),
        kolomnaam varchar(255),
        dataorder int4,
        primary key (id)
    );

    create table themas (
        id  serial not null,
        code varchar(255),
        naam varchar(255),
        metadata_link varchar(255),
        connectie int4,
        belangnr int4 not null,
        cluster int4 not null,
        opmerkingen text,
        analyse_thema bool not null,
        locatie_thema bool not null,
        visible bool not null,
        admin_tabel_opmerkingen varchar(255),
        admin_tabel varchar(255),
        admin_pk varchar(255),
        admin_pk_complex bool not null,
        admin_spatial_ref varchar(255),
        admin_query text,
        spatial_tabel_opmerkingen text,
        spatial_tabel varchar(255),
        spatial_pk varchar(255),
        spatial_pk_complex bool not null,
        spatial_admin_ref varchar(255),
        wms_url varchar(255),
        wms_layers varchar(255),
        wms_layers_real varchar(255),
        wms_legendlayer varchar(255),
        wms_legendlayer_real varchar(255),
        wms_querylayers varchar(255),
        wms_querylayers_real varchar(255),
        update_frequentie_in_dagen int4,
        view_geomtype varchar(255),
        organizationcodekey varchar(255),
        maptipstring varchar(255),
        primary key (id)
    );

    create table waarde_typen (
        id  serial not null,
        naam varchar(255),
        primary key (id)
    );

    alter table clusters 
        add constraint FK4B672DB9213F33D3 
        foreign key (parent) 
        references clusters;

    alter table thema_data 
        add constraint FK19E205E4E888F629 
        foreign key (data_type) 
        references data_typen;

    alter table thema_data 
        add constraint FK19E205E4E611B385 
        foreign key (waarde_type) 
        references waarde_typen;

    alter table thema_data 
        add constraint FK19E205E4B89C5283 
        foreign key (thema) 
        references themas;

    alter table themas 
        add constraint FKCBDB434E908F3D23 
        foreign key (cluster) 
        references clusters;

    alter table themas 
        add constraint FKCBDB434E2921D2A3 
        foreign key (connectie) 
        references connecties;
