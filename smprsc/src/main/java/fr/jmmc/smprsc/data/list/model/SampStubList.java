
package fr.jmmc.smprsc.data.list.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 JMMC AppLauncher Stub list.
 *             
 * 
 * <p>Java class for SampStubList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SampStubList">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="family" type="{}Family" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SampStubList", propOrder = {
    "families"
})
@XmlRootElement(name = "SampStubList")
public class SampStubList {

    @XmlElement(name = "family", required = true)
    protected List<Family> families;

    /**
     * Gets the value of the families property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the families property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFamilies().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Family }
     * 
     * 
     */
    public List<Family> getFamilies() {
        if (families == null) {
            families = new ArrayList<Family>();
        }
        return this.families;
    }

    public boolean isSetFamilies() {
        return ((this.families!= null)&&(!this.families.isEmpty()));
    }

    public void unsetFamilies() {
        this.families = null;
    }

}
