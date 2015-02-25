/*
 * Copyright (C) 2013 b3partners
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

/**
 *
 * @author b3partners
 */
public class CrossWebAppUtil {

    private static final Log log = LogFactory.getLog(CrossWebAppUtil.class);

    public Map<String, String> getGisviewerThemes() throws ServletException, IOException {
        String strThemes = null;
        
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = null;

        if (ctx != null) {
            request = ctx.getHttpServletRequest();
        }

        String url = getServerURL(request) + "/gisviewer/GetThemes";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (connection != null) {
            InputStream input = connection.getInputStream();
            strThemes = IOUtils.toString(input, "UTF-8");
        }

        /* Convert String to map to fill option box */
        Map<String, String> map = new HashMap<String, String>();
        
        if (strThemes != null && !strThemes.equals("")) {
            String[] arr = strThemes.split(",");
            
            for (int i = 0; i < arr.length; i++) {
                String string = arr[i];     
                map.put(string, string);
            }
        }

        return map;
    }

    private static String getServerURL(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
