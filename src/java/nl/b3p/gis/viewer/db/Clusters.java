/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.db;

import java.util.Set;

public class Clusters {

    private Integer id;
    private String naam;
    private String omschrijving;
    private String metadatalink;
    private boolean default_cluster;
    private boolean hide_legend;
    private boolean hide_tree;
    private boolean background_cluster;
    private boolean extra_level;
    private boolean callable;
    private boolean default_visible;
    private Clusters parent;
    private Set children;
    private Set themas;

    /** Creates a new instance of Clusters */
    public Clusters() {
    }

    /** 
     * Return het ID van de Clusters.
     *
     * @return int ID van de clusters
     */
    // <editor-fold defaultstate="" desc="public int getId()">
    public Integer getId() {
        return id;
    }
    // </editor-fold>
    /** 
     * Set het ID van de Clusters.
     *
     * @param id int id
     */
    // <editor-fold defaultstate="" desc="public void setId(int id)">
    public void setId(Integer id) {
        this.id = id;
    }
    // </editor-fold>
    /** 
     * Return de naam van het cluster.
     *
     * @return String met de naam.
     */
    // <editor-fold defaultstate="" desc="public String getNaam()">
    public String getNaam() {
        return naam;
    }
    // </editor-fold>
    /** 
     * Set de naam van het cluster.
     *
     * @param naam String met de naam van het cluster.
     */
    // <editor-fold defaultstate="" desc="public void setNaam(String naam)">
    public void setNaam(String naam) {
        this.naam = naam;
    }
    // </editor-fold>
    /** 
     * Return de omschrijving van het cluster.
     *
     * @return String met de omschrijving van het cluster.
     */
    // <editor-fold defaultstate="" desc="public String getOmschrijving()">
    public String getOmschrijving() {
        return omschrijving;
    }
    // </editor-fold>
    /** 
     * Set de omschrijving van het cluster.
     *
     * @param omschrijving String met de omschrijving van het cluster.
     */
    // <editor-fold defaultstate="" desc="public void setOmschrijving(String omschrijving)">
    public void setOmschrijving(String omschrijving) {
        this.omschrijving = omschrijving;
    }
    // </editor-fold>
    /** 
     * Return de children van dit cluster.
     *
     * @return Set met alle children van dit cluster.
     *
     * @see Set
     */
    // <editor-fold defaultstate="" desc="public String getChildren()">
    public Set getChildren() {
        return children;
    }
    // </editor-fold>
    /** 
     * Set de children van dit cluster.
     *
     * @param children Set children met de children van dit cluster.
     *
     * @see Set
     */
    // <editor-fold defaultstate="" desc="public void setChildren(Set children)">
    public void setChildren(Set children) {
        this.children = children;
    }
    // </editor-fold>
    /** 
     * Return de themas van dit cluster.
     *
     * @return Set met alle themas van dit cluster.
     *
     * @see Set
     */
    // <editor-fold defaultstate="" desc="public String getThemas()">
    public Set getThemas() {
        return themas;
    }
    // </editor-fold>
    /** 
     * Set de themas van dit cluster.
     *
     * @param themas Set children met de themas van dit cluster.
     *
     * @see Set
     */
    // <editor-fold defaultstate="" desc="public void setThemas(Set themas)">
    public void setThemas(Set themas) {
        this.themas = themas;
    }
    // </editor-fold>
    /** 
     * Return de parent van dit cluster.
     *
     * @return Clusters met de parent van dit Cluster.
     */
    // <editor-fold defaultstate="" desc="public String getParent()">
    public Clusters getParent() {
        return parent;
    }
    // </editor-fold>
    /** 
     * Set de parent van dit cluster.
     *
     * @param parent Clusters met de parent van dit cluster.
     */
    // <editor-fold defaultstate="" desc="public void setParent(Clusters parent)">
    public void setParent(Clusters parent) {
        this.parent = parent;
    }

    /**
     * @return the default_cluster
     */
    public boolean isDefault_cluster() {
        return default_cluster;
    }

    /**
     * @param default_cluster the default_cluster to set
     */
    public void setDefault_cluster(boolean default_cluster) {
        this.default_cluster = default_cluster;
    }

    /**
     * @return the hide_legend
     */
    public boolean isHide_legend() {
        return hide_legend;
    }

    /**
     * @param hide_legend the hide_legend to set
     */
    public void setHide_legend(boolean hide_legend) {
        this.hide_legend = hide_legend;
    }

    /**
     * @return the hide_tree
     */
    public boolean isHide_tree() {
        return hide_tree;
    }

    /**
     * @param hide_tree the hide_tree to set
     */
    public void setHide_tree(boolean hide_tree) {
        this.hide_tree = hide_tree;
    }

    /**
     * @return the background_cluster
     */
    public boolean isBackground_cluster() {
        return background_cluster;
    }

    /**
     * @param background_cluster the background_cluster to set
     */
    public void setBackground_cluster(boolean background_cluster) {
        this.background_cluster = background_cluster;
    }

    /**
     * @return the extra_level
     */
    public boolean isExtra_level() {
        return extra_level;
    }

    /**
     * @param extra_level the extra_level to set
     */
    public void setExtra_level(boolean extra_level) {
        this.extra_level = extra_level;
    }

    /**
     * @return the callable
     */
    public boolean isCallable() {
        return callable;
    }

    /**
     * @param callable the callable to set
     */
    public void setCallable(boolean callable) {
        this.callable = callable;
    }

    /**
     * @return the metadatalink
     */
    public String getMetadatalink() {
        return metadatalink;
    }

    /**
     * @param metadatalink the metadatalink to set
     */
    public void setMetadatalink(String metadatalink) {
        this.metadatalink = metadatalink;
    }

    /**
     * @return the default_visible
     */
    public boolean isDefault_visible() {
        return default_visible;
    }

    /**
     * @param default_visible the default_visible to set
     */
    public void setDefault_visible(boolean default_visible) {
        this.default_visible = default_visible;
    }
    // </editor-fold>
}
