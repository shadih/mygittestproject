package com.att.gfp.ciena.cienaPD.topoModel;

public class MptData
{
	public String faultyend;// from mepid table
	public String port_aid;	// from mepid tble
	public String clli;	// from mepid tble
	public String clci;	// from mepid tble
	public String clfi;	// from pport tble
	public String mpaType;	// from pport tble
	public String device_type;	// from pport tble
	public String device_sub_role;	// from pport tble
	public String prodType;	// from evcNode tble
	public String evc_name;	// from evcNode tble
	public String mpa_ind;	// from device tble
	public String device_name;	// from device tble
	

	public MptData(String faultyend, String port_aid, String clli, String clci, String clfi, String mpaType, String device_type, String device_sub_role, String prodType, String mpa_ind,String device_name,String evc_name)
	{
		this.faultyend = faultyend;
		this.port_aid = port_aid;
		this.clli = clli;
		this.clci = clci;
		this.clfi = clfi;
		this.mpaType = mpaType;
		this.device_type = device_type;
		this.device_sub_role = device_sub_role;
		this.prodType = prodType;
		this.mpa_ind = mpa_ind;
		this.device_name = device_name;
		this.evc_name = evc_name;
	}
}
