package nl.b3p.gis.geotools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.geotools.xml.schema.Attribute;
import org.geotools.xml.schema.AttributeGroup;
import org.geotools.xml.schema.ComplexType;
import org.geotools.xml.schema.Element;
import org.geotools.xml.schema.Group;
import org.geotools.xml.schema.Schema;
import org.geotools.xml.schema.SimpleType;

public class APPSchema implements Schema {

    private static APPSchema instance = new APPSchema();

    protected APPSchema() {
        //do nothing
    }

    public static APPSchema getInstance() {
        return instance;
    }
    public static final URI NAMESPACE = loadNS();

    private static URI loadNS() {
        try {
            return new URI("http://www.deegree.org/app");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public int getBlockDefault() {
        return 0;
    }

    public int getFinalDefault() {
        return 0;
    }

    public String getId() {
        return "null";
    }

    public Schema[] getImports() {
        return null;
    }

    public String getPrefix() {
        return "app";
    }

    public URI getTargetNamespace() {
        return NAMESPACE;
    }

    public URI getURI() {
        return NAMESPACE;
    }

    public String getVersion() {
        return "null";
    }

    public boolean includesURI(URI uri) {
        return false;
    }

    public boolean isAttributeFormDefault() {
        return false;
    }

    public boolean isElementFormDefault() {
        return false;
    }

    public AttributeGroup[] getAttributeGroups() {
        return null;
    }

    public Attribute[] getAttributes() {
        return null;
    }

    public ComplexType[] getComplexTypes() {
        return null;
    }

    public Element[] getElements() {
        return null;
    }

    public Group[] getGroups() {
        return null;
    }

    public SimpleType[] getSimpleTypes() {
        return null;
    }

    /**
     * Returns the implementation hints. The default implementation returns en empty map.
     */
    public Map getImplementationHints() {
        return Collections.EMPTY_MAP;
    }
}
