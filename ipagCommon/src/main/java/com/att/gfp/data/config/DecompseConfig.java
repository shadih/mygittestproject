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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Parses the decompose rules xml file
 * 
 * @author st133d
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)  
@XmlType(name = "", propOrder = {
    "decomposeRules",  
})  
@XmlRootElement(name = "DecompseConfig")  
public class DecompseConfig {      
 

    @XmlElement(required = true)
    protected DecompseConfig.DecomposeRules decomposeRules;    
	
	
    public DecompseConfig.DecomposeRules getDecomposeRules() {
		return decomposeRules;
	}


	public void setDecomposeRules(DecompseConfig.DecomposeRules decomposeRules) {
		this.decomposeRules = decomposeRules;
	}
 

	@XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "decomposeRule"
    })
    public static class DecomposeRules {

        protected List<DecompseConfig.DecomposeRules.DecomposeRule> decomposeRule;

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
        public List<DecompseConfig.DecomposeRules.DecomposeRule> getDecomposeRule() {
            if (decomposeRule == null) {
            	decomposeRule = new ArrayList<DecompseConfig.DecomposeRules.DecomposeRule>();
            }
            return this.decomposeRule; 
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
            "type",
            "order",
            "eventNames"
        })
        public static class DecomposeRule {

			@XmlElement(required = true) 
            protected String type;
			@XmlElement(required = true)
            protected String order;
			   
            public String getOrder() {
				return order;
			}

			public void setOrder(String order) {
				this.order = order;
			}

			public String getType() {  
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}


			protected DecompseConfig.DecomposeRules.DecomposeRule.EventNames eventNames;

             /**
             * Gets the value of the eventNames property.
             * 
             * @return
             *     possible object is   
             *     {@link CascadePolicies.CascadeTargets.CascadeTarget.EventNames }
             *     
             */
            public DecompseConfig.DecomposeRules.DecomposeRule.EventNames getEventNames() {
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
            public void setEventNames(DecompseConfig.DecomposeRules.DecomposeRule.EventNames value) {
                this.eventNames = value;
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
