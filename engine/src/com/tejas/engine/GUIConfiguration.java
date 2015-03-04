/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.engine;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.internal.ErrorHandling;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * This class is a graphical inteface to edit Net2Plan-wide options.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class GUIConfiguration extends JDialog implements ActionListener
{
    private JButton btn_cancel, btn_save;
    
    private JTextField txt_averagePacketLengthInBytes, txt_binaryRateInBitsPerSecondPerErlang, txt_precisionFactor, txt_propagationSpeedInKmPerSecond, txt_defaultRunnableCodePath;
    private JTextField txt_cplexSolverLibraryName, txt_glpkSolverLibraryName, txt_ipoptSolverLibraryName;
    private JTextField txt_defaultILPSolver, txt_defaultNLPSolver;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.3
     */
    public GUIConfiguration()
    {
        super();

	setTitle("Options");
	setLayout(new BorderLayout());
        
        JTabbedPane pane = new JTabbedPane();
        JPanel buttonBar = new JPanel();
        
	btn_save = new JButton("Save");
        btn_save.setToolTipText("Save the current options in the .ini file");
        btn_save.addActionListener(this);
        
	btn_cancel = new JButton("Cancel");
        btn_cancel.setToolTipText("Close the dialog without saving");
        btn_cancel.addActionListener(this);
        
        buttonBar.add(btn_save);
        buttonBar.add(btn_cancel);

        add(pane, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
        
        JPanel pane_generalOptions = new JPanel(new GridLayout(0, 2));
        JPanel pane_solverOptions = new JPanel(new GridLayout(0, 2));
        
        pane.addTab("General options", pane_generalOptions);
        pane.addTab("Solver options", pane_solverOptions);
        
        txt_averagePacketLengthInBytes = new JTextField(Configuration.getOption("averagePacketLengthInBytes"));
        txt_binaryRateInBitsPerSecondPerErlang = new JTextField(Configuration.getOption("binaryRateInBitsPerSecondPerErlang"));
        txt_precisionFactor = new JTextField(Configuration.getOption("precisionFactor"));
        txt_propagationSpeedInKmPerSecond = new JTextField(Configuration.getOption("propagationSpeedInKmPerSecond"));
        txt_defaultRunnableCodePath = new JTextField(Configuration.getOption("defaultRunnableCodePath"));
        
        txt_cplexSolverLibraryName = new JTextField(Configuration.getOption("cplexSolverLibraryName"));
        txt_glpkSolverLibraryName = new JTextField(Configuration.getOption("glpkSolverLibraryName"));
        txt_ipoptSolverLibraryName = new JTextField(Configuration.getOption("ipoptSolverLibraryName"));
        
        txt_defaultILPSolver = new JTextField(Configuration.getOption("defaultILPSolver"));
        txt_defaultNLPSolver = new JTextField(Configuration.getOption("defaultNLPSolver"));
        
        JLabel lbl_averagePacketLengthInBytes = new JLabel("Average packet length (bytes)");
	JLabel lbl_binaryRateInBitsPerSecondPerErlang = new JLabel("Binary rate per Erlang (bps)");
	JLabel lbl_precisionFactor = new JLabel("Precision factor for checks");
	JLabel lbl_propagationSpeedInKmPerSecond = new JLabel("<html>Propagation speed (km/s) (zero or<br />negative value implies no propagation delay)</html>");
	JLabel lbl_defaultRunnableCodePath = new JLabel("Default path for executable code (either folder or .jar file, requires re-start Net2Plan)");

        pane_generalOptions.add(lbl_averagePacketLengthInBytes);
        pane_generalOptions.add(txt_averagePacketLengthInBytes);
        pane_generalOptions.add(lbl_binaryRateInBitsPerSecondPerErlang);
        pane_generalOptions.add(txt_binaryRateInBitsPerSecondPerErlang);
        pane_generalOptions.add(lbl_precisionFactor);
        pane_generalOptions.add(txt_precisionFactor);
        pane_generalOptions.add(lbl_propagationSpeedInKmPerSecond);
        pane_generalOptions.add(txt_propagationSpeedInKmPerSecond);
        pane_generalOptions.add(lbl_defaultRunnableCodePath);
        pane_generalOptions.add(txt_defaultRunnableCodePath);

        JLabel lbl_cplexSolverLibraryName = new JLabel("Default path for cplex library (.dll/.so file)");
        JLabel lbl_glpkSolverLibraryName = new JLabel("Default path for glpk library (.dll/.so file)");
        JLabel lbl_ipoptSolverLibraryName = new JLabel("Default path for ipopt library (.dll/.so file)");

        JLabel lbl_defaultILPSolver = new JLabel("Default solver for LP/ILP models");
        JLabel lbl_defaultNLPSolver = new JLabel("Default solver for NLP models");

        pane_solverOptions.add(lbl_defaultILPSolver);
        pane_solverOptions.add(txt_defaultILPSolver);
        pane_solverOptions.add(lbl_defaultNLPSolver);
        pane_solverOptions.add(txt_defaultNLPSolver);
        pane_solverOptions.add(lbl_cplexSolverLibraryName);
        pane_solverOptions.add(txt_cplexSolverLibraryName);
        pane_solverOptions.add(lbl_glpkSolverLibraryName);
        pane_solverOptions.add(txt_glpkSolverLibraryName);
        pane_solverOptions.add(lbl_ipoptSolverLibraryName);
        pane_solverOptions.add(txt_ipoptSolverLibraryName);
        
	pane.registerKeyboardAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

	setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
	pack();
	setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == btn_save)
        {
            Configuration.setOption("averagePacketLengthInBytes", txt_averagePacketLengthInBytes.getText());
            Configuration.setOption("binaryRateInBitsPerSecondPerErlang", txt_binaryRateInBitsPerSecondPerErlang.getText());
            Configuration.setOption("precisionFactor", txt_precisionFactor.getText());
            Configuration.setOption("propagationSpeedInKmPerSecond", txt_propagationSpeedInKmPerSecond.getText());
            Configuration.setOption("defaultRunnableCodePath", txt_defaultRunnableCodePath.getText());
            
            Configuration.setOption("cplexSolverLibraryName", txt_cplexSolverLibraryName.getText());
            Configuration.setOption("glpkSolverLibraryName", txt_glpkSolverLibraryName.getText());
            Configuration.setOption("ipoptSolverLibraryName", txt_ipoptSolverLibraryName.getText());

            Configuration.setOption("defaultILPSolver", txt_defaultILPSolver.getText());
            Configuration.setOption("defaultNLPSolver", txt_defaultNLPSolver.getText());

            try
            {
                Configuration.saveOptions();
            }
            catch(Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error saving options");
                return;
            }
        }

        dispose();
    }
}
