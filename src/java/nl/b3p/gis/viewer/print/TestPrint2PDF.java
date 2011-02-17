package nl.b3p.gis.viewer.print;

import java.io.File;
import java.util.Date;

public class TestPrint2PDF {

    public static void main(String[] args) {
        try {
            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            File xsltfile = new File(baseDir, "input/limburg_a4_liggend.xsl");
            File pdffile = new File(outDir, "Kaart.pdf");

            PrintInfo info = new PrintInfo();
            info.setTitel("Test titel");
            info.setOpmerking("Test opmerking");

            Date datum = new Date();
            info.setDatum(datum.toString());

            info.setKwaliteit(100);

            info.setOrientatie("liggend");
            info.setOutputFormaat("pdf");
            info.setPaginaFormaat("A4");

            PrintExample2PDF example = new PrintExample2PDF();
            example.convertPersoonToPDF(info, xsltfile, pdffile);

            System.out.println("PDF staat in "+ pdffile);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
}
