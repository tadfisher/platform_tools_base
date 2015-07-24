//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.24 at 04:32:37 PM PDT 
//


package com.android.sdklib.repositorycore.impl.generated;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for archiveType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="archiveType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="size" type="{http://www.w3.org/2001/XMLSchema}positiveInteger"/>
 *         &lt;element name="checksum" type="{http://schemas.android.com/sdk/android/common/01}checksumType"/>
 *         &lt;element name="url" type="{http://www.w3.org/2001/XMLSchema}token"/>
 *         &lt;element name="host-os" type="{http://schemas.android.com/sdk/android/common/01}osType" minOccurs="0"/>
 *         &lt;element name="host-bits" type="{http://schemas.android.com/sdk/android/common/01}bitSizeType" minOccurs="0"/>
 *         &lt;element name="jvm-bits" type="{http://schemas.android.com/sdk/android/common/01}bitSizeType" minOccurs="0"/>
 *         &lt;element name="min-jvm-version" type="{http://schemas.android.com/sdk/android/common/01}jvmVersionType" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "archiveType", propOrder = {

})
public class ArchiveType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger size;
    @XmlElement(required = true)
    protected ChecksumType checksum;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String url;
    @XmlElement(name = "host-os")
    @XmlSchemaType(name = "token")
    protected OsType hostOs;
    @XmlElement(name = "host-bits")
    @XmlSchemaType(name = "token")
    protected BitSizeType hostBits;
    @XmlElement(name = "jvm-bits")
    @XmlSchemaType(name = "token")
    protected BitSizeType jvmBits;
    @XmlElement(name = "min-jvm-version")
    protected String minJvmVersion;

    /**
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSize(BigInteger value) {
        this.size = value;
    }

    /**
     * Gets the value of the checksum property.
     * 
     * @return
     *     possible object is
     *     {@link ChecksumType }
     *     
     */
    public ChecksumType getChecksum() {
        return checksum;
    }

    /**
     * Sets the value of the checksum property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChecksumType }
     *     
     */
    public void setChecksum(ChecksumType value) {
        this.checksum = value;
    }

    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the hostOs property.
     * 
     * @return
     *     possible object is
     *     {@link OsType }
     *     
     */
    public OsType getHostOs() {
        return hostOs;
    }

    /**
     * Sets the value of the hostOs property.
     * 
     * @param value
     *     allowed object is
     *     {@link OsType }
     *     
     */
    public void setHostOs(OsType value) {
        this.hostOs = value;
    }

    /**
     * Gets the value of the hostBits property.
     * 
     * @return
     *     possible object is
     *     {@link BitSizeType }
     *     
     */
    public BitSizeType getHostBits() {
        return hostBits;
    }

    /**
     * Sets the value of the hostBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link BitSizeType }
     *     
     */
    public void setHostBits(BitSizeType value) {
        this.hostBits = value;
    }

    /**
     * Gets the value of the jvmBits property.
     * 
     * @return
     *     possible object is
     *     {@link BitSizeType }
     *     
     */
    public BitSizeType getJvmBits() {
        return jvmBits;
    }

    /**
     * Sets the value of the jvmBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link BitSizeType }
     *     
     */
    public void setJvmBits(BitSizeType value) {
        this.jvmBits = value;
    }

    /**
     * Gets the value of the minJvmVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMinJvmVersion() {
        return minJvmVersion;
    }

    /**
     * Sets the value of the minJvmVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMinJvmVersion(String value) {
        this.minJvmVersion = value;
    }

}
