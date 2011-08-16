
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
        exclusive_childs bool not null,
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

    create table gegevensbron (
        id  serial not null,
        naam varchar(255),
        bron int4,
        admin_tabel varchar(255),
        admin_pk varchar(255),
        admin_query text,
        parent int4,
        admin_fk varchar(255),
        admin_tabel_opmerkingen varchar(255),
        volgordenr int4,
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

    create table tekstblok (
        id  serial not null,
        titel varchar(255) not null,
        tekst text,
        url varchar(255),
        toonurl bool,
        pagina varchar(255) not null,
        volgordenr int4,
        auteur varchar(255),
        cdate timestamp not null,
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
        waarde_type int4,
        data_type int4,
        commando varchar(255),
        kolomnaam varchar(255),
        dataorder int4,
        gegevensbron int4,
        primary key (id)
    );

    create table themas (
        id  serial not null,
        code varchar(255),
        naam varchar(255),
        metadata_link varchar(255),
        belangnr int4 not null,
        clusters int4 not null,
        opmerkingen text,
        analyse_thema bool not null,
        locatie_thema bool not null,
        visible bool not null,
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
        gegevensbron int4,
        primary key (id)
    );

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

    create table user_layer (
        id  serial not null,
        serviceid int4,
        title varchar(255),
        name varchar(255),
        queryable bool,
        scalehint_min varchar(255),
        scalehint_max varchar(255),
        use_style varchar(255),
        sld_part varchar(255),
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

    create table user_service (
        id  serial not null,
        code varchar(255) not null,
        url varchar(255) not null,
        groupname varchar(255) not null,
        sld_url varchar(255),
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
        resultlistdynamic bool,
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
        inputtype int4,
        inputsize int4,
        inputzoekconfiguratie int4,
        primary key (id)
    );

    alter table clusters 
        add constraint FK4B672DB9213F33D3 
        foreign key (parent) 
        references clusters;

    alter table gegevensbron 
        add constraint FK2218571FBFA316D7 
        foreign key (bron) 
        references bron;

    alter table gegevensbron 
        add constraint FK2218571F75542539 
        foreign key (parent) 
        references gegevensbron;

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
        add constraint FK19E205E4D2C173AE 
        foreign key (gegevensbron) 
        references gegevensbron;

    alter table themas 
        add constraint FKCBDB434EA7FB58E2 
        foreign key (clusters) 
        references clusters;

    alter table themas 
        add constraint FKCBDB434ED2C173AE 
        foreign key (gegevensbron) 
        references gegevensbron;

    alter table user_layer 
        add constraint FK72E7DFDD1B3B6F8A 
        foreign key (serviceid) 
        references user_service;

    alter table user_layer 
        add constraint FK72E7DFDDEC465980 
        foreign key (parent) 
        references user_layer;

    alter table user_layer_style 
        add constraint FK4AAD46CF251B2302 
        foreign key (layerid) 
        references user_layer;

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

    alter table zoekveld 
        add constraint FK239789828F5D4DF5 
        foreign key (inputzoekconfiguratie) 
        references zoekconfiguratie;
