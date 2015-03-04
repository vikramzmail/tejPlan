package com.tejas.eda.spice.device.source.optical;

import com.tejas.eda.spice.device.DWDMNetworkDevice;

public abstract class OpticalSource extends DWDMNetworkDevice implements OpticalSourceInstance {
	
	protected int posIndex;
	protected int negIndex;

	public void setInstName(String instName) {
		this.instName = instName;		
	}

	public void setPosIndex(int nodeIndex) {
		this.posIndex = nodeIndex;
	}
	
	public void setNegIndex(int nodeIndex){
		this.negIndex = nodeIndex;
	}

}
