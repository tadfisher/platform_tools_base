//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.23 at 02:23:52 PM PDT 
//


package com.android.sdklib.repositoryv2.generated.sysimg;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import com.android.sdklib.repositorycore.impl.generated.RepositoryType;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.android.sdklib.repositoryv2.generated.sysimg package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SdkSysImg_QNAME = new QName("http://schemas.android.com/sdk/android/sys-img/4", "sdk-sys-img");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.android.sdklib.repositoryv2.generated.sysimg
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SysimgDetailsType }
     * 
     */
    public SysimgDetailsType createSysimgDetailsType() {
        return new SysimgDetailsType();
    }

    /**
     * Create an instance of {@link AddonType }
     * 
     */
    public AddonType createAddonType() {
        return new AddonType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RepositoryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.android.com/sdk/android/sys-img/4", name = "sdk-sys-img")
    public JAXBElement<RepositoryType> createSdkSysImg(RepositoryType value) {
        return new JAXBElement<RepositoryType>(_SdkSysImg_QNAME, RepositoryType.class, null, value);
    }

}
