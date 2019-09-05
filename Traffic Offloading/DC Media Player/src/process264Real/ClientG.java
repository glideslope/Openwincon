package process264Real;

import Player.Display;
import java.net.SocketException;
import java.net.UnknownHostException;
import FEC.common.TypeFEC;
import NetworkCost.CostModel;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import javax.swing.SwingUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ClientG extends javax.swing.JFrame {
    class StructurePath implements Serializable {

        java.net.NetworkInterface nface;
        java.net.DatagramSocket socket;
        CostModel costModel;
        int interfaceType;

        String address;

        int delay;

        int bandwidth;

        int bandwidth_step;
        int bandwidth_max;
        int bandwidth_min;
    }

    TypeFEC fType;
    Thread t_DisplayInner;
    Thread t_WaitInner;
    Thread t_BlinkTable;
    Thread t_ProcessPackets;

    StructurePath[] paths;
    java.util.concurrent.BlockingQueue<StructurePacket> packet_queue;

    String selected = null;

    StructurePacket previous_packet = new StructurePacket();
    int socket_base_number;

    java.nio.ByteBuffer vbf;
    boolean vbf_lock = false;

    byte[][] source;

    private XYSeriesCollection dataset;
    private XYSeries series_psnr;

    private XYSeriesCollection errors;
    private XYSeries series_error, series_total;
    private XYSeriesCollection received;
    private XYSeries series_generated_total;
    private XYSeries series_received_total;
    private XYSeries[] series_received;

    private int[] num_packets;
    long absolute_time;
    int video_GOP_num;
    int framesPerGOP;
    int YUV_SIZE;
    Display display = null;

    public ClientG() throws IOException {
        Util.Tools.delete_file(StructurePT.file);
        initComponents();
        basicInit();
        org.jfree.ui.RefineryUtilities.centerFrameOnScreen(this);
        readYUV();
    }

    private void basicInit() {
        framesPerGOP = StructurePT.frameperGOP;

        YUV_SIZE = StructurePT.YUV_SIZE;

        packet_queue = new java.util.concurrent.LinkedBlockingQueue<StructurePacket>();

        vbf = java.nio.ByteBuffer.allocateDirect(Util.Tools.get_video_buffer_size());

        jProgressBar1.setMaximum(Util.Tools.get_video_buffer_size());
        jProgressBar1.setValue(0);
    }

    private void graphSetting() {
        dataset = new XYSeriesCollection();
        series_psnr = new XYSeries("PSNR");
        dataset.addSeries(series_psnr);
        jLineChart1.setDataset(dataset);

        errors = new XYSeriesCollection();
        series_error = new XYSeries("error");
        series_total = new XYSeries("total");
        errors.addSeries(series_error);
        errors.addSeries(series_total);
        graph_Error.setDataset(errors);

        received = new XYSeriesCollection();
        int len = networkInterfaces1.getSelectedInterfaces().length;
        series_received = new XYSeries[len];
        for (int i = 0; i < len; i++) {
            series_received[i] = new XYSeries(new Integer(i).toString());
            received.addSeries(series_received[i]);
        }
        series_generated_total = new XYSeries("generate_total");
        series_received_total = new XYSeries("received_total");
        received.addSeries(series_generated_total);
        received.addSeries(series_received_total);
        graph_Received.setDataset(received);
        num_packets = new int[len];
    }

    private void readYUV() throws IOException {
        String selected_one = StructurePT.original;
        String full_selected_one = StructurePT.repository_video_sequence + selected_one;
        java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(full_selected_one));
        java.nio.channels.FileChannel fc = fis.getChannel();
        int sz = (int) fc.size();
        process264Real.block_process.TargetVideoInfo videoInfo = new process264Real.block_process.TargetVideoInfo(sz, YUV_SIZE, framesPerGOP);
        videoInfo.Cal_FrameNum_GOPNum();
        videoInfo.printVideoInfo();
        System.out.println("file size : " + sz);
        ByteBuffer bb = null;

        source = new byte[videoInfo.GOP_num][];
        video_GOP_num = videoInfo.GOP_num;
        for (int i = 0; i < videoInfo.GOP_num; i++) {
            source[i] = new byte[(int) sz / videoInfo.GOP_num];
            bb = ByteBuffer.allocateDirect((int) sz / videoInfo.GOP_num);
            fc.read(bb);
            bb.position(0);
            bb.get(source[i]);
        }

        fis.close();
    }

    @SuppressWarnings("unchecked")

    private void initComponents() {

        graph_PSNR = new javax.swing.JPanel();
        jLineChart1 = new org.jfree.beans.JLineChart();
        Display = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        displayYUV1 = new mybeans.DisplayYUV();
        networkInterfaces1 = new mybeans.NetworkInterfaces();
        jProgressBar2 = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        textIP = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        Control = new javax.swing.JToolBar();
        btnStart = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        graph_Error = new org.jfree.beans.JLineChart();
        jPanel2 = new javax.swing.JPanel();
        graph_Received = new org.jfree.beans.JLineChart();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("JVirtual Client");

        graph_PSNR.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLineChart1.setLegendItemFont(new java.awt.Font("Georgia", 0, 12));
        jLineChart1.setLegendPosition(org.jfree.beans.LegendPosition.NONE);
        jLineChart1.setPlotBackgroundAlpha(0.5F);
        jLineChart1.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        jLineChart1.setShapesVisible(true);
        jLineChart1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLineChart1.setSource("");
        jLineChart1.setSubtitle("");
        jLineChart1.setTitle("");
        jLineChart1.setXAxisLabel("Time (sec)");
        jLineChart1.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        jLineChart1.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));
        jLineChart1.setYAxisLabel("PSNR (dB)");
        jLineChart1.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        jLineChart1.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));

        javax.swing.GroupLayout jLineChart1Layout = new javax.swing.GroupLayout(jLineChart1);
        jLineChart1.setLayout(jLineChart1Layout);
        jLineChart1Layout.setHorizontalGroup(
                jLineChart1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 417, Short.MAX_VALUE)
        );
        jLineChart1Layout.setVerticalGroup(
                jLineChart1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 195, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout graph_PSNRLayout = new javax.swing.GroupLayout(graph_PSNR);
        graph_PSNR.setLayout(graph_PSNRLayout);
        graph_PSNRLayout.setHorizontalGroup(
                graph_PSNRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLineChart1, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
        );
        graph_PSNRLayout.setVerticalGroup(
                graph_PSNRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(graph_PSNRLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLineChart1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))
        );

        Display.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        displayYUV1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        displayYUV1.setPreferredSize(new java.awt.Dimension(180, 150));
        displayYUV1.setYuv_height(144);
        displayYUV1.setYuv_width(176);

        javax.swing.GroupLayout displayYUV1Layout = new javax.swing.GroupLayout(displayYUV1);
        displayYUV1.setLayout(displayYUV1Layout);
        displayYUV1Layout.setHorizontalGroup(
                displayYUV1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 176, Short.MAX_VALUE)
        );
        displayYUV1Layout.setVerticalGroup(
                displayYUV1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 146, Short.MAX_VALUE)
        );

        jProgressBar2.setMaximum(300);

        jLabel1.setFont(new java.awt.Font("Georgia", 0, 12));
        jLabel1.setText("Display Buffer");

        jLabel2.setFont(new java.awt.Font("Georgia", 0, 12));
        jLabel2.setText("Processing Buffer");

        jLabel5.setFont(new java.awt.Font("Georgia", 0, 12));
        jLabel5.setText("Server Address: ");

        textIP.setText("141.223.65.100");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icon/logo2_001.png")));

        jLabel6.setFont(new java.awt.Font("Georgia", 0, 12));
        jLabel6.setText("FEC Scheme");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "fountain code", "RS code" }));

        javax.swing.GroupLayout DisplayLayout = new javax.swing.GroupLayout(Display);
        Display.setLayout(DisplayLayout);
        DisplayLayout.setHorizontalGroup(
                DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, DisplayLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                .addComponent(displayYUV1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jProgressBar2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 206, Short.MAX_VALUE)
                                                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                                                .addComponent(jLabel6)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGap(0, 0, Short.MAX_VALUE))))
                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(textIP, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel5))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                        .addComponent(networkInterfaces1, javax.swing.GroupLayout.DEFAULT_SIZE, 830, Short.MAX_VALUE)
        );
        DisplayLayout.setVerticalGroup(
                DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(DisplayLayout.createSequentialGroup()
                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(textIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(displayYUV1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(DisplayLayout.createSequentialGroup()
                                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(DisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel6)
                                                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(15, 15, 15)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(networkInterfaces1, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Control.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0, 0, 0)));
        Control.setFloatable(false);
        Control.setRollover(true);

        btnStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/pc16/control_play.png")));
        btnStart.setFocusable(false);
        btnStart.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnStart.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });
        Control.add(btnStart);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        graph_Error.setLegendItemFont(new java.awt.Font("Georgia", 0, 12));
        graph_Error.setPlotBackgroundAlpha(0.5F);
        graph_Error.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_Error.setShapesVisible(true);
        graph_Error.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        graph_Error.setSource("");
        graph_Error.setSubtitle("");
        graph_Error.setTitle("");
        graph_Error.setXAxisLabel("Time (sec)");
        graph_Error.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        graph_Error.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));
        graph_Error.setYAxisLabel("# of symbols");
        graph_Error.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));

        javax.swing.GroupLayout graph_ErrorLayout = new javax.swing.GroupLayout(graph_Error);
        graph_Error.setLayout(graph_ErrorLayout);
        graph_ErrorLayout.setHorizontalGroup(
                graph_ErrorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 417, Short.MAX_VALUE)
        );
        graph_ErrorLayout.setVerticalGroup(
                graph_ErrorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 207, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(graph_Error, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 211, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(graph_Error, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        graph_Received.setLegendItemFont(new java.awt.Font("Georgia", 0, 12));
        graph_Received.setPlotBackgroundAlpha(0.5F);
        graph_Received.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_Received.setShapesVisible(true);
        graph_Received.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        graph_Received.setSource("");
        graph_Received.setSubtitle("");
        graph_Received.setTitle("");
        graph_Received.setXAxisLabel("Time (sec)");
        graph_Received.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        graph_Received.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));
        graph_Received.setYAxisLabel("# of packets");
        graph_Received.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        graph_Received.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));

        javax.swing.GroupLayout graph_ReceivedLayout = new javax.swing.GroupLayout(graph_Received);
        graph_Received.setLayout(graph_ReceivedLayout);
        graph_ReceivedLayout.setHorizontalGroup(
                graph_ReceivedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 336, Short.MAX_VALUE)
        );
        graph_ReceivedLayout.setVerticalGroup(
                graph_ReceivedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 205, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(graph_Received, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 223, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(graph_Received, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                                        .addContainerGap()))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Georgia", 0, 14));
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Georgia", 0, 14));
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(51, 51, 51)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                                        .addComponent(jScrollPane2))
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(Display, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(graph_PSNR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE))))
                                        .addComponent(Control, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(Control, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(graph_PSNR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(Display, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {
        graphSetting();
        btnStart.setEnabled(false);

        switch (jComboBox2.getSelectedIndex()) {
            case 0:
                fType = TypeFEC.fountainP;
                break;
            case 1:
                fType = TypeFEC.RS223;
                break;
            default:
                System.out.println("FEC type error!");
        }
        java.net.NetworkInterface[] nint = networkInterfaces1.getSelectedInterfaces();
        paths = new StructurePath[nint.length];

        socket_base_number = 30000 + new java.util.Random().nextInt(10000);
        for (int i = 0; i < nint.length; i++) {
            paths[i] = new StructurePath();
            paths[i].nface = nint[i];
            if (nint[i].getName().startsWith("eth0")) {
                paths[i].bandwidth_max = StructurePT.bandwidth_max_eth0;
                paths[i].bandwidth_min = StructurePT.bandwidth_min_eth0;
                paths[i].interfaceType = StructurePT.Ethernet;
            } else if (nint[i].getName().startsWith("eth1")) {
                paths[i].bandwidth_max = StructurePT.bandwidth_max_eth1;
                paths[i].bandwidth_min = StructurePT.bandwidth_min_eth1;
                paths[i].interfaceType = StructurePT.Ethernet;
            } else if (nint[i].getName().startsWith("eth")) {
                paths[i].bandwidth_max = StructurePT.bandwidth_max_eth;
                paths[i].bandwidth_min = StructurePT.bandwidth_min_eth;
                paths[i].interfaceType = StructurePT.Ethernet;
                paths[i].costModel = new CostModel(StructurePT.Ethernet, StructurePT.EthernetStartData);
            } else if (nint[i].getName().startsWith("ppp")) {
                paths[i].bandwidth_max = StructurePT.bandwidth_max_ppp;
                paths[i].bandwidth_min = StructurePT.bandwidth_min_ppp;

            } else {

                if (nint[i].getDisplayName().startsWith("LGE"))
                {
                    paths[i].interfaceType = StructurePT.LTE;
                    paths[i].costModel = new CostModel(StructurePT.LTE, StructurePT.LTEStartData);
                } else
                {
                    paths[i].interfaceType = StructurePT.WiFi;
                    paths[i].costModel = new CostModel(StructurePT.WiFi, StructurePT.WiFiStartData);

                }

                paths[i].bandwidth_max = StructurePT.bandwidth_max_others;
                paths[i].bandwidth_min = StructurePT.bandwidth_min_others;
            }

            try {
                paths[i].socket = new DatagramSocket(socket_base_number + i, nint[i].getInetAddresses().nextElement());
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
            try {
                paths[i].socket.setSoTimeout(StructurePT.timeout);
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
            if (false) {
                try {
                    paths[i].socket.setReceiveBufferSize(StructurePT.receiver_buffer_size);
                } catch (SocketException ex) {
                    ex.printStackTrace();
                }
            }
            paths[i].address = paths[i].socket.getLocalAddress().toString();
            paths[i].bandwidth = StructurePT.initial_bandwidth;
            paths[i].bandwidth_step = StructurePT.initial_bandwidth_step;
        }
        StructurePacket p = new StructurePacket();

        p.pType = TypePacket.request;
        p.fType = fType;

        p.num_addresses = paths.length;

        p.costModel = new CostModel[paths.length];
        for (int i = 0; i < paths.length; i++) {
            p.costModel[i] = paths[i].costModel;
        }

        byte data[] = Util.Tools.object_to_bytes(p);
        p.destination_address = textIP.getText().trim();
        java.net.InetAddress addr = null;
        try {
            addr = java.net.InetAddress.getByName(p.destination_address);
        } catch (UnknownHostException ex) {
            System.out.println("Could not find " + textIP.getText());
            ex.printStackTrace();
        }
        java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(data, data.length, addr, StructurePT.server_port);
        absolute_time = System.currentTimeMillis();

        t_WaitInner = new Thread(new WaitInner());
        t_WaitInner.start();

        for (int i = 0; i < paths.length; i++) {
            try {
                paths[i].socket.send(sendPacket);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        t_ProcessPackets = new Thread(new ProcessPackets());
        t_ProcessPackets.start();

        if (t_DisplayInner == null) {
            t_DisplayInner = new Thread(new DisplayInner());
            t_DisplayInner.start();
        }
    }

    public static void main(String args[]) {
        try {
            javax.swing.UIManager.setLookAndFeel(StructurePT.laf);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        ClientG c = null;
        try {
            c = new ClientG();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        c.setVisible(true);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        c.networkInterfaces1.doLayout();
        c.pack();
    }

    class DisplayInner implements Runnable {
        byte[] buffer;
        long reference_time;
        int thread_start = 0;
        int playedFrameNum = 0;

        public DisplayInner() {
            int frameSize = Util.Tools.get_Frame_size();

            buffer = new byte[frameSize];

            reference_time = System.currentTimeMillis();
        }

        public void run() {
            while (true) {
                if (vbf.position() < vbf.capacity() * 0.5 && thread_start == 1) {
                    feedback(previous_packet.gop_index + 1, 0.0);
                }
                thread_start = 1;
                while (vbf.position() < buffer.length) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                System.out.println("영상 출력 준비 완료");

                while (true) {

                    if (vbf_lock == false) {
                        vbf_lock = true;
                        System.out.println("vbf.position() 1: " + vbf.position());
                        vbf.flip();
                        System.out.println("vbf.position() 2: " + vbf.position());
                        vbf.get(buffer);
                        System.out.println("vbf.position() 3: " + vbf.position());
                        vbf.compact();
                        System.out.println("vbf.position() 4: " + vbf.position());
                        vbf_lock = false;
                        break;
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                System.out.println("vbf.position() 5: " + vbf.position());
                jProgressBar1.setValue(vbf.position());

                playedFrameNum++;


                if (display == null) {
                    display = new Display();
                    display.pack();

                }
                display.setBuffer(buffer);
                display.draw();

                while (System.currentTimeMillis() - reference_time < StructurePT.interframetime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                reference_time = System.currentTimeMillis();


            }
        }
    }

    class WaitInner implements Runnable {

        int index;
        byte[] data;
        java.net.DatagramPacket receivePacket;
        StructurePacket p;
        long[] previous_times;
        long receiving_time;

        public WaitInner() {
            index = 0;
            data = new byte[StructurePT.packetsize];
            receivePacket = new java.net.DatagramPacket(data, data.length);
            p = null;
            previous_times = new long[paths.length];
            java.util.Arrays.fill(previous_times, System.currentTimeMillis());
            receiving_time = 0;
        }

        public void run() {
            while (true) {
                try {
                    assert index < Integer.MAX_VALUE;
                    if(paths.length!=0)
                        paths[(++index) % paths.length].socket.receive(receivePacket);

                    receiving_time = System.currentTimeMillis();
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (StructurePT.is_blink) {
                    selected = paths[index % paths.length].socket.getLocalAddress().toString();
                }
                try {
                    p = (StructurePacket) Util.Tools.bytes_to_object(receivePacket.getData());
                } catch (IOException ex) {
                    continue;
                }

                p.path_index = index % paths.length;
                paths[p.path_index].costModel.updateCurrentData(receivePacket.getLength());

                if (p.pType == TypePacket.ping) {
                    try {
                        paths[p.path_index].socket.send(receivePacket);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                packet_queue.add(p);

                jProgressBar2.setValue(packet_queue.size());
                long diff = receiving_time - previous_times[p.path_index];
                double norm = (StructurePT.packetsize * 8.0 * 1000) / paths[p.path_index].bandwidth;
                if (diff > norm) {
                    paths[p.path_index].bandwidth -= paths[p.path_index].bandwidth_step;
                } else {
                    paths[p.path_index].bandwidth += paths[p.path_index].bandwidth_step;
                }

                paths[p.path_index].bandwidth =
                        Util.Tools.clip(paths[p.path_index].bandwidth, paths[p.path_index].bandwidth_max, paths[p.path_index].bandwidth_min);
                previous_times[p.path_index] = receiving_time;
                if (p.pType == TypePacket.last) {
                    reportSummary();
                    for (int i = 0; i < paths.length; i++) {
                    }
                    reset();
                    return;
                }
            }
        }

        private void reportSummary() {


            String result_packet = "Packet: ";
            String result_detail_packet_wifi = "---------- packet result detail wifi------------\n";
            double sum_packet = 0;
            int count_wifi = series_received[0].getItemCount();
            for (int i = 0; i < count_wifi; i++) {
                double packet = series_received[0].getY(i).doubleValue();
                sum_packet += packet;
                result_detail_packet_wifi += packet + "\n";
            }
            System.out.println(result_detail_packet_wifi);


            String result_detail_packet_lte = "---------- packet result detail lte------------\n";
            sum_packet = 0;
            int count_lte = series_received[1].getItemCount();
            for (int i = 0; i < count_lte; i++) {
                double packet = series_received[1].getY(i).doubleValue();
                sum_packet += packet;
                result_detail_packet_lte += packet + "\n";
            }
            System.out.println(result_detail_packet_lte);

            String result_detail_packet_generated = "---------- packet result detail generated total------------\n";
            sum_packet = 0;
            int count_generated = series_generated_total.getItemCount();
            for (int i = 0; i < count_generated; i++) {
                double packet = series_generated_total.getY(i).doubleValue();
                sum_packet += packet;
                result_detail_packet_generated += packet + "\n";
            }
            System.out.println(result_detail_packet_generated);

            String result_detail_packet_received = "---------- packet result detail received total------------\n";
            sum_packet = 0;
            int count_received = series_received_total.getItemCount();
            for (int i = 0; i < count_received; i++) {
                double packet = series_received_total.getY(i).doubleValue();
                sum_packet += packet;
                result_detail_packet_received += packet + "\n";
            }
            System.out.println(result_detail_packet_received);

            String result_avg_psnr = "Average PSNR: ";
            String result_detail_psnr = "---------- psnr result detail ------------\n";
            double sum_psnr = 0;
            int count_psnr = series_psnr.getItemCount();
            for (int i = 0; i < count_psnr; i++) {
                double psnr = series_psnr.getY(i).doubleValue();
                sum_psnr += psnr;
                result_detail_psnr += psnr + "\n";
            }

            double avg_psnr = sum_psnr / count_psnr;
            result_avg_psnr += Util.Tools.fixedWidthDoubletoString(avg_psnr, 5, 3) + "\n";
            System.out.println(result_detail_psnr);

            String result_avg_rate = "Average Video Rate: ";
            String result_detail_rate = "---------- video rate result detail ------------\n";
            System.out.println("---------- the number of source symbols ------------");
            double sum_rate = 0;
            int count_rate = series_total.getItemCount();
            for (int i = 0; i < count_rate; i++) {
                double rate = (series_total.getY(i).doubleValue() - series_error.getY(i).doubleValue()) * StructurePT.symbolSize * 8.0;
                sum_rate += rate;
                result_detail_rate += rate + "\n";
                System.out.println(series_total.getY(i));
            }
            System.out.println(result_detail_rate);

            double avg_rate = sum_rate / count_rate;
            result_avg_rate += Util.Tools.fixedWidthDoubletoString(avg_rate, 5, 3) + "\n";

            String result_total = result_avg_psnr + result_avg_rate;
            SwingUtilities.invokeLater(new DisplayMessage(result_total));
        }
    }

    class DisplayMessage implements Runnable {

        private String message;
        public DisplayMessage(String message) {
            this.message = message;
        }

        public void run() {
            jTextArea1.append(message);
            jTextArea1.setCaretPosition(jTextArea1.getText().length());
        }
    }

    class DisplayMessage2 implements Runnable {

        private String message;

        public DisplayMessage2(String message) {
            this.message = message;
        }

        public void run() {
            jTextArea2.append(message);
            jTextArea2.setCaretPosition(jTextArea2.getText().length());
        }
    }

    class BlinkTable implements Runnable {
        public void run() {
            if (!StructurePT.is_blink) {
                return;
            } else {
                blink();
            }
        }

        private void blink() {
            while (true) {
                networkInterfaces1.blink(selected);
                selected = "tmp1231";
                try {
                    Thread.sleep(StructurePT.blink_interval);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    class ProcessPackets implements Runnable {

        byte[] local_fountain_encoded;
        StructurePacket current_packet;
        process264Real.block_process.ProcessBlock receiver;
        int fileWriteFirst = 0;

        public ProcessPackets() {
            local_fountain_encoded = null;
            current_packet = null;
            previous_packet.gop_index = -1;
            System.out.println("===============> selected FEC :: " + fType.toString());
            receiver = new process264Real.block_process.ProcessBlock(fType);
        }

        public void run() {
            while (true) {
                try {
                    current_packet = packet_queue.take();

                    if (current_packet.pType == TypePacket.last) {
                        receiver.setFountainEncoded(local_fountain_encoded);
                        boolean[] success = null;
                        try {
                            success = receiver.deprocess(previous_packet.k, previous_packet.t, previous_packet.symbol_size);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        while (true) {
                            if (vbf_lock == false) {
                                vbf_lock = true;
                                vbf.put(receiver.source);
                                vbf_lock = false;
                                break;
                            } else {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }

                        int error = getError(success, success.length);
                        series_error.add(Util.Tools.getTime(absolute_time), error);
                        series_total.add(Util.Tools.getTime(absolute_time), previous_packet.k);
                        double psnr = Util.Tools.calcPSNR_y(source[previous_packet.gop_index % video_GOP_num], receiver.source);
                        series_psnr.add(Util.Tools.getTime(absolute_time), psnr);
                        for (int i = 0; i < paths.length; i++) {
                            if (i == (current_packet.path_index)) {
                                num_packets[i]--;
                            }
                            series_received[i].add(Util.Tools.getTime(absolute_time), num_packets[i]);
                        }
                        series_generated_total.add(Util.Tools.getTime(absolute_time), Math.ceil(previous_packet.t * previous_packet.symbol_size / (double) StructurePT.packetpayloadsize));

                        series_received_total.add(Util.Tools.getTime(absolute_time), Util.Tools.sum_array(num_packets));
                        java.util.Arrays.fill(num_packets, 0);
                        num_packets[current_packet.path_index]++;
                        return;
                    }
                    jProgressBar2.setValue(packet_queue.size());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                num_packets[current_packet.path_index]++;

                if (current_packet.gop_index > previous_packet.gop_index && previous_packet.gop_index
                        != -1) {
                    receiver.setFountainEncoded(local_fountain_encoded);
                    receiver.setPlr(previous_packet.plr);
                    boolean[] success = null;
                    try {
                        success = receiver.deprocess(previous_packet.k, previous_packet.t, previous_packet.symbol_size);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    while (true) {
                        if (vbf_lock == false) {
                            vbf_lock = true;
                            if (receiver.source == null) {
                                continue;
                            }
                            vbf.put(receiver.source);
                            vbf_lock = false;
                            break;
                        } else {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    int error = getError(success, success.length);
                    series_error.add(Util.Tools.getTime(absolute_time), error);
                    series_total.add(Util.Tools.getTime(absolute_time), previous_packet.k);
                    double psnr = Util.Tools.calcPSNR_y(source[previous_packet.gop_index % video_GOP_num], receiver.source);
                    series_psnr.add(Util.Tools.getTime(absolute_time), psnr);
                    for (int i = 0; i < paths.length; i++) {
                        if (i == (current_packet.path_index)) {
                            num_packets[i]--;
                        }
                        series_received[i].add(Util.Tools.getTime(absolute_time), num_packets[i]);
                    }
                    series_generated_total.add(Util.Tools.getTime(absolute_time), Math.ceil(previous_packet.t * previous_packet.symbol_size / (double) StructurePT.packetpayloadsize));
                    series_received_total.add(Util.Tools.getTime(absolute_time), Util.Tools.sum_array(num_packets));
                    java.util.Arrays.fill(num_packets, 0);
                    num_packets[current_packet.path_index]++;
                }

                if (current_packet.gop_index > previous_packet.gop_index) {
                    local_fountain_encoded = new byte[current_packet.t * current_packet.symbol_size];
                    receiver.non_error = new boolean[current_packet.t];
                    java.util.Arrays.fill(receiver.non_error, false);
                    previous_packet.k = current_packet.k;
                    previous_packet.plr = current_packet.plr;
                    previous_packet.gop_index = current_packet.gop_index;
                    previous_packet.t = current_packet.t;
                    previous_packet.symbol_size = current_packet.symbol_size;

                }
                if (current_packet.gop_index < previous_packet.gop_index && local_fountain_encoded
                        == null) {
                    continue;
                } else if (previous_packet.gop_index == current_packet.gop_index) {
                    for (int i = 0; (i < current_packet.payload.length) && (current_packet.fountain_id_start + i < current_packet.t * current_packet.symbol_size); i++) {
                        local_fountain_encoded[current_packet.fountain_id_start + i] = current_packet.payload[i];
                    }
                    if (current_packet.fountain_id_start == 0) {
                        for (int i = 0; i < current_packet.payload.length / current_packet.symbol_size; i++) {
                            receiver.non_error[i] = true;
                        }
                    } else {
                        for (int i = current_packet.fountain_id_start / current_packet.symbol_size;
                             i < (current_packet.fountain_id_start + current_packet.payload.length) / current_packet.symbol_size; i++) {
                            if (i < receiver.non_error.length) {
                                receiver.non_error[i] = true;
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    continue;
                }

                feedback(previous_packet.gop_index
                                + 1,
                        getplr(receiver.non_error, Math.min(current_packet.fountain_id_start + StructurePT.packetpayloadsize, current_packet.t)));
            }
        }

        private int getError(boolean[] array, int num) {
            int error = 0;
            for (int i = 0; i < num; i++) {
                if (array[i] == false) {
                    error++;
                }
            }
            return error;
        }

        private double getplr(boolean[] non_error, int num) {
            if (non_error == null) {
                return 0.0;
            }
            return getError(non_error, num) / ((double) num);
        }
    }

    private void feedback(int request_gop, double plr) {
        StructurePacket p = new StructurePacket();
        p.pType = TypePacket.feedback;
        p.plr = plr;
        p.bandwidth = new double[paths.length];
        p.costModel = new CostModel[paths.length];
        if (vbf.position() < (vbf.capacity() * 0.5)) {
            p.gop_index = request_gop;
        } else {
            p.gop_index = 0;
        }
        for (int i = 0; i < paths.length; i++) {
            p.bandwidth[i] = paths[i].bandwidth;
            p.costModel[i] = paths[i].costModel;
        }
        byte[] data = Util.Tools.object_to_bytes(p);
        java.net.InetAddress addr = null;
        try {
            addr = java.net.InetAddress.getByName(textIP.getText());
        } catch (UnknownHostException ex) {
            System.out.println("Could not find " + textIP.getText());
            ex.printStackTrace();
        }
        java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(data, data.length, addr, StructurePT.server_port);
        for (int i = 0; i < paths.length; i++) {
            try {
                paths[i].socket.send(sendPacket);

                ex.printStackTrace();
            }
        }
    }

    private void reset() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        if (t_BlinkTable != null) {
            t_BlinkTable.interrupt();
        }
        t_WaitInner.interrupt();

        t_ProcessPackets.interrupt();
        btnStart.setEnabled(true);

    }
    private javax.swing.JToolBar Control;
    private javax.swing.JPanel Display;
    private javax.swing.JButton btnStart;
    private mybeans.DisplayYUV displayYUV1;
    private org.jfree.beans.JLineChart graph_Error;
    private javax.swing.JPanel graph_PSNR;
    private org.jfree.beans.JLineChart graph_Received;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private org.jfree.beans.JLineChart jLineChart1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private mybeans.NetworkInterfaces networkInterfaces1;
    private javax.swing.JTextField textIP;
}
