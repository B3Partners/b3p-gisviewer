<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
    <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>

    <!-- formatter -->
    <xsl:decimal-format decimal-separator="," grouping-separator="." name="MyFormat" NaN="&#160;" infinity="&#160;"/>

    <!-- berekent de breedte van de kaart in meters na correctie vanwege verschil
	in verhouding hoogte/breedte kaart op scherm en van kaart in template -->
    <xsl:template name="calc-bbox-width-m-corrected">
        <xsl:param name="bbox"/>

        <xsl:variable name="xmin" select="substring-before($bbox, ',')"/>
        <xsl:variable name="bbox1" select="substring-after($bbox, ',')"/>
        <xsl:variable name="ymin" select="substring-before($bbox1, ',')"/>
        <xsl:variable name="bbox2" select="substring-after($bbox1, ',')"/>
        <xsl:variable name="xmax" select="substring-before($bbox2, ',')"/>
        <xsl:variable name="ymax" select="substring-after($bbox2, ',')"/>
        <xsl:variable name="bbox-width-m" select="$xmax - $xmin"/>
        <xsl:variable name="bbox-height-m" select="$ymax - $ymin"/>
        <xsl:variable name="bbox-ratio" select="(mapWidth * $bbox-height-m) div (mapHeight * $bbox-width-m)"/>
        <xsl:choose>
            <xsl:when test="$bbox-ratio &gt; 1">
                <xsl:value-of select="$bbox-width-m * $bbox-ratio"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$bbox-width-m"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- berekent nieuwe bbox indien verhouding hoogte/breedte van kaart op scherm
    anders is dan verhouding van kaart in template, kaart in template bevat minimaal
    dekking van kaart op scherm, maar mogelijk meer -->
    <xsl:template name="correct-bbox">
        <xsl:param name="bbox"/>

        <xsl:variable name="xmin" select="substring-before($bbox, ',')"/>
        <xsl:variable name="bbox1" select="substring-after($bbox, ',')"/>
        <xsl:variable name="ymin" select="substring-before($bbox1, ',')"/>
        <xsl:variable name="bbox2" select="substring-after($bbox1, ',')"/>
        <xsl:variable name="xmax" select="substring-before($bbox2, ',')"/>
        <xsl:variable name="ymax" select="substring-after($bbox2, ',')"/>
        <xsl:variable name="xmid" select="($xmin + $xmax) div 2"/>
        <xsl:variable name="ymid" select="($ymin + $ymax) div 2"/>
        <xsl:variable name="bbox-width-m" select="$xmax - $xmin"/>
        <xsl:variable name="bbox-height-m" select="$ymax - $ymin"/>
        <xsl:variable name="bbox-ratio" select="(mapWidth * $bbox-height-m) div (mapHeight * $bbox-width-m)"/>
        <xsl:choose>
            <xsl:when test="$bbox-ratio = 1">
                <xsl:value-of select="$bbox"/>
            </xsl:when>
            <xsl:when test="$bbox-ratio &gt; 1">
                <xsl:variable name="bbox-width-m-corrected" select="$bbox-width-m * $bbox-ratio"/>
                <xsl:value-of select="$xmid - ($bbox-width-m-corrected div 2)"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$ymin"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$xmid + ($bbox-width-m-corrected div 2)"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$ymax"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="bbox-height-m-corrected" select="$bbox-height-m div $bbox-ratio"/>
                <xsl:value-of select="$xmin"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$ymid - ($bbox-height-m-corrected div 2)"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$xmax"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="$ymid + ($bbox-height-m-corrected div 2)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!-- berekent en tekent de schaalstok, houdt rekening met echte schaal op kaart -->
<xsl:template name="calc-scale">
    <xsl:param name="m-width"/>
    <xsl:param name="px-width"/>
    <xsl:variable name="scale-label">
        <xsl:call-template name="calc-scale-m">
            <xsl:with-param name="width-m" select="$m-width"/>
            <xsl:with-param name="width-px" select="$px-width"/>
        </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="scale-width">
        <xsl:call-template name="calc-scale-px">
            <xsl:with-param name="width-m" select="$m-width"/>
            <xsl:with-param name="width-px" select="$px-width"/>
        </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="scale-unit">
        <xsl:choose>
            <xsl:when test="$scale-label &gt;= 1000">
                <xsl:text>km</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>m</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="scale-label-corrected">
        <xsl:choose>
            <xsl:when test="$scale-label &gt;= 1000">
                <xsl:value-of select="format-number($scale-label div 1000,'0','MyFormat')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="format-number($scale-label,'0','MyFormat')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:call-template name="create-scale">
        <xsl:with-param name="width" select="$scale-width"/>
        <xsl:with-param name="label" select="$scale-label-corrected"/>
        <xsl:with-param name="unit" select="$scale-unit"/>
    </xsl:call-template>
</xsl:template>

<!-- verkleint iteratief waarde door delen met 10 naar waarde tussen 0 en 1 -->
<xsl:template name="strip-zeros">
    <xsl:param name="value"/>
    <xsl:choose>
        <xsl:when test="$value >= 10">
            <xsl:call-template name="strip-zeros">
                <xsl:with-param name="value" select="$value div 10"/>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$value"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- berekent de lengte van een segment van de schaalbalk in meters -->
<xsl:template name="calc-scale-m">
    <xsl:param name="width-m" select="'100000'"/>
    <xsl:param name="width-px" select="'700'"/>
    <xsl:variable name="screen-scale" select="$width-px div $width-m"/>
    <xsl:variable name="tmp-length-guess" select="20 div $screen-scale"/>
    <xsl:variable name="scale-length-guess" select="$tmp-length-guess"/>
    <xsl:variable name="tmp-length">
        <xsl:call-template name="strip-zeros">
            <xsl:with-param name="value" select="$tmp-length-guess"/>
        </xsl:call-template>
    </xsl:variable>
		<!-- lengte van schaalbalk in meters -->
    <xsl:variable name="tmp-length-rounded" select="format-number($tmp-length,'0','MyFormat')"/>
    <xsl:choose>
        <xsl:when test="$tmp-length-rounded > $tmp-length">
            <xsl:value-of select="$scale-length-guess * $tmp-length-rounded div $tmp-length"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$scale-length-guess * ($tmp-length-rounded + 1) div $tmp-length"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- berekent de lengte van een segment van de schaalbalk in pixels -->
<xsl:template name="calc-scale-px">
    <xsl:param name="width-m"/>
    <xsl:param name="width-px"/>
    <xsl:variable name="screen-scale" select="$width-px div $width-m"/>
    <xsl:variable name="scale-length">
        <xsl:call-template name="calc-scale-m">
            <xsl:with-param name="width-m" select="$width-m"/>
            <xsl:with-param name="width-px" select="$width-px"/>
        </xsl:call-template>
    </xsl:variable>

    <!-- lengte in pixels van schaalbalk -->
    <xsl:value-of select="$scale-length*$screen-scale"/>
</xsl:template>

<!-- tekent schaalstok dmv svg -->
<xsl:template name="create-scale">
    <xsl:param name="width"/>
    <xsl:param name="label"/>
    <xsl:param name="unit"/>
    <xsl:variable name="text-height" select="'6'"/>
    <xsl:variable name="text-offset" select="'2'"/>
    <xsl:variable name="scale-top" select="'4'"/>
    <xsl:variable name="scale-left" select="'0'"/>
    <xsl:variable name="scale-height" select="'3'"/>
    <xsl:variable name="scale-segment-width" select="$width"/>
    <xsl:variable name="dash-height" select="'2'"/>
    <xsl:variable name="scale-1">
        <xsl:value-of select="$text-offset +$scale-left"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + $scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + $scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
    </xsl:variable>
    <xsl:variable name="scale-2">
        <xsl:value-of select="$text-offset +$scale-left + $scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 2*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 2*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + $scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + $scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
    </xsl:variable>
    <xsl:variable name="scale-3">
        <xsl:value-of select="$text-offset +$scale-left + 2*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 3*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 3*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 2*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top + $scale-height"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$text-offset +$scale-left + 2*$scale-segment-width"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$text-height + $scale-top"/>
        <xsl:text> </xsl:text>
    </xsl:variable>

    <fo:instream-foreign-object font-size="7pt" xsl:use-attribute-sets="default-font">
        <svg xmlns="http://www.w3.org/2000/svg" width="3cm" height="0.6cm" preserveAspectRatio="xMaxYMax meet">
            <g font-size="7pt" xsl:use-attribute-sets="default-font">
                <polygon points="{$scale-1}" fill="black" stroke="black" stroke-width="0.5"/>
                <polygon points="{$scale-2}" fill="white" stroke="black" stroke-width="0.5"/>
                <polygon points="{$scale-3}" fill="black" stroke="black" stroke-width="0.5"/>
                <line x1="{$text-offset +$scale-left}" y1="{$text-height + $scale-top - $dash-height}" x2="{$text-offset +$scale-left}" y2="{$text-height + $scale-top}" stroke="black" stroke-width="0.5"/>
                <line x1="{$text-offset +$scale-left + $scale-segment-width}" y1="{$text-height + $scale-top - $dash-height}" x2="{$text-offset +$scale-left + $scale-segment-width}" y2="{$text-height + $scale-top}" stroke="black" stroke-width="0.5"/>
                <line x1="{$text-offset +$scale-left + 2*$scale-segment-width}" y1="{$text-height + $scale-top - $dash-height}" x2="{$text-offset +$scale-left + 2*$scale-segment-width}" y2="{$text-height + $scale-top}" stroke="black" stroke-width="0.5"/>
                <line x1="{$text-offset +$scale-left + 3*$scale-segment-width}" y1="{$text-height + $scale-top - $dash-height}" x2="{$text-offset +$scale-left + 3*$scale-segment-width}" y2="{$text-height + $scale-top}" stroke="black" stroke-width="0.5"/>
                <text x="{$scale-left}" y="{$text-height}">0</text>
                <text x="{$scale-left + $scale-segment-width}" y="{$text-height}">
                    <xsl:value-of select="$label"/>
                </text>
                <text x="{$scale-left + 2*$scale-segment-width}" y="{$text-height}">
                    <xsl:value-of select="2*$label"/>
                </text>
                <text x="{$scale-left + 3*$scale-segment-width}" y="{$text-height}">
                    <xsl:value-of select="3*$label"/>
                    <xsl:value-of select="$unit"/>
                </text>
            </g>
        </svg>
    </fo:instream-foreign-object>
</xsl:template>

</xsl:stylesheet>