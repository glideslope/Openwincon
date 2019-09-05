package mybeans;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.io.IOException;
import java.io.Serializable;

public class DisplayYUV extends javax.swing.JComponent implements Serializable {

    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;  
    private PropertyChangeSupport propertySupport;     
    private byte stream[];  
    private int yuv_width, yuv_height; 
    private int frame_number;   
    private int total_frames;   
    private BufferedImage bufferedImage;   

    public DisplayYUV() {
        super();
        propertySupport = new PropertyChangeSupport(this);
        stream = null;
        bufferedImage = null;
    }

    public String getSampleProperty() {
        return sampleProperty;
    }

    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public void readYUV(String file_name) throws IOException {
        setYUVStream(Util.Tools.file_to_byte(file_name));
    }

    public void readYUV(String file_name, int width, int heigth) throws IOException {
        setYUVStream(Util.Tools.file_to_byte(file_name), width, heigth);
    }
    
    public void setYUVStream(byte[] stream) {   
        setStream(stream);
        total_frames = (int) (stream.length / (yuv_width * yuv_height * 1.5));	
        if (bufferedImage == null) {
            bufferedImage = new BufferedImage(yuv_width, yuv_height, BufferedImage.TYPE_INT_RGB);
            color_array = new int[yuv_width * yuv_height];
        }
    }

    public void setYUVStream(byte[] stream, int width, int height) {
        setStream(stream);
        this.yuv_height = height;
        this.yuv_width = width;
        
        total_frames = (int) (stream.length / (yuv_width * yuv_height * 1.5));
        if (bufferedImage == null) {
            bufferedImage = new BufferedImage(yuv_width, yuv_height, BufferedImage.TYPE_INT_RGB);
            color_array = new int[yuv_width * yuv_height];
        }
    }

    private void setStream(byte[] stream) {
        this.stream = stream;
    }

    public byte[] getStream() {
        return stream;
    }

    public void setYuv_width(int yuv_width) {
        int oldYuv_width = this.yuv_width;
        this.yuv_width = yuv_width;
    }

    public int getYuv_width() {
        return yuv_width;
    }

    public void setYuv_height(int yuv_height) {
        int oldYuv_height = this.yuv_height;
        this.yuv_height = yuv_height;
    }

    public int getYuv_height() {
        return yuv_height;
    }
    
    public void setFrame_number(int frame_number) {
        int oldFrame_number = this.frame_number;
        this.frame_number = frame_number;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldFrame_number, sampleProperty);
    }

    public int getTotal_frames() {
        total_frames = (int) (stream.length / (yuv_width * yuv_height * 1.5));
        return total_frames;
    }
    
    public int getFrame_number() {
        return frame_number;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (bufferedImage != null) {
            int fbase = (int) (yuv_width * yuv_height * 1.5 * frame_number);
            for (int j = 0; j < yuv_height; j++) {
                for (int k = 0; k < yuv_width; k++) {
                    int uvoffset = (int) ((yuv_width / 2) * (j / 2) + (k / 2));
                    color_array[yuv_width * j + k] = Util.MyColor.getIntfromYUV(
                            stream[fbase + yuv_width * j + k],
                            stream[fbase + (yuv_width * yuv_height) + uvoffset],
                            stream[fbase + (int) (yuv_width * yuv_height * 1.25) + uvoffset]);
                }
            }
            bufferedImage.setRGB(0, 0, yuv_width, yuv_height, color_array, 0, yuv_width);
            g2.drawImage(bufferedImage, 0, 0, java.awt.Color.BLACK, this);
        }
    }
}
