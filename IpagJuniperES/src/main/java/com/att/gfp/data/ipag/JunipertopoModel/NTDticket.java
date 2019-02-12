package com.att.gfp.data.ipag.JunipertopoModel;

public class NTDticket {

	private String number;
	private String CLFI;
	private String hostName;
	
	public NTDticket() {
		number = null;
		CLFI = null;
		hostName = null;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getCLFI() {
		return CLFI;
	}

	public void setCLFI(String cLFI) {
		CLFI = cLFI;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
