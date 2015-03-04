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

import com.tejas.engine.GUIConfiguration;
import com.tejas.engine.GUINet2Plan.PluginType;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.Version;
import com.tejas.engine.tools.GUIConnectionSimulation;
import com.tejas.engine.tools.GUINetworkDesign;
import com.tejas.engine.tools.GUIResilienceSimulation;
import com.tejas.engine.tools.GUITimeVaryingTrafficSimulation;
import com.tejas.engine.tools.GUITrafficDesign;
import com.tejas.engine.utils.ClassPathEditor;
import com.tejas.engine.utils.TransparentPanel;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Main class for the graphical user interface (GUI).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUINet2Plan extends JFrame implements ActionListener
{
    private JPanel container;
    private JMenuItem exitItem, optionsItem, errorConsoleItem, classPathEditorItem;
    private JMenuItem item_networkDesign, item_trafficDesign, item_resilienceSimulator, item_connectionSimulator, item_trafficChangeSimulator;
    private JMenuItem aboutItem, helpItem, javadocItem, javadocExamplesItem;

    public static enum PluginType { ABOUT, NETWORK_DESIGNER, TRAFFIC_DESIGNER, FAILURE_SIMULATOR, CONNECTION_SIMULATOR, TRAFFIC_CHANGE_SIMULATOR };

    private static String ABOUT_TEXT = "<html><p align='justify'>Welcome to Net2Plan©: an educational software for network planning courses.</p><br>"
	    + "<p align='justify'>Net2Plan is a valuable resource to support the teaching/learning process of network optimization and planning theory. The tool provides a framework for designing, evaluating and comparing network planning algorithms. A set of built-in planning algorithms and libraries are included in the tool. Also, the tool is designed to permit the students the integration and evaluation of their own made algorithms.</p><br>"
	    + "<p align='justify'>For more information, please visit Net2Plan website: http://www.net2plan.com</p></html>";

    @Override
    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource().equals(optionsItem))
	{
            JDialog dialog = new GUIConfiguration();
            dialog.setVisible(true);
	    return;
	}
        if (e.getSource().equals(errorConsoleItem))
        {
            ErrorHandling.showConsole();
            return;
        }
        if (e.getSource().equals(classPathEditorItem))
        {
	    ClassPathEditor.showGUI();
            return;
        }
	if (e.getSource().equals(exitItem))
	{
	    askForClose();
	    return;
	}
	if (e.getSource().equals(helpItem))
	{
	    loadHelp();
	    return;
	}
	if (e.getSource().equals(javadocItem))
	{
	    loadJavadocLib();
	    return;
	}
	if (e.getSource().equals(javadocExamplesItem))
	{
	    loadExamples();
	    return;
	}

	try
	{
	    if (e.getSource().equals(item_networkDesign))
	    {
		loadPanel(PluginType.NETWORK_DESIGNER);
		return;
	    }
	    if (e.getSource().equals(item_trafficDesign))
	    {
		loadPanel(PluginType.TRAFFIC_DESIGNER);
		return;
	    }
	    if (e.getSource().equals(aboutItem)) { loadPanel(PluginType.ABOUT); }
	    if (e.getSource().equals(item_resilienceSimulator)) { loadPanel(PluginType.FAILURE_SIMULATOR); }
	    if (e.getSource().equals(item_connectionSimulator)) { loadPanel(PluginType.CONNECTION_SIMULATOR); }
	    if (e.getSource().equals(item_trafficChangeSimulator)) { loadPanel(PluginType.TRAFFIC_CHANGE_SIMULATOR); }

	}
	catch (Throwable ex)
	{
	    ErrorHandling.addErrorOrException(ex, GUINet2Plan.class);
	    ErrorHandling.showErrorDialog("Unable to execute option");
	}
    }

    private void loadHelp()
    {
	File helpFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "help" + SystemUtils.getDirectorySeparator() + "index.html");
	try
	{
	    Desktop.getDesktop().browse(helpFile.toURI());
	}
	catch (Throwable ex)
	{
	    ErrorHandling.showErrorDialog(ex.getMessage(), "Error showing help file");
	}
    }

    private void loadJavadocLib()
    {
	File helpFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "javadoc" + SystemUtils.getDirectorySeparator() + "index.html");
	try
	{
	    Desktop.getDesktop().browse(helpFile.toURI());
	}
	catch (Throwable ex)
	{
	    ErrorHandling.showErrorDialog(ex.getMessage(), "Error showing Library API Javadoc");
	}
    }

    private void loadExamples()
    {
	try
	{
	    Desktop.getDesktop().browse(new URL("http://www.net2plan.com/examples/viewKeywords.php").toURI());
	}
	catch (URISyntaxException | IOException ex)
	{
	    ErrorHandling.addErrorOrException(ex, GUINet2Plan.class);
	    ErrorHandling.showErrorDialog("Error showing Examples page");
	}
    }

    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public GUINet2Plan()
    {
	try
	{
	    setExtendedState(JFrame.MAXIMIZED_BOTH);
	    setMinimumSize(new Dimension(800, 600));
            
            URL iconURL = getClass().getResource("/resources/icon.png");
            ImageIcon icon = new ImageIcon(iconURL);
            setIconImage(icon.getImage());            
            
	    setTitle(new Version().toString());
	    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    addWindowListener(new WindowAdapter()
	    {

		@Override
		public void windowClosing(WindowEvent e)
		{
		    askForClose();
		}
	    });

	    getContentPane().setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));
	    container = new JPanel();
	    container.setBorder(new LineBorder(Color.BLACK));
	    container.setLayout(new MigLayout("fill"));
	    getContentPane().add(container, "grow");

	    // Create menu bar
	    JMenuBar menu = new JMenuBar();
	    setJMenuBar(menu);

	    // File menu
	    JMenu file = new JMenu("File");
	    file.setMnemonic('F');
	    menu.add(file);

	    optionsItem = new JMenuItem("Options");
	    optionsItem.addActionListener(this);
	    file.add(optionsItem);

	    classPathEditorItem = new JMenuItem("Classpath editor");
	    classPathEditorItem.addActionListener(this);
	    file.add(classPathEditorItem);

	    errorConsoleItem = new JMenuItem("Show Java console");
	    errorConsoleItem.addActionListener(this);
	    errorConsoleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK));
	    file.add(errorConsoleItem);

	    exitItem = new JMenuItem("Exit");
	    exitItem.addActionListener(this);
	    exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
	    file.add(exitItem);

	    // Tools menu
	    JMenu tools = new JMenu("Tools");
	    tools.setMnemonic('T');
	    menu.add(tools);

	    item_networkDesign = new JMenuItem("Offline network design");
	    item_networkDesign.addActionListener(this);
	    item_networkDesign.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK));
	    tools.add(item_networkDesign);

	    item_trafficDesign = new JMenuItem("Traffic matrix design");
	    item_trafficDesign.addActionListener(this);
	    item_trafficDesign.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK));
	    tools.add(item_trafficDesign);

	    item_resilienceSimulator = new JMenuItem("Resilience simulation");
	    item_resilienceSimulator.addActionListener(this);
	    item_resilienceSimulator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_DOWN_MASK));
	    tools.add(item_resilienceSimulator);

	    item_connectionSimulator = new JMenuItem("Connection-admission-control simulation");
	    item_connectionSimulator.addActionListener(this);
	    item_connectionSimulator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.ALT_DOWN_MASK));
	    tools.add(item_connectionSimulator);

	    item_trafficChangeSimulator = new JMenuItem("Time-varying traffic simulation");
	    item_trafficChangeSimulator.addActionListener(this);
	    item_trafficChangeSimulator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.ALT_DOWN_MASK));
	    tools.add(item_trafficChangeSimulator);


	    // Help menu
	    JMenu help = new JMenu("Help");
	    help.setMnemonic('H');
	    menu.add(help);

	    aboutItem = new JMenuItem("About");
	    aboutItem.addActionListener(this);
	    help.add(aboutItem);

	    helpItem = new JMenuItem("User's guide");
	    helpItem.addActionListener(this);
	    helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
	    help.add(helpItem);

	    javadocItem = new JMenuItem("Library API Javadoc");
	    javadocItem.addActionListener(this);
	    help.add(javadocItem);

	    javadocExamplesItem = new JMenuItem("Examples in website");
	    javadocExamplesItem.addActionListener(this);
	    help.add(javadocExamplesItem);

	    container.add(showAbout());
	    container.revalidate();

	    // Load options
	    try
	    {
		Configuration.readFromOptionsDefaultFile();

		Map<String, String> options = Configuration.getOptions();

		if (options.containsKey("classpath"))
		{
		    String classpath = options.get("classpath");
		    StringTokenizer tokens = new StringTokenizer(classpath, ";");
		    while(tokens.hasMoreTokens())
		    {
			String token = tokens.nextToken();
			if (!token.isEmpty())
			    ClassPathEditor.addToClasspath(new File(token));
		    }
		}

	    }
	    catch (IOException ex)
	    {
		ErrorHandling.showWarningDialog(ex.getMessage(), "Error loading options");
	    }
	    catch (Exception ex)
	    {
		ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading options");
	    }
	}
	catch(Throwable ex)
	{
	    throw new RuntimeException(ex);
	}
    }

    private void loadPanel(PluginType plugin)
    {
	JPanel pane = null;
	switch (plugin)
	{
	    case ABOUT:
		pane = showAbout();
		break;

	    case NETWORK_DESIGNER:
		pane = new GUINetworkDesign();
		break;

	    case TRAFFIC_DESIGNER:
		pane = new GUITrafficDesign();
		break;

	    case FAILURE_SIMULATOR:
		pane = new GUIResilienceSimulation();
		break;

	    case CONNECTION_SIMULATOR:
		pane = new GUIConnectionSimulation();
		break;

	    case TRAFFIC_CHANGE_SIMULATOR:
		pane = new GUITimeVaryingTrafficSimulation();
		break;

	    default:
		throw new RuntimeException("Bad");
        }

	container.removeAll();
	container.add(pane, "grow");
	container.revalidate();
	container.updateUI();
    }

    private JPanel showAbout()
    {
	TransparentPanel aboutPanel = new TransparentPanel();
        
        ImageIcon image = new ImageIcon(aboutPanel.createImage("/resources/logo.png").getImage());
        JLabel label = new JLabel("", image, JLabel.CENTER);

	aboutPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow][grow]"));
	aboutPanel.add(label, "align center, wrap");
	aboutPanel.add(new JLabel(ABOUT_TEXT), "align center");

//        aboutPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow][grow]", "[grow][][grow]"));
//	aboutPanel.add(label, "cell 0 1");
//	aboutPanel.add(new JLabel(ABOUT_TEXT), "cell 1 1");

	return aboutPanel;
    }

    private static void askForClose()
    {
	int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit Net2Plan?", "Exit from Net2Plan", JOptionPane.YES_NO_OPTION);

	if (result == JOptionPane.YES_OPTION) System.exit(0);
    }

    /**
     * <p>Main method</p>
     *
     * @param args Command-line parameters (unused)
     * @since 0.2.0
     */
    public static void main(String[] args)
    {
        SystemUtils.configureEnvironment(GUINet2Plan.class, SystemUtils.UserInterface.GUI);
        
	PrintStream stdout = System.out;
	PrintStream stderr = System.err;

	try
	{
	    PrintStream out = new PrintStream(ErrorHandling.stream, true, "UTF-8");
	    System.setOut(out);
	    System.setErr(out);
            
//            ErrorHandling.setUserInterface(ErrorHandling.USER_INTERFACE.GUI_MODE);

	    JFrame net2Plan = new GUINet2Plan();
            net2Plan.setVisible(true);
	}
	catch(Throwable ex)
	{
	    System.setOut(stdout);
	    System.setErr(stderr);

//            ErrorHandling.setUserInterface(ErrorHandling.USER_INTERFACE.CLI_MODE);
	    System.out.println("Error loading the graphic environment. Please, try the command line interface.");
	    ex.printStackTrace();
	}
    }
}