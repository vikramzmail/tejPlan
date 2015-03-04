/*
 * EDACanvas.java
 *
 * Created on April 20, 2007, 1:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.schem;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputListener;
import javolution.util.FastList;
import org.apache.batik.swing.JSVGCanvas;

import com.tejas.eda.DeviceTransferHandler;
import com.tejas.eda.Terminal;
import com.tejas.eda.spice.Circuit;

/**
 *
 * @author Kristopher T. Beck
 */
public class Schematic extends JPanel implements MouseInputListener {

    private Circuit cktroot;
    private FastList<Symbol> symbols;
    FastList<Terminal> terminals;
    private Symbol symbol;
    Point offset;
    JSVGCanvas canvas;
    private int mode;
    int NONE = 0x0;
    int SELECT = 0x1;
    int GRAB = 0x2;
    int WIRE = 0x4;
    int NEW = 0x8;

    /** Creates a new instance of EDACanvas */
    public Schematic() {
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(canvas);
        add(scrollPane, BorderLayout.CENTER);
        //     if (installInputMapBindings) {
        InputMap imap = this.getInputMap();
        imap.put(KeyStroke.getKeyStroke("ctrl X"),
                DeviceTransferHandler.getCutAction().getValue(Action.NAME));
        imap.put(KeyStroke.getKeyStroke("ctrl C"),
                DeviceTransferHandler.getCopyAction().getValue(Action.NAME));
        imap.put(KeyStroke.getKeyStroke("ctrl V"),
                DeviceTransferHandler.getPasteAction().getValue(Action.NAME));
        // }

        ActionMap map = this.getActionMap();
        map.put(DeviceTransferHandler.getCutAction().getValue(Action.NAME),
                DeviceTransferHandler.getCutAction());
        map.put(DeviceTransferHandler.getCopyAction().getValue(Action.NAME),
                DeviceTransferHandler.getCopyAction());
        map.put(DeviceTransferHandler.getPasteAction().getValue(Action.NAME),
                DeviceTransferHandler.getPasteAction());
    }

    public int getSelectMode() {
        return mode;
    }

    public FastList<Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(FastList<Symbol> symbols) {
        this.symbols = symbols;
    }

    public Symbol getSelectedSymbol() {
        return symbol;
    }

    public void setSelectedSymbol(Symbol selectedSymbol) {
        this.symbol = selectedSymbol;
    }

    public void setSelectMode(int selectMode) {
        if (selectMode == NEW) {
            for (FastList.Node<Symbol> n = symbols.head(),
                    end = symbols.tail(); (n = n.getNext()) != end;) {
                n.getValue().removeMouseListener(this);
            }
            if (symbol != null) {
                symbol.addMouseListener(this);
            }
        }
        if (selectMode == SELECT && mode != SELECT) {
            if (mode == NEW) {
                this.removeMouseListener(this);
            }
            symbol = null;
            for (FastList.Node<Symbol> n = symbols.head(),
                    end = symbols.tail(); (n = n.getNext()) != end;) {
                n.getValue().addMouseListener(this);
            }
        }
        if (selectMode == GRAB) {
            for (FastList.Node<Symbol> n = symbols.head(),
                    end = symbols.tail(); (n = n.getNext()) != end;) {
                n.getValue().removeMouseListener(this);
            }
            symbol.addMouseMotionListener(this);
        }
        if (selectMode == WIRE) {
            if (mode == SELECT) {
                for (FastList.Node<Symbol> n = symbols.head(),
                        end = symbols.tail(); (n = n.getNext()) != end;) {
                    n.getValue().removeMouseListener(this);
                }
            }
            if (mode == NEW && symbol != null) {
                symbol.removeMouseListener(this);
            }
            for (FastList.Node<Terminal> n = terminals.head(),
                    end = terminals.tail(); (n = n.getNext()) != end;) {
//?                 n.getValue().addMouseListener(this);
            }
        }
        this.mode = selectMode;

    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1 && mode != NEW) {
            if (e.getComponent() instanceof Symbol) {
                symbol = (Symbol) e.getComponent();
                symbol.setSelected(true);
                mode = SELECT;
            }

        }
        if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1) {
            setSelectMode(SELECT);
            symbol = null;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (mode == SELECT) {
            symbol.setLocation((int) (e.getX() - offset.getX()), (int) (e.getY() - offset.getY()));
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        if (e.getSource() == this && mode == NEW) {
            symbol.setLocation(e.getPoint());
        }
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (mode != NEW) {
                if (e.getComponent() instanceof Symbol) {
                    mode = SELECT;
                    symbol = (Symbol) e.getComponent();
                    offset = new Point(symbol.getX() - e.getX(), symbol.getY() - e.getY());
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (mode == NEW && e.getSource() == this) {
            symbol.setLocation(e.getPoint());
            removeMouseMotionListener(this);
            symbols.add(symbol);
            FastList<Terminal> terms = symbol.getTerminals();
            for (FastList.Node<Terminal> n = terms.head(),
                    end = terms.tail(); (n = n.getNext()) != end;) {
                terminals.add(n.getValue());
            }
            for (FastList.Node<Symbol> n = symbols.head(),
                    end = symbols.tail(); (n = n.getNext()) != end;) {
                n.getValue().addMouseListener(this);
            }
        }
    }
}
