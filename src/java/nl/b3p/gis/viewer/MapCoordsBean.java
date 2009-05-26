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

public class MapCoordsBean {

    private String naam;
    private String minx;
    private String miny;
    private String maxx;
    private String maxy;

    /** Creates a new instance of MapCoordsBean */
    public MapCoordsBean() {
    }

    /** 
     * Return de naam van de map coordinaten bean.
     *
     * @return String met de naam van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public String getNaam()">
    public String getNaam() {
        return naam;
    }
    // </editor-fold>
    /** 
     * Set de naam van de map coordinaten bean.
     *
     * @param naam String met de naam van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public void setNaam(String naam)">
    public void setNaam(String naam) {
        this.naam = naam;
    }
    // </editor-fold>
    /** 
     * Return de minimale x waarde van de map coordinaten bean.
     *
     * @return String met de minimale x waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public String getMinx()">
    public String getMinx() {
        return minx;
    }
    // </editor-fold>
    /** 
     * Set de minimale x waarde van de map coordinaten bean.
     *
     * @param minx String met de minimale x waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public void setMinx(String minx)>
    public void setMinx(String minx) {
        this.minx = minx;
    }
    // </editor-fold>
    /** 
     * Return de minimale y waarde van de map coordinaten bean.
     *
     * @return String met de minimale y waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public String getMiny()">
    public String getMiny() {
        return miny;
    }
    // </editor-fold>
    /** 
     * Set de minimale y waarde van de map coordinaten bean.
     *
     * @param miny String met de minimale y waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public void setMiny(String miny)">
    public void setMiny(String miny) {
        this.miny = miny;
    }
    // </editor-fold>
    /** 
     * Return de maximale x waarde van de map coordinaten bean.
     *
     * @return String met de maximale x waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public String getMaxx()">
    public String getMaxx() {
        return maxx;
    }
    // </editor-fold>
    /** 
     * Set de maximale x waarde van de map coordinaten bean.
     *
     * @param maxx String met de maximale x waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public void setMaxx(String maxx)">
    public void setMaxx(String maxx) {
        this.maxx = maxx;
    }
    // </editor-fold>
    /** 
     * Return de maximale y waarde van de map coordinaten bean.
     *
     * @return String met de maximale y waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public String getMaxy()">
    public String getMaxy() {
        return maxy;
    }
    // </editor-fold>
    /** 
     * Set de maximale y waarde van de map coordinaten bean.
     *
     * @param maxy String met de maximale y waarde van de map coordinaten bean.
     */
    // <editor-fold defaultstate="" desc="public void setMaxy(String maxy)">
    public void setMaxy(String maxy) {
        this.maxy = maxy;
    }
    // </editor-fold>    
}
