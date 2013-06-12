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

import java.net.URLEncoder;
import java.text.Normalizer;
import org.apache.log4j.Logger;

public class Slugify {

    private static final Logger logger = Logger.getLogger(Slugify.class);

    /**
     *
     * modified version of Jozef Ševcík's slugify
     *
     * @link http://maddemcode.com/java/seo-friendly-urls-using-slugify-in-java/
     *
     * @param input
     * @return formatted URL
     */
    public static String slugify(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        try {
            String toReturn = normalize(input);
            toReturn = toReturn.replaceAll("[^\\w\\s\\-]", "");
            toReturn = toReturn.replace(" ", "-");
            toReturn = toReturn.toLowerCase();
            toReturn = URLEncoder.encode(toReturn, "UTF-8");
            return toReturn;
        } catch (Exception e) {
            logger.error(e, e);
        }
        return "";

    }

    private static String normalize(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}