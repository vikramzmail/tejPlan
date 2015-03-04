package com.tejas.eda.spice.device.sources;

import com.tejas.eda.spice.Circuit;
import com.tejas.math.numbers.Complex;

public class CurrentElement extends SourceElement {

    protected String contName;
    protected int branchIndex;

    public CurrentElement() {
    }

    public String getContName() {
        return contName;
    }

    public void setContName(String contName) {
        this.contName = contName;
    }

    @Override
    public String getName() {
        if(name == null){
            name = "I("+ contName + "#branch)";
        }
        return name;
    }

    @Override
    public boolean init(Circuit ckt) {
        branchIndex = ckt.findBranch(contName);
        return super.init(ckt);
    }

    @Override
    public Complex getValue() {
        if (branchIndex != 0) {
            return wrk.getRhsOldAt(branchIndex);
        }
        return Complex.zero();
    }

    @Override
    public String toString() {
        return getName();
    }
}
