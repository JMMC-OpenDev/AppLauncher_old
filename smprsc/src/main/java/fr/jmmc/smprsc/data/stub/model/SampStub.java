
package fr.jmmc.smprsc.data.stub.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 JMMC AppLauncher Stub meta data.
 *             
 * 
 * <p>Java class for SampStub complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SampStub">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="metadata" type="{}Metadata" maxOccurs="unbounded"/>
 *         &lt;element name="subscription" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="uid" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lag" type="{http://www.w3.org/2001/XMLSchema}integer" default="-1" />
 *       &lt;attribute name="type" type="{}Type" default="JNLP" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SampStub", propOrder = {
    "metadatas",
    "subscriptions"
})
@XmlRootElement(name = "SampStub")
public class SampStub {

    @XmlElement(name = "metadata", required = true)
    protected List<Metadata> metadatas;
    @XmlElement(name = "subscription", required = true)
    protected List<String> subscriptions;
    @XmlAttribute(name = "uid", required = true)
    protected String uid;
    @XmlAttribute(name = "lag")
    protected BigInteger lag;
    @XmlAttribute(name = "type")
    protected Type type;

    /**
     * Gets the value of the metadatas property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the metadatas property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMetadatas().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Metadata }
     * 
     * 
     */
    public List<Metadata> getMetadatas() {
        if (metadatas == null) {
            metadatas = new ArrayList<Metadata>();
        }
        return this.metadatas;
    }

    public boolean isSetMetadatas() {
        return ((this.metadatas!= null)&&(!this.metadatas.isEmpty()));
    }

    public void unsetMetadatas() {
        this.metadatas = null;
    }

    /**
     * Gets the value of the subscriptions property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subscriptions property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubscriptions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getSubscriptions() {
        if (subscriptions == null) {
            subscriptions = new ArrayList<String>();
        }
        return this.subscriptions;
    }

    public boolean isSetSubscriptions() {
        return ((this.subscriptions!= null)&&(!this.subscriptions.isEmpty()));
    }

    public void unsetSubscriptions() {
        this.subscriptions = null;
    }

    /**
     * Gets the value of the uid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the value of the uid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUid(String value) {
        this.uid = value;
    }

    public boolean isSetUid() {
        return (this.uid!= null);
    }

    /**
     * Gets the value of the lag property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLag() {
        if (lag == null) {
            return new BigInteger("-1");
        } else {
            return lag;
        }
    }

    /**
     * Sets the value of the lag property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLag(BigInteger value) {
        this.lag = value;
    }

    public boolean isSetLag() {
        return (this.lag!= null);
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link Type }
     *     
     */
    public Type getType() {
        if (type == null) {
            return Type.JNLP;
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link Type }
     *     
     */
    public void setType(Type value) {
        this.type = value;
    }

    public boolean isSetType() {
        return (this.type!= null);
    }

}
