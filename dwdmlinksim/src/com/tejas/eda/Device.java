/*
 * Device.java
 *
 * Created on May 9, 2007, 6:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.eda;

import com.tejas.eda.schem.Symbol;
import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.device.Instance;

import javolution.util.FastList;

/**
 *
 * @author Kristopher T. Beck
 */
public class Device {
    protected String modelName;
    protected String instName = "unnamed";
    protected String description = "not described";
    protected Circuit ckt;
    Instance model;
    Symbol symbol;
    FastList<Terminal> terminals;

    /** Creates a new instance of Device */
    public Device() {
    }
    
    public Instance getModel() {
        return model;
    }

    public void setModel(Instance model) {
        this.model = model;
    }

    public String getInstName() {
        return instName;
    }

    public void setInstName(String instName) {
        this.instName = instName;
    }

    public String getModelName(){
        return modelName;
    }
    
    public void setModelName(String modelName){
        this.modelName = modelName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public Circuit getCircuit(){
        return ckt;
    }
    
    public void setCircuit(Circuit ckt) {
        this.ckt = ckt;
    }

}
