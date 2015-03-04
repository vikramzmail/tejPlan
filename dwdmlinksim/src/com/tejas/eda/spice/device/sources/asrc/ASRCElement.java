package com.tejas.eda.spice.device.sources.asrc;


public interface ASRCElement {

    public boolean init();

    public boolean acload(double deriv);

    public boolean load(double deriv);
}
