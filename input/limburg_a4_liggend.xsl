<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>
  <xsl:param name="versionParam" select="'1.0'"/> 
  <xsl:param name="imageUrl"/>
  <xsl:param name="legenduri"/>

  <xsl:variable name="noordpijl" select="'images/pzh_noordpijl.jpg'"/>

    <!-- styles -->
    <xsl:attribute-set name="default-font">
        <xsl:attribute name="font-family">Arial</xsl:attribute>
        <xsl:attribute name="margin-left">0.5cm</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="simple-border">
        <xsl:attribute name="border-top-color">#000000</xsl:attribute>
        <xsl:attribute name="border-top-style">solid</xsl:attribute>
        <xsl:attribute name="border-top-width">medium</xsl:attribute>
        <xsl:attribute name="border-bottom-color">#000000</xsl:attribute>
        <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
        <xsl:attribute name="border-bottom-width">medium</xsl:attribute>
        <xsl:attribute name="border-left-color">#000000</xsl:attribute>
        <xsl:attribute name="border-left-style">solid</xsl:attribute>
        <xsl:attribute name="border-left-width">medium</xsl:attribute>
        <xsl:attribute name="border-right-color">#000000</xsl:attribute>
        <xsl:attribute name="border-right-style">solid</xsl:attribute>
        <xsl:attribute name="border-right-width">medium</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="column-block" use-attribute-sets="simple-border">
        <xsl:attribute name="position">relative</xsl:attribute>
        <xsl:attribute name="top">0cm</xsl:attribute>
        <xsl:attribute name="left">0cm</xsl:attribute>
        <xsl:attribute name="width">100%</xsl:attribute>
    </xsl:attribute-set>

<!-- root -->
<xsl:template match="info">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink" font-family="Arial">

    <xsl:call-template name="layout-master-set"/>

    <fo:page-sequence master-reference="a4-staand">
        <fo:flow flow-name="body">
            <fo:block-container width="4cm" height="20cm" top="0cm" position="absolute" left="0cm">
                <xsl:call-template name="left-column"/>
            </fo:block-container>
            <fo:block-container width="14.0cm" height="20cm" top="0cm" position="absolute" left="4.5cm">
                <xsl:call-template name="body-column"/>
            </fo:block-container>
        </fo:flow>
    </fo:page-sequence>
    </fo:root>
</xsl:template>

<!-- columns -->
<xsl:template name="left-column">
    <fo:block>
        <xsl:call-template name="logo-block"/>
        <xsl:call-template name="legend-block"/>
        <xsl:call-template name="extra-block"/>
    </fo:block>
</xsl:template>

<xsl:template name="body-column">
    <fo:block>
        <xsl:call-template name="title-block"/>
        <xsl:call-template name="map-block"/>
    </fo:block>
</xsl:template>

<!-- blocks -->
<xsl:template name="logo-block">
<fo:block-container height="4.0cm" xsl:use-attribute-sets="column-block">
    <fo:block margin-top="0.2cm" margin-left="0.2cm">
        <fo:external-graphic src="http://www.bibl-rijswijk.nl/jaarverslag/Images/logo%20provincie%20zuid-holland.jpg" content-width="3.5cm"/>
    </fo:block>
</fo:block-container>
</xsl:template>

<xsl:template name="legend-block">
<xsl:variable name="legend">
    <xsl:value-of select="$legenduri"/>
</xsl:variable>

<fo:block-container margin-top="0.5cm" height="4.0cm" xsl:use-attribute-sets="column-block">
    <fo:block margin-top="0.2cm" margin-left="0.2cm">
        Legenda
        <fo:block margin-top="0.1cm" margin-left="0.1cm">
            <fo:external-graphic src="{$legenduri}"/>
        </fo:block>

    </fo:block>
</fo:block-container>
</xsl:template>

<xsl:template name="extra-block">
<fo:block-container margin-top="0.5cm" height="6.0cm" xsl:use-attribute-sets="column-block">
    <fo:block margin-top="0.2cm" margin-left="0.2cm">
        <fo:block margin-left="0.1cm" margin-top="0.2cm">
            <fo:external-graphic src="{$noordpijl}"/>
        </fo:block>
        <fo:block margin-left="0.1cm" margin-top="0.1cm" font-size="10pt" xsl:use-attribute-sets="default-font">
            <xsl:text>Titel: </xsl:text>
            <xsl:value-of select="titel"/>
        </fo:block>
        <fo:block margin-left="0.1cm" margin-top="0.1cm" font-size="10pt" xsl:use-attribute-sets="default-font">
            <xsl:text>Opmerking: </xsl:text>
            <xsl:value-of select="opmerking"/>
        </fo:block>
    </fo:block>
</fo:block-container>
</xsl:template>

<xsl:template name="title-block">
<fo:block-container height="1.5cm" xsl:use-attribute-sets="column-block">
    <fo:block margin-left="0.2cm" margin-top="0.2cm" font-size="17pt" xsl:use-attribute-sets="default-font">
        Kaart van Nederland
    </fo:block>
</fo:block-container>
</xsl:template>

<xsl:template name="map-block">
    <xsl:variable name="map">
        <xsl:value-of select="$imageUrl"/>

        <!-- floats min x, min y, max x, max y -->
        <xsl:text>&amp;bbox=135000,490000,155000,510000</xsl:text>
        </xsl:variable>
    <fo:block-container margin-right="0.5cm" margin-top="0.5cm" height="17cm" xsl:use-attribute-sets="column-block">
        <fo:block margin-left="0.2cm" margin-top="0.2cm">
                <fo:external-graphic src="{$map}"/>
        </fo:block>
    </fo:block-container>
</xsl:template>

<!-- master set -->
<xsl:template name="layout-master-set">
<fo:layout-master-set>
        <!-- titel pagina -->
        <fo:simple-page-master master-name="a4-title" page-width="210mm" page-height="297mm" margin-top="0.0cm" margin-bottom="0.0cm" margin-left="0.0cm" margin-right="0.0cm">
                <fo:region-body region-name="body"/>
        </fo:simple-page-master>
        <!-- staande pagina -->
        <fo:simple-page-master master-name="a4-staand" page-width="210mm" page-height="297mm" margin-top="0.45cm" margin-bottom="0cm" margin-left="1.97cm" margin-right="1.87cm">
                <fo:region-body region-name="body" margin-top="2cm" margin-bottom="2cm"/>
                <fo:region-before region-name="header"/>
                <fo:region-after region-name="footer" extent="2cm"/>
        </fo:simple-page-master>
        <!-- liggende pagina -->
        <fo:simple-page-master master-name="a4-liggend" page-height="210mm" page-width="297mm" margin-top="1cm" margin-bottom="1cm" margin-left="2cm" margin-right="1cm">
                <fo:region-body region-name="body"/>
        </fo:simple-page-master>
</fo:layout-master-set>
</xsl:template>

</xsl:stylesheet>
