package juniperES.JuniperCompletion;

public class PPort {
	private String aafda_role;
	private String remoteDeviceType;
	private String name;
	private String aid;
	private String num;
	
	public PPort() {
		aafda_role = null;
		remoteDeviceType = null;
		name = null;
		num = null;
		aid = null;
		
	}
	
	public String getAafda_role() {
		return aafda_role;
	}

	public void setAafda_role(String aafda_role) {
		this.aafda_role = aafda_role;
	}

	public String getRemoteDeviceType() {
		return remoteDeviceType;
	}

	public void setRemoteDeviceType(String remoteDeviceType) {
		this.remoteDeviceType = remoteDeviceType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAid() {
		return aid;
	}

	public void setAid(String aid) {
		this.aid = aid;
	}

	public String getNum() {
		return num;
	}

	public void setNum(String num) {
		this.num = num;
	}
	
}
