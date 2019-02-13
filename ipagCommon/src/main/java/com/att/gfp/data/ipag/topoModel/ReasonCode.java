package com.att.gfp.data.ipag.topoModel;

public class ReasonCode {
	private String rVlanId;
	private String rDevName;
	public String getrVlanId() {
		return rVlanId;
	}
	public void setrVlanId(String rVlanId) {
		this.rVlanId = rVlanId;
	}
	public String getrDevName() {
		return rDevName;
	}
	public void setrDevName(String rDevName) {
		this.rDevName = rDevName;
	}
	public String getrNvlanIdTop() {
		return rNvlanIdTop;
	}
	public void setrNvlanIdTop(String rNvlanIdTop) {
		this.rNvlanIdTop = rNvlanIdTop;
	}
	public String getrAid() {
		return rAid;
	} 
	public void setrAid(String rAid) {
		this.rAid = rAid;
	}
	public String getrMdLevel() {
		return rMdLevel;
	}
	public void setrMdLevel(String rMdLevel) {
		this.rMdLevel = rMdLevel;
	}
	private String rNvlanIdTop;
	private String rAid;
	private String rMdLevel;

}
