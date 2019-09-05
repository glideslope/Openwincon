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
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class YuvFrame extends JFrame {

    static final long serialVersionUID = 0;

    final protected int           max_pic_width  = 640;
    final protected int           max_pic_height = 480;

    protected YuvFile             reader;
    protected int[]               pixels;
    protected BufferedImage       image;
    protected AffineTransformOp   scaleOp;

    protected YuvCanvas           canvas;
    protected JFormattedTextField ftFieldFramePos;
    protected JSlider             sliderFramePos;
    protected JSpinner            spinnerScaleX;
    protected JSpinner            spinnerScaleY;

    public YuvFrame(YuvFile file) {
        super(file.name);

        int width  = file.getWidth();
        int height = file.getHeight();

        reader = file;
        pixels = new int[width * height];
        image  = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        canvas = new YuvCanvas();
        JPanel panelCanvas = new JPanel(new BorderLayout());
        panelCanvas.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        panelCanvas.add(canvas, BorderLayout.CENTER);

        JLabel labelFramePos = new JLabel("Frame:");
        NumberFormat frameFormat = NumberFormat.getIntegerInstance();
        ftFieldFramePos = new JFormattedTextField(frameFormat);
        ftFieldFramePos.setValue(new Integer(0));
        ftFieldFramePos.setColumns((int)Math.floor(Math.log10(file.getFrameNum()))+2);
        ftFieldFramePos.setHorizontalAlignment(JTextField.RIGHT);
        ftFieldFramePos.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                JFormattedTextField source = (JFormattedTextField)e.getSource();
                sliderFramePos.setValue(((Number)source.getValue()).intValue());
            }
        });

        JPanel panelFramePos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFramePos.add(labelFramePos);
        panelFramePos.add(ftFieldFramePos);

        sliderFramePos = new JSlider(JSlider.HORIZONTAL, 0, (int)(file.frameNum-1), 0);

        sliderFramePos.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                int framePos = source.getValue();
                ftFieldFramePos.setValue(new Integer(framePos));
                if (!source.getValueIsAdjusting()) {
                    read((long)framePos);
                    canvas.repaint();
                }
            }
        });

        JPanel panelFrame = new JPanel(new BorderLayout());
        panelFrame.add(panelFramePos, BorderLayout.LINE_START);
        panelFrame.add(sliderFramePos, BorderLayout.CENTER);
        panelFrame.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JPanel panelZoom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        String[] stringsZoom = {"/8","/4","/2","*1","*2","*4","*8"};

        spinnerScaleX = new JSpinner(new SpinnerListModel(stringsZoom));
        spinnerScaleX.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setScaleOp();
                YuvFrame.this.pack();
                canvas.repaint();
            }
        });
        JSpinner.DefaultEditor spinEditX = (JSpinner.DefaultEditor)spinnerScaleX.getEditor();
        JFormattedTextField textFieldX = spinEditX.getTextField();
        textFieldX.setHorizontalAlignment(JTextField.RIGHT);
        JLabel labelX = new JLabel(" X: ");
        labelX.setLabelFor(spinnerScaleX);
        panelZoom.add(labelX);
        panelZoom.add(spinnerScaleX);

        spinnerScaleY = new JSpinner(new SpinnerListModel(stringsZoom));
        spinnerScaleY.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setScaleOp();
                YuvFrame.this.pack();
                canvas.repaint();
            }
        });
        JSpinner.DefaultEditor spinEditY = (JSpinner.DefaultEditor)spinnerScaleY.getEditor();
        JFormattedTextField textFieldY = spinEditY.getTextField();
        textFieldY.setHorizontalAlignment(JTextField.RIGHT);
        JLabel labelY = new JLabel(" Y: ");
        labelY.setLabelFor(spinnerScaleY);
        panelZoom.add(labelY);
        panelZoom.add(spinnerScaleY);
        panelZoom.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JPanel panelControl = new JPanel(new BorderLayout());
        panelControl.add(panelFrame, BorderLayout.CENTER);
        panelControl.add(panelZoom, BorderLayout.LINE_END);

        add(panelCanvas, BorderLayout.CENTER);
        add(panelControl, BorderLayout.PAGE_END);

        defaultScale();
        setScaleOp();
        read(0);

        pack();
        setResizable(false);
    }

    private void defaultScale() {
        double scaleX = quantizeScale((double)max_pic_width  / (double)reader.getWidth());
        double scaleY = quantizeScale((double)max_pic_height / (double)reader.getHeight());

        if (scaleX < scaleY) scaleY = scaleX;
        if (scaleY < scaleX) scaleX = scaleY;

        spinnerScaleX.setValue("*1");
        spinnerScaleY.setValue("*1");
    }

    private void setScaleOp() {
        double scaleX = convertStringtoScale((String)spinnerScaleX.getValue());
        double scaleY = convertStringtoScale((String)spinnerScaleY.getValue());

        AffineTransform xform = new AffineTransform(scaleX, 0, 0, scaleY, 0, 0);
        scaleOp = new AffineTransformOp(xform, AffineTransformOp.TYPE_BICUBIC);

        int width  = (int)(reader.getWidth()  * scaleX);
        int height = (int)(reader.getHeight() * scaleY);

        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setSize(width, height);
    }

    private class YuvCanvas extends Canvas {

        static final long serialVersionUID = 0;

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.drawImage(image, scaleOp, 0, 0);
        }
        public void update(Graphics g) {
            paint(g);
        }
    }

    private double quantizeScale(double scale) {
        if (scale < 0.25) return 0.125;
        if (scale < 0.5 ) return 0.25;
        if (scale < 1.0 ) return 0.5;
        if (scale < 2.0 ) return 1.0;
        if (scale < 4.0 ) return 2.0;
        if (scale < 8.0 ) return 4.0;

        return 8.0;
    }
    private double convertStringtoScale(String spinVal) {
        if (spinVal.equals("/8")) return 0.125;
        if (spinVal.equals("/4")) return 0.25;
        if (spinVal.equals("/2")) return 0.5;
        if (spinVal.equals("*1")) return 1.0;
        if (spinVal.equals("*2")) return 2.0;
        if (spinVal.equals("*4")) return 4.0;
        if (spinVal.equals("*8")) return 8.0;

        return 1.0;
    }
    private String convertScaletoString(double scale) {
        if (scale == 0.125) return "/8";
        if (scale == 0.25 ) return "/4";
        if (scale == 0.5  ) return "/2";
        if (scale == 1.0  ) return "*1";
        if (scale == 2.0  ) return "*2";
        if (scale == 4.0  ) return "*4";
        if (scale == 8.0  ) return "*8";

        return "*1";
    }

    private void read(long framePos) {
        int width  = reader.getWidth();
        int height = reader.getHeight();

        reader.read(framePos, pixels);
        image.getRaster().setDataElements(0, 0, width, height, pixels);
    }

    private void changePos(long framePos) {
        sliderFramePos.setValue((int)framePos);
        return;
    }

    public void step(long stepSize) {
        if (!reader.isFirstPos() && stepSize < 0) {
            long framePos = reader.getFramePos() + stepSize;
            if (framePos < 0)
                framePos = 0;
            changePos(framePos);
        }
        if (!reader.isLastPos() && stepSize > 0) {
            long framePos = reader.getFramePos() + stepSize;
            if (framePos > reader.getFrameNum())
                framePos = reader.getFrameNum()-1;
            changePos(framePos);
        }
    }

    public void jump(boolean last) {
        if (!last) {
            if (!reader.isFirstPos())
                changePos(0);
        } else {
            if (!reader.isLastPos())
                changePos(reader.getFrameNum()-1);
        }
    }
}