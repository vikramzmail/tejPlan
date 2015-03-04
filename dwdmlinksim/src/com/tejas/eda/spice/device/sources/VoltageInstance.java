/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources;

/**
 *
 * @author owenbad
 */
public interface VoltageInstance extends SourceInstance {

    public int findBranch(String name);
}
