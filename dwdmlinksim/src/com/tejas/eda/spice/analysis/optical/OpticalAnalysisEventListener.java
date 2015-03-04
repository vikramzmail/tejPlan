/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.eda.spice.analysis.optical;

import java.util.EventListener;


/**
 *
 * @author Kristopher T. Beck
 */
public interface OpticalAnalysisEventListener extends EventListener{
    public void update(OpticalAnalysisEvent evt);
    public void beginOutput(OpticalAnalysisEvent evt);
    public void endOutput(OpticalAnalysisEvent evt);
    public void restartOutput(OpticalAnalysisEvent evt);
}
