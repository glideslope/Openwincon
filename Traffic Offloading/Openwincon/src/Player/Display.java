package Player;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import process264Real.StructurePT;

public class Display extends javax.swing.JFrame {
    int width;
    int height;
    protected int[] pixels;
    protected YuvCanvas canvas;
    protected BufferedImage image;
    protected byte[] data;
    YUVFormat yuvImage;
    protected AffineTransformOp scaleOp;

    public Display() {

        width = StructurePT.width;
        height = StructurePT.height;

        pixels = new int[width * height];
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        yuvImage = new YUVFormat(YUVFormat.Format.YUV_420, width, height);

        canvas = new YuvCanvas();

        JPanel panelCanvas = new JPanel(new BorderLayout());
        panelCanvas.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panelCanvas.add(canvas, BorderLayout.CENTER);

        add(panelCanvas, BorderLayout.CENTER);

        setScaleOp();

        pack();
        setResizable(false);
        setVisible(true);
    }

    public void setBuffer(byte[] buffer) {
        this.data = buffer;

    }

    public void draw() {
        yuvImage.setData(data);
        yuvImage.convertYUVtoRGB(pixels);
        image.getRaster().setDataElements(0, 0, width, height, pixels);
        canvas.repaint();
    }

    private void setScaleOp() {
        double scaleX = 1;
        double scaleY = 1;

        AffineTransform xform = new AffineTransform(scaleX, 0, 0, scaleY, 0, 0);
        scaleOp = new AffineTransformOp(xform, AffineTransformOp.TYPE_BICUBIC);

        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setSize(width, height);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }
    
    private class YuvCanvas extends Canvas {

        static final long serialVersionUID = 0;

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(image, scaleOp, 0, 0);
        }

        public void update(Graphics g) {
            paint(g);
        }
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Display.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Display.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Display.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Display.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Display().setVisible(true);
            }
        });
    }
}
