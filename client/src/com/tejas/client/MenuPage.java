package com.tejas.client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.factories.DefaultComponentFactory;

public class MenuPage extends JFrame implements ActionListener {

    private JPanel contentPane;

    /*** Launch the application. */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MenuPage frame = new MenuPage();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /*** Create the frame. */
    public MenuPage() {
        setTitle("TJ_DWDM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 597, 530);
        setDefaultLookAndFeelDecorated(true);
        setLocationRelativeTo(null);
        setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenu mnNew = new JMenu("New");
        mnFile.add(mnNew);

        JMenuItem mntmDesignANetwork = new JMenuItem(
                "Design a Network Topology");
        mnNew.add(mntmDesignANetwork);

        JMenuItem mntmPowerlinkBudgetAnalysis = new JMenuItem(
                "Power/Link Budget Analysis");
        mnNew.add(mntmPowerlinkBudgetAnalysis);

        JMenuItem mntmLoadTrafficMatrixdemands = new JMenuItem(
                "Load Traffic Matrix/Demands");
        mnNew.add(mntmLoadTrafficMatrixdemands);

        JMenuItem mntmRwaAnalysis = new JMenuItem("RWA Analysis");
        mnNew.add(mntmRwaAnalysis);

        JMenuItem mntmNew_1 = new JMenuItem("Open");
        mnFile.add(mntmNew_1);

        JMenuItem mntmPrint = new JMenuItem("Print");
        mnFile.add(mntmPrint);

        JMenuItem mntmOpen = new JMenuItem("Save");
        mnFile.add(mntmOpen);

        JMenuItem mntmNew = new JMenuItem("Exit");
        mnFile.add(mntmNew);

        JMenu mnEdit = new JMenu("Edit");
        menuBar.add(mnEdit);

        JMenuItem mntmCopy = new JMenuItem("Copy");
        mnEdit.add(mntmCopy);

        JMenuItem mntmPaste = new JMenuItem("Paste");
        mnEdit.add(mntmPaste);

        JMenuItem mntmDelete = new JMenuItem("Delete");
        mnEdit.add(mntmDelete);

        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        JMenuItem mntmContents = new JMenuItem("Contents");
        mnHelp.add(mntmContents);

        JMenuItem mntmSearch = new JMenuItem("Search");
        mnHelp.add(mntmSearch);

        JMenuItem mntmAbout = new JMenuItem("About");
        mnHelp.add(mntmAbout);
        contentPane = new JPanel();
        contentPane.setBackground(SystemColor.menu);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[] { 21, 0, 253, 0, 0, 0, 0, 0 };
        gbl_contentPane.rowHeights = new int[] { 0, 105, 27, 23, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0 };
        gbl_contentPane.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0,
                0.0, 0.0, Double.MIN_VALUE };
        gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        contentPane.setLayout(gbl_contentPane);

        JLabel lblNewLabel = new JLabel("");
        lblNewLabel.setIcon(new ImageIcon(
                "E:\\Java UI\\tejplan\\client\\src\\tejas_mod.JPG"));
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.gridheight = 2;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 1;
        gbc_lblNewLabel.gridy = 0;
        contentPane.add(lblNewLabel, gbc_lblNewLabel);

        JLabel lblNewJgoodiesTitle = DefaultComponentFactory.getInstance()
                .createTitle("A Planning Tool for DWDM Networks");
        lblNewJgoodiesTitle.setFont(new Font("Calibri", Font.BOLD, 19));
        GridBagConstraints gbc_lblNewJgoodiesTitle = new GridBagConstraints();
        gbc_lblNewJgoodiesTitle.gridwidth = 6;
        gbc_lblNewJgoodiesTitle.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewJgoodiesTitle.gridx = 1;
        gbc_lblNewJgoodiesTitle.gridy = 2;
        contentPane.add(lblNewJgoodiesTitle, gbc_lblNewJgoodiesTitle);

        JButton btnNewButton = new JButton("Design a Network Topology");
        btnNewButton.addActionListener(this);
        btnNewButton.setActionCommand("Open");

        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 4;
        contentPane.add(btnNewButton, gbc_btnNewButton);

        JButton btnPowerlinkBudgetAnalysis = new JButton(
                "Power/Link Budget Analysis");
        GridBagConstraints gbc_btnPowerlinkBudgetAnalysis = new GridBagConstraints();
        gbc_btnPowerlinkBudgetAnalysis.insets = new Insets(0, 0, 5, 5);
        gbc_btnPowerlinkBudgetAnalysis.gridx = 1;
        gbc_btnPowerlinkBudgetAnalysis.gridy = 6;
        contentPane.add(btnPowerlinkBudgetAnalysis,
                gbc_btnPowerlinkBudgetAnalysis);

        JButton btnLoadTrafficMatrixdemands = new JButton(
                "Load Traffic Matrix/Demands");
        GridBagConstraints gbc_btnLoadTrafficMatrixdemands = new GridBagConstraints();
        gbc_btnLoadTrafficMatrixdemands.anchor = GridBagConstraints.BASELINE;
        gbc_btnLoadTrafficMatrixdemands.insets = new Insets(0, 0, 5, 5);
        gbc_btnLoadTrafficMatrixdemands.gridx = 1;
        gbc_btnLoadTrafficMatrixdemands.gridy = 8;
        contentPane.add(btnLoadTrafficMatrixdemands,
                gbc_btnLoadTrafficMatrixdemands);

        JButton btnRwaAnalysis = new JButton("RWA Analysis");
        GridBagConstraints gbc_btnRwaAnalysis = new GridBagConstraints();
        gbc_btnRwaAnalysis.insets = new Insets(0, 0, 5, 5);
        gbc_btnRwaAnalysis.gridx = 1;
        gbc_btnRwaAnalysis.gridy = 10;
        contentPane.add(btnRwaAnalysis, gbc_btnRwaAnalysis);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Open")) {
            dispose();
            new TopoFrame();
        }
    }

}
