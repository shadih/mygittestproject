package com.att.gfp.data.ipag.topoModel;

/**
 * Topology model object containing pport data
 * 
 * @author st133d
 *
 */
public class PPortInfo {

	private String uninniType; 
	private String clfi;
	private String remoteDeviceName;

	public String getClfi() {
		return clfi;
	}
	public void setClfi(String clfi) {
		this.clfi = clfi;
	}
	public String getUninniType() {
		return uninniType;
	}
	public void setUninniType(String uninniType) {
		this.uninniType = uninniType;
	}
	public String getRemoteDeviceName() {
		return remoteDeviceName;
	}
	public void setRemoteDeviceName(String remoteDeviceName) {
		this.remoteDeviceName = remoteDeviceName;
	}
	
	@Override
	public String toString() {
		return "PPortInfo [uninniType=" + uninniType + ", clfi=" + clfi
				+ ", remoteDeviceName=" + remoteDeviceName + "]";
	}
}
