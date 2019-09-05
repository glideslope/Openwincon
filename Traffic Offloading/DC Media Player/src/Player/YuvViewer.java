/*
 * Java YUV Image Player
 * Copyright (C) 2010 Luuvish <luuvish@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package Player;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import process264Real.StructurePT;

public class YuvViewer {

    public static void main(String[] args) {
        System.err.println("Java YUV Image Player");
        System.err.println("Copyright (C) 2010 Luuvish <luuvish@gmail.com>");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        YuvViewer viewer = new YuvViewer();
        viewer.run();
    }

    public void test() {
        comboImgFormat.setSelectedIndex(1);
        sliderImgWidth.setValue(80);
        sliderImgHeight.setValue(96);
        openFile(new File("yuv/Allegro_BDWIDTH_CAVLC_B00_L30_5x6_6.1.26l.yuv"));
        openFile(new File("yuv/Allegro_BDWIDTH_CAVLC_B00_L30_5x6_6.1.26l.yuv"));

        play(1);
    }
    WindowAdapter winAdapter;
    JFrame frameMain;
    JPanel panelMain;
    JComboBox comboImgFormat;
    JLabel labelImgName;
    JFormattedTextField ftFieldImgWidth;
    JFormattedTextField ftFieldImgHeight;
    JSlider sliderImgWidth;
    JSlider sliderImgHeight;
    JButton buttonFastBackward;
    JButton buttonBackward;
    JButton buttonPlayStop;
    JButton buttonFastForward;
    JButton buttonFirst;
    JButton buttonStepBackward10;
    JButton buttonStepBackward;
    JButton buttonStepForward;
    JButton buttonStepForward10;
    JButton buttonLast;
    ArrayList<YuvFrame> listYuvFrame = new ArrayList<YuvFrame>();
    Timer timer = null;
    boolean isPlaying = false;
    long stepSize = 1;
    long delay = Math.round(1000.0 / StructurePT.frameperGOP);    
    YuvFrame view = null;
    public YuvFile reader = null;

    public void run() {
        initGUI();
    }

    public void init() {
        YuvImage.Format format = YuvImage.Format.YUV_420;

        File file = new File(StructurePT.file);

        reader = new YuvFile(file, new YuvImage(format, 704, 576));
        view = new YuvFrame(reader);

        view.pack();
        view.setVisible(true);
        listYuvFrame.add(view);

        play(1);

    }

    public void setMax() {
        view.sliderFramePos.setMaximum(view.sliderFramePos.getMaximum() + StructurePT.frameperGOP);
    }

    private void initGUI() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        winAdapter = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (e.getSource() instanceof YuvFrame) {
                    YuvFrame view = (YuvFrame) e.getSource();
                    listYuvFrame.remove(view);
                    stateViewMenu();
                }
            }
        };
        frameMain = new JFrame("Yuv Viewer");
        frameMain.addWindowListener(winAdapter);
        frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panelMain = new JPanel();
        panelMain.setOpaque(true);
        panelMain.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.Y_AXIS));

        panelMain.add(initFilePanel());
        panelMain.add(Box.createRigidArea(new Dimension(0, 4)));
        panelMain.add(initPlayPanel());

        new DropTarget(panelMain, new FileDropListener());

        frameMain.setJMenuBar(initMenuBar());
        frameMain.getContentPane().add(panelMain);

        statePlayPanel();
        statePlayMenu();
        stateViewMenu();

        frameMain.setBounds(50, 50, 300, 300);
        frameMain.pack();
        frameMain.setResizable(false);
        frameMain.setVisible(true);
    }

    private JPanel initFilePanel() {
        JPanel panelName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fileOpen = new JButton("Open");
        fileOpen.addActionListener(new FileOpenListener());
        panelName.add(fileOpen);
        String[] comboString = {"4:0:0", "4:2:0", "4:2:2", "4:2:2v", "4:4:4"};
        comboImgFormat = new JComboBox(comboString);
        comboImgFormat.setSelectedIndex(1);
        comboImgFormat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int menuIndex = ((JComboBox) e.getSource()).getSelectedIndex() + 2;
                frameMain.getJMenuBar().getMenu(0).getItem(menuIndex).setSelected(true);
            }
        });
        panelName.add(comboImgFormat);

        JPanel panelFile = new JPanel(new BorderLayout());
        labelImgName = new JLabel("- no file -", JLabel.CENTER);
        labelImgName.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        panelFile.add(panelName, BorderLayout.LINE_START);
        panelFile.add(labelImgName, BorderLayout.CENTER);

        JPanel panelWidth = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel labelWidth = new JLabel(" Width:");
        panelWidth.add(labelWidth);
        NumberFormat imgWidthFormat = NumberFormat.getIntegerInstance();
        ftFieldImgWidth = new JFormattedTextField(imgWidthFormat);
        ftFieldImgWidth.setValue(new Integer(352));    
        ftFieldImgWidth.setColumns(4);
        ftFieldImgWidth.setHorizontalAlignment(JTextField.RIGHT);
        ftFieldImgWidth.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                JFormattedTextField source = (JFormattedTextField) e.getSource();
                sliderImgWidth.setValue(((Number) source.getValue()).intValue());
            }
        });
        panelWidth.add(ftFieldImgWidth);
        sliderImgWidth = new JSlider(JSlider.HORIZONTAL, 1, 2560, 352);
        sliderImgWidth.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                ftFieldImgWidth.setValue(new Integer(((JSlider) e.getSource()).getValue()));
            }
        });
        sliderImgWidth.setMajorTickSpacing(352);
        sliderImgWidth.setMinorTickSpacing(16);
        sliderImgWidth.setFocusable(false);
        panelWidth.add(sliderImgWidth);

        JPanel panelHeight = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel labelHeight = new JLabel(" Height:");
        panelHeight.add(labelHeight);
        NumberFormat imgHeightFormat = NumberFormat.getIntegerInstance();
        ftFieldImgHeight = new JFormattedTextField(imgHeightFormat);
        ftFieldImgHeight.setValue(new Integer(288));       
        ftFieldImgHeight.setColumns(4);
        ftFieldImgHeight.setHorizontalAlignment(JTextField.RIGHT);
        ftFieldImgHeight.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                JFormattedTextField source = (JFormattedTextField) e.getSource();
                sliderImgHeight.setValue(((Number) source.getValue()).intValue());
            }
        });
        panelHeight.add(ftFieldImgHeight);
        sliderImgHeight = new JSlider(JSlider.HORIZONTAL, 1, 1600, 288);
        sliderImgHeight.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                ftFieldImgHeight.setValue(new Integer(((JSlider) e.getSource()).getValue()));
            }
        });
        sliderImgHeight.setMajorTickSpacing(288);
        sliderImgHeight.setMinorTickSpacing(16);
        sliderImgHeight.setFocusable(false);
        panelHeight.add(sliderImgHeight);

        JPanel panelSize = new JPanel(new GridLayout(1, 2));
        panelSize.add(panelWidth);
        panelSize.add(panelHeight);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        panel.add(panelFile);
        panel.add(panelSize);

        return panel;
    }

    private JPanel initPlayPanel() {
        Box boxPlay = new Box(BoxLayout.X_AXIS);
        buttonFastBackward = new JButton("Fast Backward");
        buttonFastBackward.addActionListener(new FastBackwardListener());
        boxPlay.add(buttonFastBackward);
        buttonBackward = new JButton("Backward");
        buttonBackward.addActionListener(new BackwardListener());
        boxPlay.add(buttonBackward);
        buttonPlayStop = new JButton("Play");
        buttonPlayStop.addActionListener(new PlayStopListener());
        boxPlay.add(buttonPlayStop);
        buttonFastForward = new JButton("Fast Forward");
        buttonFastForward.addActionListener(new FastForwardListener());
        boxPlay.add(buttonFastForward);

        Box boxStep = new Box(BoxLayout.X_AXIS);
        buttonFirst = new JButton("First");
        buttonFirst.addActionListener(new FirstListener());
        boxStep.add(buttonFirst);
        buttonStepBackward10 = new JButton("Step -10");
        buttonStepBackward10.addActionListener(new StepBackward10Listener());
        boxStep.add(buttonStepBackward10);
        buttonStepBackward = new JButton("Step -1");
        buttonStepBackward.addActionListener(new StepBackwardListener());
        boxStep.add(buttonStepBackward);
        buttonStepForward = new JButton("Step +1");
        buttonStepForward.addActionListener(new StepForwardListener());
        boxStep.add(buttonStepForward);
        buttonStepForward10 = new JButton("Step +10");
        buttonStepForward10.addActionListener(new StepForward10Listener());
        boxStep.add(buttonStepForward10);
        buttonLast = new JButton("Last");
        buttonLast.addActionListener(new LastListener());
        boxStep.add(buttonLast);

        Box boxPlayStep = new Box(BoxLayout.Y_AXIS);
        boxPlayStep.add(boxPlay);
        boxPlayStep.add(boxStep);

        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        panel.add(boxPlayStep);

        return panel;
    }

    private JMenuBar initMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        ButtonGroup group;

        menuBar = new JMenuBar();

        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.getAccessibleContext().setAccessibleDescription("File menu");
        menuBar.add(menu);

        menuItem = new JMenuItem("Open", KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("Image file open");
        menuItem.addActionListener(new FileOpenListener());
        menu.add(menuItem);

        menu.addSeparator();

        group = new ButtonGroup();

        rbMenuItem = new JRadioButtonMenuItem("YUV 4:0:0");
        rbMenuItem.setMnemonic(KeyEvent.VK_1);
        rbMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        rbMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboImgFormat.setSelectedIndex(0);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("YUV 4:2:0");
        rbMenuItem.setSelected(true);
        rbMenuItem.setMnemonic(KeyEvent.VK_2);
        rbMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        rbMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboImgFormat.setSelectedIndex(1);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("YUV 4:2:2");
        rbMenuItem.setMnemonic(KeyEvent.VK_3);
        rbMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
        rbMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboImgFormat.setSelectedIndex(2);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("YUV 4:2:2v");
        rbMenuItem.setMnemonic(KeyEvent.VK_4);
        rbMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
        rbMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboImgFormat.setSelectedIndex(3);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("YUV 4:4:4");
        rbMenuItem.setMnemonic(KeyEvent.VK_5);
        rbMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, ActionEvent.ALT_MASK));
        rbMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboImgFormat.setSelectedIndex(4);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("Close application");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        menu.add(menuItem);

        menu = new JMenu("Play");
        menu.setMnemonic(KeyEvent.VK_P);
        menu.getAccessibleContext().setAccessibleDescription("Play menu");
        menuBar.add(menu);

        menuItem = new JMenuItem("Play");
        menuItem.addActionListener(new PlayStopListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Backward");
        menuItem.addActionListener(new BackwardListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Fast Forward");
        menuItem.addActionListener(new FastForwardListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Fast Backward");
        menuItem.addActionListener(new FastBackwardListener());
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("First");
        menuItem.addActionListener(new FirstListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Step -10");
        menuItem.addActionListener(new StepBackward10Listener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Step -1");
        menuItem.addActionListener(new StepBackwardListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Step +1");
        menuItem.addActionListener(new StepForwardListener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Step +10");
        menuItem.addActionListener(new StepForward10Listener());
        menu.add(menuItem);

        menuItem = new JMenuItem("Last");
        menuItem.addActionListener(new LastListener());
        menu.add(menuItem);

        menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);
        menu.getAccessibleContext().setAccessibleDescription("View menu");
        menuBar.add(menu);

        menu = new JMenu("Look & Feel");
        menu.setMnemonic(KeyEvent.VK_L);
        menu.getAccessibleContext().setAccessibleDescription("Look and Feel menu");
        menuBar.add(menu);

        group = new ButtonGroup();
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            rbMenuItem = new JRadioButtonMenuItem(info.getName() + " Look & Feel");
            rbMenuItem.setSelected(info.getName().equals(UIManager.getLookAndFeel().getName()));
            rbMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem) e.getSource();
                    JMenu menuView = frameMain.getJMenuBar().getMenu(3);
                    int count = menuView.getItemCount();

                    for (int i = 0; i < count; i++) {
                        if (item == menuView.getItem(i)) {
                            String name = UIManager.getInstalledLookAndFeels()[i].getClassName();
                            try {
                                UIManager.setLookAndFeel(name);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            break;
                        }
                    }

                    for (YuvFrame view : listYuvFrame) {
                        SwingUtilities.updateComponentTreeUI(view);
                    }
                    SwingUtilities.updateComponentTreeUI(frameMain);
                }
            });
            group.add(rbMenuItem);
            menu.add(rbMenuItem);
        }

        return menuBar;
    }

    private void statePlayPanel() {
        if (isPlaying) {
            buttonPlayStop.setText("Stop");

            buttonFirst.setEnabled(false);
            buttonStepBackward10.setEnabled(false);
            buttonStepBackward.setEnabled(false);
            buttonStepForward.setEnabled(false);
            buttonStepForward10.setEnabled(false);
            buttonLast.setEnabled(false);
        } else {
            buttonPlayStop.setText("Play");

            buttonFirst.setEnabled(true);
            buttonStepBackward10.setEnabled(true);
            buttonStepBackward.setEnabled(true);
            buttonStepForward.setEnabled(true);
            buttonStepForward10.setEnabled(true);
            buttonLast.setEnabled(true);
        }
    }

    private void statePlayMenu() {
        JMenu menuPlay = frameMain.getJMenuBar().getMenu(1);
        if (isPlaying) {
            menuPlay.getItem(0).setText("Stop");
            for (int i = 5; i < 11; i++) {
                menuPlay.getItem(i).setEnabled(false);
            }
        } else {
            menuPlay.getItem(0).setText("Play");
            for (int i = 5; i < 11; i++) {
                menuPlay.getItem(i).setEnabled(true);
            }
        }
    }

    private void stateViewMenu() {
        JMenu menuView = frameMain.getJMenuBar().getMenu(2);
        menuView.removeAll();

        JMenuItem menuItem;
        for (YuvFrame view : listYuvFrame) {
            menuItem = new JMenuItem(view.reader.name);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem) e.getSource();
                    JMenu menuView = frameMain.getJMenuBar().getMenu(2);
                    int count = menuView.getItemCount();

                    for (int i = 0; i < count; i++) {
                        if (item == menuView.getItem(i)) {
                            listYuvFrame.get(i).toFront();
                            break;
                        }
                    }
                }
            });
            menuView.add(menuItem);
        }

        if (listYuvFrame.size() > 0) {
            menuView.addSeparator();
        }

        menuItem = new JMenuItem("Close All");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (YuvFrame viewer : listYuvFrame) {
                    viewer.setVisible(false);
                    viewer.dispose();
                }
                listYuvFrame.clear();
                stateViewMenu();
            }
        });
        menuView.add(menuItem);
    }

    private class FileOpenListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter("YUV Image", "yuv"));
            if (chooser.showOpenDialog(frameMain) == JFileChooser.APPROVE_OPTION) {
                File[] list = chooser.getSelectedFiles();
                for (File file : list) {
                    openFile(file);
                }
            }
        }
    }

    private class FileDropListener implements DropTargetListener {

        public void dragEnter(DropTargetDragEvent de) {
        }

        public void dragExit(DropTargetEvent de) {
        }

        public void dragOver(DropTargetDragEvent de) {
        }

        public void dropActionChanged(DropTargetDragEvent de) {
        }

        public void drop(DropTargetDropEvent de) {
            de.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            Transferable tr = de.getTransferable();
            try {
                if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    java.util.List<File> list = (java.util.List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : list) {
                        openFile(file);
                    }
                    de.dropComplete(true);
                } else {
                    de.rejectDrop();
                }
            } catch (Exception e) {
                e.printStackTrace();
                de.rejectDrop();
            }
        }
    }

    private class FastBackwardListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.play(-10);
        }
    }

    private class BackwardListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.play(-1);
        }
    }

    private class PlayStopListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.play(1);
        }
    }

    private class FastForwardListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.play(10);
        }
    }

    private class FirstListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.jump(false);
        }
    }

    private class StepBackward10Listener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.step(-10);
        }
    }

    private class StepBackwardListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.step(-1);
        }
    }

    private class StepForwardListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.step(1);
        }
    }

    private class StepForward10Listener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.step(10);
        }
    }

    private class LastListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            YuvViewer.this.jump(true);
        }
    }

    private void openFile(File file) {
        try {
            ftFieldImgWidth.commitEdit();
            ftFieldImgHeight.commitEdit();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        labelImgName.setText(file.getName());   

        YuvImage.Format format = YuvImage.Format.YUV_400;
        switch (comboImgFormat.getSelectedIndex()) {
            case 0:
                format = YuvImage.Format.YUV_400;
                break;
            case 1:
                format = YuvImage.Format.YUV_420;
                break;
            case 2:
                format = YuvImage.Format.YUV_422;
                break;
            case 3:
                format = YuvImage.Format.YUV_224;
                break;
            case 4:
                format = YuvImage.Format.YUV_444;
                break;
        }

        int width = sliderImgWidth.getValue(); 
        int height = sliderImgHeight.getValue();   

        YuvFile reader = new YuvFile(file, new YuvImage(format, width, height));
        YuvFrame view = new YuvFrame(reader);

        view.addWindowListener(winAdapter);
        if (listYuvFrame.size() > 0) {
            YuvFrame last = listYuvFrame.get(listYuvFrame.size() - 1);
            Rectangle bound = last.getBounds();
            view.setBounds(bound.x + bound.width + 4, bound.y, 4, 4);
        } else {
            Rectangle bound = frameMain.getBounds();
            view.setBounds(bound.x, bound.y + bound.height + 4, 4, 4);
        }
        view.pack();
        view.setVisible(true);
        listYuvFrame.add(view);
        stateViewMenu();
    }

    private void play(long stepSize) {
        this.stepSize = stepSize;
        if (isPlaying && stepSize == 1) {
            isPlaying = false;
            statePlayPanel();
            statePlayMenu();
            stopTimer();
        } else {
            isPlaying = true;
            startTimer();
        }
    }

    private void step(long stepSize) {
        if (!isPlaying) {
            for (YuvFrame view : listYuvFrame) {
                view.step(stepSize);
            }
        }
    }

    private void jump(boolean last) {
        if (!isPlaying) {
            for (YuvFrame view : listYuvFrame) {
                view.jump(last);
            }
        }
    }

    private class PlayTimerTask extends TimerTask {

        public void run() {
            tick();
        }
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer("YUV image viewer", true);
            timer.schedule(new PlayTimerTask(), 0, delay);
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void tick() {
        for (YuvFrame view : listYuvFrame) {
            view.step(stepSize);
        }

        boolean isRunning = false;

        for (YuvFrame view : listYuvFrame) {
            if (stepSize < 0 && !view.reader.isFirstPos()) {
                isRunning = true;
                break;
            }
            if (stepSize > 0 && !view.reader.isLastPos()) {
                isRunning = true;
                break;
            }
        }

        if (!isRunning) {
        }
    }
}