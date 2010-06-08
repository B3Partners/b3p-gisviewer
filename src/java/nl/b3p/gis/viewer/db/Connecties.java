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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Roy Braam
 * @deprecated use Bron. Alleen de connectie specifieke hulp functies staan hier nog. Maar moeten eigenlijk
 * ook vervallen. Gewoon gebruik maken van de Bron.toDatastore() functie.
 */
public class Connecties extends Bron {

    private static final Log log = LogFactory.getLog(Connecties.class);
    public static final String TYPE_JDBC = "jdbc";
    public static final String TYPE_WFS = "wfs";
    public static final String TYPE_EMPTY = "empty";
    public static final String DIALECT_POSTGRESQL = "postgresql";
    public static final String DIALECT_MYSQL = "mysql";

    private String dialect=null;
    private String type=null;

    /** Creates a new instance of Connectie */
    public Connecties() {
    }

    @Override
    public void setUrl(String url) {
        super.setUrl(url);
        type = null;
        dialect = null;
        if (url != null && url.length() > 0) {
            //controleer of het een jdbc connectie is
            if (url.toLowerCase().startsWith("jdbc:")) {
                this.setType(TYPE_JDBC);
                //als postgres is:                
                if (url.toLowerCase().substring(5, url.length()).startsWith("postgresql:")) {
                    this.setDialect(DIALECT_POSTGRESQL);
                } else if (url.toLowerCase().substring(5, url.length()).startsWith("mysql:")) {
                    this.setDialect(DIALECT_MYSQL);
                }
            } else {
                this.setType(TYPE_WFS);
            }
        }
    }

    public Connection getJdbcConnection() throws SQLException {
        if (!TYPE_JDBC.equalsIgnoreCase(getType())) {
            return null;
        }
        try {
            if (DIALECT_POSTGRESQL.equalsIgnoreCase(getDialect())) {
                Class.forName("org.postgresql.Driver");
            } else if (DIALECT_MYSQL.equalsIgnoreCase(getDialect())) {
                Class.forName("com.mysql.jdbc.Driver");
            } else {
                return null;
            }
        } catch (ClassNotFoundException cfe) {
            log.error("Kan db driver niet laden ", cfe);
        }
        return DriverManager.getConnection(this.getUrl(), getGebruikersnaam(), getWachtwoord());
    }
    public void setType(String type){
        this.type=type;
    }
    public String getType() {
        return type;
    }
    public void setDialect(String dialect){
        this.dialect=dialect;
    }
    public String getDialect() {
        return dialect;
    }    
}
