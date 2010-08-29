
    create table bron (
        id  serial not null,
        naam varchar(255),
        url varchar(255),
        gebruikersnaam varchar(255),
        wachtwoord varchar(255),
        volgorde int4,
        primary key (id)
    );

    create table clusters (
        id  serial not null,
        naam varchar(255),
        omschrijving varchar(255),
        belangnr int4 not null,
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

    create table configuratie (
        id  serial not null,
        property varchar(255),
        propval varchar(255),
        setting varchar(255),
        soort varchar(255),
        primary key (id)
    );

    create table data_typen (
        id  serial not null,
        naam varchar(255),
        primary key (id)
    );

    create table resultaatveld (
        id  serial not null,
        naam varchar(255),
        label varchar(255),
        volgorde int4,
        attribuutnaam varchar(255),
        soort int4,
        zoekconfiguratie int4,
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
        clusters int4 not null,
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
        sldattribuut varchar(255),
        layoutadmindata varchar(255),
        primary key (id)
    );

    create table waarde_typen (
        id  serial not null,
        naam varchar(255),
        primary key (id)
    );

    create table zoekconfiguratie (
        id  serial not null,
        naam varchar(255),
        featuretype varchar(255),
        parentbron int4,
        parentzoekconfiguratie int4,
        primary key (id)
    );

    create table zoekveld (
        id  serial not null,
        naam varchar(255),
        label varchar(255),
        attribuutnaam varchar(255),
        soort int4,
        volgorde int4,
        zoekconfiguratie int4,
        primary key (id)
    );

    alter table clusters 
        add constraint FK4B672DB9213F33D3 
        foreign key (parent) 
        references clusters;

    alter table resultaatveld 
        add constraint FK1DFFA7FE7EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;

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
        add constraint FKCBDB434EA7FB58E2 
        foreign key (clusters) 
        references clusters;

    alter table themas 
        add constraint FKCBDB434E9CEEC42E 
        foreign key (connectie) 
        references bron;

    alter table zoekconfiguratie 
        add constraint FK88B2EC896D4F3ED5 
        foreign key (parentzoekconfiguratie) 
        references zoekconfiguratie;

    alter table zoekconfiguratie 
        add constraint FK88B2EC89B0E5BA81 
        foreign key (parentbron) 
        references bron;

    alter table zoekveld 
        add constraint FK239789827EE6CA2B 
        foreign key (zoekconfiguratie) 
        references zoekconfiguratie;
