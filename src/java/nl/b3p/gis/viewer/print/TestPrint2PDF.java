package nl.b3p.gis.viewer.print;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestPrint2PDF {

    public static void main(String[] args) {
        try {
            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            File xsltfile = new File(baseDir, "input/A4_Liggend.xsl");
            File pdffile = new File(outDir, "Kaart.pdf");

            PrintInfo info = new PrintInfo();
            info.setTitel("Test titel");
            info.setOpmerking("Test opmerking");

            Date datum = new Date();
            DateFormat df = new SimpleDateFormat("dd-M-yyyy");

            info.setDatum(df.format(datum));

            info.setKwaliteit(100);

            info.setOrientatie("liggend");
            info.setOutputFormaat("pdf");
            info.setPaginaFormaat("A4");

            info.setKwaliteit(1500);
            info.setBbox("89129.1428571428,296000,284324.857142857,435000");
            
            info.setImageUrl("http://192.168.1.14:8084/kaartenbalie/services/fc050d16f589d1f82ffd43beba38f933?&SERVICE=WMS&REQUEST=GetMap&TIMEOUT=30&RATIO=1&STYLES=&TRANSPARENT=TRUE&SRS=EPSG:28992&VERSION=1.1.1&EXCEPTIONS=application/vnd.ogc.se_inimage&LAYERS=demo_gemeenten_2006&FORMAT=image/png&HEIGHT=700&WIDTH=983");

            PrintExample2PDF example = new PrintExample2PDF();
            example.convertPersoonToPDF(info, xsltfile, pdffile);

            System.out.println("PDF staat in "+ pdffile);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
}
