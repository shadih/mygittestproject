//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.10 at 07:53:45 PM CDT 
//


package com.att.gfp.data.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type. 
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cascadeTargets">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="cascadeTarget" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="enableCascading" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *                             &lt;element name="scenarioName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *                             &lt;element name="eventNames">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="eventName" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                                     &lt;/sequence>
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                           &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "cascadeTargets"
})
@XmlRootElement(name = "CascadePolicies")
public class CascadePolicies {

    @XmlElement(required = true)
    protected CascadePolicies.CascadeTargets cascadeTargets;

    /**
     * Gets the value of the cascadeTargets property.
     * 
     * @return
     *     possible object is
     *     {@link CascadePolicies.CascadeTargets }
     *     
     */
    public CascadePolicies.CascadeTargets getCascadeTargets() {
        return cascadeTargets;
    }

    /**
     * Sets the value of the cascadeTargets property.
     * 
     * @param value
     *     allowed object is
     *     {@link CascadePolicies.CascadeTargets }
     *     
     */
    public void setCascadeTargets(CascadePolicies.CascadeTargets value) {
        this.cascadeTargets = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="cascadeTarget" maxOccurs="unbounded" minOccurs="0">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="enableCascading" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
     *                   &lt;element name="scenarioName" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}float"/>
     *                   &lt;element name="eventNames">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="eventName" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     *                           &lt;/sequence>
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *                 &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "cascadeTarget"
    })
    public static class CascadeTargets {

        protected List<CascadePolicies.CascadeTargets.CascadeTarget> cascadeTarget;

        /**
         * Gets the value of the cascadeTarget property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the cascadeTarget property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCascadeTarget().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link CascadePolicies.CascadeTargets.CascadeTarget }
         * 
         * 
         */
        public List<CascadePolicies.CascadeTargets.CascadeTarget> getCascadeTarget() {
            if (cascadeTarget == null) {
                cascadeTarget = new ArrayList<CascadePolicies.CascadeTargets.CascadeTarget>();
            }
            return this.cascadeTarget;
        }

        public CascadeTarget getCascadeTargetByName(String cascadeTargetName) {
        	CascadeTarget cascadeTargetN = null;
        	List<CascadePolicies.CascadeTargets.CascadeTarget> cascadeTargets = getCascadeTarget();
        	for(CascadePolicies.CascadeTargets.CascadeTarget cascadeTarget : cascadeTargets) {
        		if(cascadeTargetName.equals(cascadeTarget.getName())) {
        			cascadeTargetN = cascadeTarget;
        		}
        	}
        	return cascadeTargetN;         
        }

        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="enableCascading" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
         *         &lt;element name="scenarioName" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}float"/>
         *         &lt;element name="eventNames">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="eventName" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
         *                 &lt;/sequence>
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "enableCascading",
            "vpName",
            "scenarioName",
            "version",  
            "eventNames"
        })
        public static class CascadeTarget {

            protected boolean enableCascading;
            @XmlElement(required = true)
            protected String vpName;
			@XmlElement(required = true)
            protected String scenarioName;
            protected float version;
            @XmlElement(required = true)
            protected CascadePolicies.CascadeTargets.CascadeTarget.EventNames eventNames;
            @XmlAttribute(name = "name")
            protected String name;

            /**
             * Gets the value of the enableCascading property.
             * 
             */
            public boolean isEnableCascading() {
                return enableCascading;
            }

            /**
             * Sets the value of the enableCascading property.
             * 
             */
            public void setEnableCascading(boolean value) {
                this.enableCascading = value;
            }
            
            /**
             * Gets the vpName
             * @return
             */
            public String getVpName() {
				return vpName;
			}
            /**
             * Sets the VP Name
             * @param vpName
             */
			public void setVpName(String vpName) {
				this.vpName = vpName;
			}

            /**
             * Gets the value of the scenarioName property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getScenarioName() {
                return scenarioName;
            }

            /**
             * Sets the value of the scenarioName property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setScenarioName(String value) {
                this.scenarioName = value;
            }

            /**
             * Gets the value of the version property.
             * 
             */
            public float getVersion() {
                return version;
            }

            /**
             * Sets the value of the version property.
             * 
             */
            public void setVersion(float value) {
                this.version = value;
            }

            /**
             * Gets the value of the eventNames property.
             * 
             * @return
             *     possible object is
             *     {@link CascadePolicies.CascadeTargets.CascadeTarget.EventNames }
             *     
             */
            public CascadePolicies.CascadeTargets.CascadeTarget.EventNames getEventNames() {
                return eventNames;
            }

            /**
             * Sets the value of the eventNames property.
             * 
             * @param value
             *     allowed object is
             *     {@link CascadePolicies.CascadeTargets.CascadeTarget.EventNames }
             *     
             */
            public void setEventNames(CascadePolicies.CascadeTargets.CascadeTarget.EventNames value) {
                this.eventNames = value;
            }

            /**
             * Gets the value of the name property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getName() {
                return name;
            }

            /**
             * Sets the value of the name property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setName(String value) {
                this.name = value;
            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="eventName" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
             *       &lt;/sequence>
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "eventName"
            })
            public static class EventNames {

                protected List<String> eventName;

                /**
                 * Gets the value of the eventName property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the JAXB object.
                 * This is why there is not a <CODE>set</CODE> method for the eventName property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getEventName().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link String }
                 * 
                 * 
                 */
                public List<String> getEventName() {
                    if (eventName == null) {
                        eventName = new ArrayList<String>();
                    }
                    return this.eventName;
                }

            }

        }

    }

}
