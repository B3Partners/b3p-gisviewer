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
package nl.b3p.gis.viewer;

import java.util.ArrayList;

public class LayerItem {

    private String name;
    private boolean checked = false;
    private boolean clickAction = false;
    private boolean cluster = false;
    private ArrayList childs = null;
    private ArrayList adminData = null;
    private ArrayList labelData = null;

    /** 
     * Creates a new instance of LayerItem.
     */
    // <editor-fold defaultstate="" desc="public LayerItem()">
    public LayerItem() {
    }
    // </editor-fold>
    /** 
     * Creates a new instance of LayerItem.
     *
     * @param n String met de naam van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public LayerItem(String n)">
    public LayerItem(String n) {
        name = n;
    }
    // </editor-fold>
    /** 
     * Creates a new instance of LayerItem.
     *
     * Wanneer een LayerItem als cluster boolean de waarde 'true' meekrijgt wordt deze in uitgeklapte
     * vorm getoond bij het laden van het scherm. Bij een 'false' waarde wordt ze ingeklapt weergegeven.
     *
     * Indien het LayerItem een layer is wordt deze bij de waarde 'true' aangevinkt weergegeven anders uitgevinkt.
     *
     * @param n String met de naam van dit layer item.
     * @param c boolean om aan te geven of dit layer item een cluster is.
     */
    // <editor-fold defaultstate="" desc="public LayerItem(String n, boolean c)">
    public LayerItem(String n, boolean c) {
        name = n;
        cluster = c;
    }
    // </editor-fold>
    /** 
     * Return de naam van dit layer item.
     *
     * @return String met de naam van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public String getName()">
    public String getName() {
        return name;
    }
    // </editor-fold>
    /** 
     * Set de naam van dit layer item.
     *
     * @param name String met de naam van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void setName(String name)">
    public void setName(String name) {
        this.name = name;
    }
    // </editor-fold>
    /** 
     * Returns een boolean of dit layer item gechecked is.
     *
     * @return boolean true als dit layer item gechecked is, anders false.
     */
    // <editor-fold defaultstate="" desc="public boolean isChecked()">
    public boolean isChecked() {
        return checked;
    }
    // </editor-fold>
    /** 
     * Set dit layer item als gechecked. Als dit layer item gechecked is zet deze dan true, anders false.
     *
     * @param checked boolean met true als dit layer item gechecked is.
     */
    // <editor-fold defaultstate="" desc="public void setChecked(boolean checked)">
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
    // </editor-fold>
    /** 
     * Returns een boolean of dit layer item een click action heeft.
     *
     * @return boolean true als dit layer item een click action heeft, anders false.
     */
    // <editor-fold defaultstate="" desc="public boolean isClickAction()">   
    public boolean isClickAction() {
        return clickAction;
    }
    // </editor-fold>
    /** 
     * Set dit layer item met een click action. Als dit layer item een click action heeft zet deze dan true, anders false.
     *
     * @param clickAction boolean met true als dit layer item een click action heeft.
     */
    // <editor-fold defaultstate="" desc="public void setClickAction(boolean clickAction)">    
    public void setClickAction(boolean clickAction) {
        this.clickAction = clickAction;
    }
    // </editor-fold>
    /** 
     * Returns een boolean of dit layer item een cluster is.
     *
     * Wanneer een LayerItem als cluster boolean de waarde 'true' meekrijgt wordt deze in uitgeklapte
     * vorm getoond bij het laden van het scherm. Bij een 'false' waarde wordt ze ingeklapt weergegeven.
     *
     * Indien het LayerItem een layer is wordt deze bij de waarde 'true' aangevinkt weergegeven anders uitgevinkt.
     *
     * @return boolean true als dit layer item een cluster is, anders false.
     */
    // <editor-fold defaultstate="" desc="public boolean isCluster()">    
    public boolean isCluster() {
        return cluster;
    }
    // </editor-fold>
    /** 
     * Set dit layer item als gechecked. Als dit layer item een cluster is zet deze dan true, anders false.
     *
     * Wanneer een LayerItem als cluster boolean de waarde 'true' meekrijgt wordt deze in uitgeklapte
     * vorm getoond bij het laden van het scherm. Bij een 'false' waarde wordt ze ingeklapt weergegeven.
     *
     * Indien het LayerItem een layer is wordt deze bij de waarde 'true' aangevinkt weergegeven anders uitgevinkt.
     *
     * @param cluster boolean met true als dit layer item een cluster is.
     */
    // <editor-fold defaultstate="" desc="public void setCluster(boolean cluster)">  
    public void setCluster(boolean cluster) {
        this.cluster = cluster;
    }
    // </editor-fold>
    /** 
     * Return een lijst met kinderen van dit layer item.
     *
     * @return ArrayList met een lijst met kinderen van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public ArrayList getChilds()">
    public ArrayList getChilds() {
        return childs;
    }
    // </editor-fold>
    /** 
     * Set een lijst met kinderen van dit layer item.
     *
     * @param childs ArrayList met een lijst met kinderen van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void setChilds(ArrayList childs)">
    public void setChilds(ArrayList childs) {
        this.childs = childs;
    }
    // </editor-fold>
    /** 
     * Voeg een nieuw item toe aan de lijst met childs binnen dit layer item.
     *
     * @param li LayerItem.
     */
    // <editor-fold defaultstate="" desc="public void addChild(LayerItem li)">        
    public void addChild(LayerItem li) {
        if (childs == null) {
            childs = new ArrayList();
        }
        childs.add(li);
    }
    // </editor-fold>
    /** 
     * Return een lijst met admin data van dit layer item.
     *
     * @return ArrayList met een lijst met admin data van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public ArrayList getAdminData()">    
    public ArrayList getAdminData() {
        return adminData;
    }
    // </editor-fold>
    /** 
     * Set een lijst met admin data van dit layer item.
     *
     * @param adminData ArrayList met een lijst met admin data van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void setAdminData(ArrayList adminData)">
    public void setAdminData(ArrayList adminData) {
        this.adminData = adminData;
    }
    // </editor-fold>
    /** 
     * Voeg nieuwe admin data aan dit layer item toe.
     *
     * @param s String[] met een lijst met admin data voor dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void addAdmindata(String[] s)">
    public void addAdmindata(String[] s) {
        if (adminData == null) {
            adminData = new ArrayList();
        }
        for (int i = 0; i < s.length; i++) {
            adminData.add(s[i]);
        }
    }
    // </editor-fold>
    /** 
     * Return de label data van dit layer item.
     *
     * @return ArrayList met de label data van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public ArrayList getLabelData()">
    public ArrayList getLabelData() {
        return labelData;
    }
    // </editor-fold>
    /** 
     * Set de label data van dit layer item.
     *
     * @param labelData ArrayList met de label data van dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void setLabelData(ArrayList labelData)">
    public void setLabelData(ArrayList labelData) {
        this.labelData = labelData;
    }
    // </editor-fold>
    /** 
     * Voeg nieuwe label data aan dit layer item toe.
     *
     * @param s String[] met een lijst met label data voor dit layer item.
     */
    // <editor-fold defaultstate="" desc="public void addLabelData(String s[])">    
    public void addLabelData(String s[]) {
        if (labelData == null) {
            labelData = new ArrayList();
        }
        for (int i = 0; i < s.length; i++) {
            labelData.add(s[i]);
        }
    }
    // </editor-fold>
}