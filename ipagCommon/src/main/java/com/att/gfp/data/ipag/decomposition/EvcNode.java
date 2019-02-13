package com.att.gfp.data.ipag.decomposition;

public class EvcNode {
	
	private String instanceName;
	private String classification;
	private String domain;
	private String source;
	private String acnaban;
	private String vrfName;
	private String unickt;
	private String evcName;
	
	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getAcnaban() {
		return acnaban;
	}

	public void setAcnaban(String acnaban) {
		this.acnaban = acnaban;
	}

	public String getVrfName() {
		return vrfName;
	}

	public void setVrfName(String vrfName) {
		this.vrfName = vrfName;
	}

	public String getUnickt() {
		return unickt;
	}

	public void setUnickt(String unickt) {
		this.unickt = unickt;
	}

	public String getEvcName() {
		return evcName;
	}

	public void setEvcName(String evcName) {
		this.evcName = evcName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public String toString() {
		return "EvcNode [instanceName=" + instanceName + ", classification="
				+ classification + ", domain=" + domain + ", source=" + source
				+ ", acnaban=" + acnaban + ", vrfName=" + vrfName + ", unickt="
				+ unickt + ", evcName=" + evcName + "]";
	}



}
