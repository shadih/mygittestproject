package com.att.gfp.data.ipag.topoModel;

public class FBSPtpData
{
	public String port_aid;	
	public String clli;
	public String mpa_connect_type;
	public String device_name;	
	public String device_type;	
	public String device_model;	
	public String evc_name;	

	public FBSPtpData(String port_aid, String clli, String mpa_connect_type, String device_name, String device_type, String device_model, String evc_name)
	{
		this.port_aid = port_aid;
		this.clli = clli;
		this.mpa_connect_type = mpa_connect_type;
		this.device_name = device_name;
		this.device_type = device_type;
		this.device_model = device_model;
		this.evc_name = evc_name;
	}
}
