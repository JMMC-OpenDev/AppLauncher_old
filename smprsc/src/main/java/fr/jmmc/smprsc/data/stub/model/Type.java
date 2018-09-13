
package fr.jmmc.smprsc.data.stub.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Type"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="JNLP"/&gt;
 *     &lt;enumeration value="WEB"/&gt;
 *     &lt;enumeration value="CLI"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "Type")
@XmlEnum
public enum Type {

    JNLP,
    WEB,
    CLI;

    public String value() {
        return name();
    }

    public static Type fromValue(String v) {
        return valueOf(v);
    }

}
