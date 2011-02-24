package nl.b3p.gis.viewer.print;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestPrint2PDF {

    private static final int MAX_QUALITY = 2048;

    public static void main(String[] args) {
        try {
            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            File xsltfile = new File(baseDir, "input/A4_Liggend.xsl");
            File pdffile = new File(outDir, "Kaart.pdf");

            Date now = new Date();
            SimpleDateFormat df = new SimpleDateFormat("dd-M-yyyy", new Locale("NL"));

            PrintInfo info = new PrintInfo();

            info.setTitel("Test titel");
            info.setDatum(df.format(now));

            /* kwaliteit is eigenlijk de width in een nieuwe getMap request */
            info.setKwaliteit(640);
            info.setImageUrl("http://192.168.1.14:8084/kaartenbalie/services/fc050d16f589d1f82ffd43beba38f933?&SERVICE=WMS&REQUEST=GetMap&TIMEOUT=30&RATIO=1&STYLES=&TRANSPARENT=TRUE&SRS=EPSG:28992&VERSION=1.1.1&EXCEPTIONS=application/vnd.ogc.se_inimage&LAYERS=osmsc_OpenStreetMap2&FORMAT=image/png&HEIGHT=700&WIDTH=983&BBOX=194148.607142857,353313.865353873,201743.964285714,358722.563217556");
            info.setBbox("194148.607142857,353313.865353873,201743.964285714,358722.563217556");
            info.setMapWidth(983);
            info.setMapHeight(700);

            PrintExample2PDF example = new PrintExample2PDF();
            example.convert2PDF(info, xsltfile, pdffile);

            System.out.println("PDF staat in "+ pdffile);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }   
}
