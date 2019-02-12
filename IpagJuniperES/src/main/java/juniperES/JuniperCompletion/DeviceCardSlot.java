package juniperES.JuniperCompletion;

public class DeviceCardSlot {
	private String device;
	private String card;
	private String slot;
	
	public DeviceCardSlot () {
		device = "0";
		card = "0";
		slot = "0";
		
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getCard() {
		return card;
	}

	public void setCard(String card) {
		this.card = card;
	}

	public String getSlot() {
		return slot;
	}

	public void setSlot(String slot) {
		this.slot = slot;
	}
	

}
