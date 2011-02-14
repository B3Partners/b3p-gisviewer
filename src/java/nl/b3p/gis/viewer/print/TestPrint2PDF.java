package nl.b3p.gis.viewer.print;

import java.io.File;

public class TestPrint2PDF {

    public static void main(String[] args) {
        try {
            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            File xsltfile = new File(baseDir, "input/xslt/persoon2fo.xsl");
            File pdffile = new File(outDir, "Kaart.pdf");

            PrintInfo p = new PrintInfo();
            p.setNaam("Boy");
            p.setLeeftijd(28);
            p.setDatum("1 sep 2009");

            PrintExample2PDF example = new PrintExample2PDF();
            example.convertPersoonToPDF(p, xsltfile, pdffile);

            System.out.println("PDF staat in "+ pdffile);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
}
