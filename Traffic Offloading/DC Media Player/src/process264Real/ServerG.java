package process264Real;

import NetworkCost.CostModel;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.SocketException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ServerG extends javax.swing.JFrame {
    class StructureServerPath implements Serializable {
        int delay;      
        int g;          
        double bandwidth;       
        DatagramPacket packet;  
        CostModel costModel;	
    }
    
    boolean started = false;        
   
    Thread t_SendeInner;        
    Thread t_WaitInner;         
    Thread t_ProcessPackets;    
    Thread t_soundServer;
    
    FEC.common.TypeFEC fType;
    java.net.DatagramSocket socket;     
    StructureServerPath paths[];        
    
    int packet_stamp;
    int num_addresses;      
    int num_path;           
    private double plr;     
    java.util.concurrent.BlockingQueue<java.net.DatagramPacket> packet_queue;  
    
    private String filename = StructurePT.TFile;    
    int requested_gop;      
    private XYSeries series_vr;             
    private XYSeriesCollection dataset_vr;  
    
    private XYSeries series_coderate;       
    private XYSeries series_plr;
    private XYSeriesCollection dataset_plr;
    
    private XYSeries[] series_distributors;     
    private XYSeriesCollection distributor;    
    
    private XYSeries[] series_delays;       
    private XYSeriesCollection dataset_delay;   
    
    long absolte_time;      
    process264Real.block_process.TargetVideoInfo videoInfo;     
    String clientIP;        
    int framesPerGOP;       
    int YUV_SIZE;

    public ServerG() {
        initComponents();
        init();             

        symbolSizeTextField.setText(StructurePT.symbolSize+"");

        start();            
       
        org.jfree.ui.RefineryUtilities.centerFrameOnScreen(this);
    }

   
    private void init() {
        try {
            if (socket == null) {
                socket = new java.net.DatagramSocket(StructurePT.server_port);
                
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        framesPerGOP = StructurePT.frameperGOP;
        YUV_SIZE = StructurePT.YUV_SIZE;
        packet_queue = new java.util.concurrent.LinkedBlockingQueue<java.net.DatagramPacket>();
        packet_stamp = 0;
        started = false;
        num_path = 0;
        requested_gop = 0;
        plr = 0.2;
        t_SendeInner = new Thread(new SendInner());
        jTextField1.setText(filename);
    }
    
    private void start() {
        t_WaitInner = new Thread(new WaitInner());
        t_WaitInner.start();
        t_ProcessPackets = new Thread(new ProcessPackets());
        t_ProcessPackets.start();
    }
    
    private void constructGraphs() {
        dataset_plr = new XYSeriesCollection();
        graph_CodeRate.setDataset(dataset_plr);
        series_coderate = new XYSeries("code rate");
        series_plr = new XYSeries("packet loss rate");
        dataset_plr.addSeries(series_coderate);
        dataset_delay = new XYSeriesCollection();
        graph_Delays.setDataset(dataset_delay);
        series_delays = new XYSeries[num_addresses];
        for (int i = 0; i < num_addresses; i++) {
            series_delays[i] = new XYSeries(i);
            dataset_delay.addSeries(series_delays[i]);
        }
        
        dataset_vr = new XYSeriesCollection();
        graph_VR.setDataset(dataset_vr);
        series_vr = new XYSeries("QP");
        dataset_vr.addSeries(series_vr);
        distributor = new XYSeriesCollection();
        graph_Distributor.setDataset(distributor);
        series_distributors = new XYSeries[num_addresses];
        for (int i = 0; i < num_addresses; i++) {
            series_distributors[i] = new XYSeries(i);
            distributor.addSeries(series_distributors[i]);
        }
    }
    
    private void constructPaths() {
        paths = new StructureServerPath[num_addresses];
        for (int i = 0; i < num_addresses; i++) {
            paths[i] = new StructureServerPath();
            paths[i].bandwidth = StructurePT.initial_bandwidth;		
            paths[i].delay = StructurePT.delay;
        }
    }
    
    @SuppressWarnings("unchecked")
   
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        Summary = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        graph_Distributor = new org.jfree.beans.JLineChart();
        graph_Delays = new org.jfree.beans.JLineChart();
        graph_CodeRate = new org.jfree.beans.JLineChart();
        Controller = new javax.swing.JToolBar();
        Config = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        label1 = new java.awt.Label();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        symbolSizeTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        graph_VR = new org.jfree.beans.JLineChart();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("JVirtual Server");

        Summary.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Georgia", 0, 14)); 
        jTextArea1.setRows(5);
        Summary.setViewportView(jTextArea1);

        graph_Distributor.setLegendItemFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_Distributor.setPlotBackgroundAlpha(0.5F);
        graph_Distributor.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_Distributor.setShapesVisible(true);
        graph_Distributor.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        graph_Distributor.setSource("");
        graph_Distributor.setSubtitle("");
        graph_Distributor.setTitle("");
        graph_Distributor.setXAxisLabel("Time (sec)");
        graph_Distributor.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_Distributor.setXAxisPositiveArrowVisible(true);
        graph_Distributor.setXAxisScale(org.jfree.beans.AxisScale.INTEGER);
        graph_Distributor.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 
        graph_Distributor.setYAxisLabel("Distribution weight");
        graph_Distributor.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        graph_Distributor.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));

        javax.swing.GroupLayout graph_DistributorLayout = new javax.swing.GroupLayout(graph_Distributor);
        graph_Distributor.setLayout(graph_DistributorLayout);
        graph_DistributorLayout.setHorizontalGroup(
                graph_DistributorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 373, Short.MAX_VALUE)
        );
        graph_DistributorLayout.setVerticalGroup(
                graph_DistributorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 216, Short.MAX_VALUE)
        );

        graph_Delays.setLegendItemFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_Delays.setPlotBackgroundAlpha(0.5F);
        graph_Delays.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_Delays.setShapesVisible(true);
        graph_Delays.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        graph_Delays.setSource("");
        graph_Delays.setSubtitle("");
        graph_Delays.setTitle("");
        graph_Delays.setXAxisLabel("Time (sec)");
        graph_Delays.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_Delays.setXAxisPositiveArrowVisible(true);
        graph_Delays.setXAxisScale(org.jfree.beans.AxisScale.INTEGER);
        graph_Delays.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 
        graph_Delays.setYAxisLabel("Delay (millisecond)");
        graph_Delays.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_Delays.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 

        javax.swing.GroupLayout graph_DelaysLayout = new javax.swing.GroupLayout(graph_Delays);
        graph_Delays.setLayout(graph_DelaysLayout);
        graph_DelaysLayout.setHorizontalGroup(
                graph_DelaysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        graph_DelaysLayout.setVerticalGroup(
                graph_DelaysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        graph_CodeRate.setLegendItemFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_CodeRate.setPlotBackgroundAlpha(0.5F);
        graph_CodeRate.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_CodeRate.setShapesVisible(true);
        graph_CodeRate.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        graph_CodeRate.setSource("");
        graph_CodeRate.setSubtitle("");
        graph_CodeRate.setTitle("");
        graph_CodeRate.setXAxisLabel("Time (sec)");
        graph_CodeRate.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_CodeRate.setXAxisPositiveArrowVisible(true);
        graph_CodeRate.setXAxisScale(org.jfree.beans.AxisScale.INTEGER);
        graph_CodeRate.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 
        graph_CodeRate.setYAxisLabel("Code rate");
        graph_CodeRate.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_CodeRate.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10));

        javax.swing.GroupLayout graph_CodeRateLayout = new javax.swing.GroupLayout(graph_CodeRate);
        graph_CodeRate.setLayout(graph_CodeRateLayout);
        graph_CodeRateLayout.setHorizontalGroup(
                graph_CodeRateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 376, Short.MAX_VALUE)
        );
        graph_CodeRateLayout.setVerticalGroup(
                graph_CodeRateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 206, Short.MAX_VALUE)
        );

        Controller.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0, 0, 0)));
        Controller.setRollover(true);

        Config.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel1.setFont(new java.awt.Font("Georgia", 0, 12)); 
        jLabel1.setText("YUV File Name: ");

        jTextField1.setFont(new java.awt.Font("Georgia", 0, 12)); 

        jButton1.setFont(new java.awt.Font("Georgia", 0, 12)); 
        jButton1.setText("Select...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        label1.setFont(new java.awt.Font("Georgia", 0, 12));
        label1.setText("Repeat :");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5" }));
        jComboBox1.setSelectedIndex(0);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icon/logo2_001.png"))); 

        jLabel3.setText("symbol size");

        symbolSizeTextField.setEditable(false);
        symbolSizeTextField.setText("25");
        symbolSizeTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                symbolSizeTextFieldActionPerformed(evt);
            }
        });

        jLabel4.setText("bytes");

        javax.swing.GroupLayout ConfigLayout = new javax.swing.GroupLayout(Config);
        Config.setLayout(ConfigLayout);
        ConfigLayout.setHorizontalGroup(
                ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ConfigLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(ConfigLayout.createSequentialGroup()
                                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                                                .addContainerGap())
                                        .addGroup(ConfigLayout.createSequentialGroup()
                                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(ConfigLayout.createSequentialGroup()
                                                                .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(jLabel1))
                                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(ConfigLayout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE))
                                                        .addGroup(ConfigLayout.createSequentialGroup()
                                                                .addGap(51, 51, 51)
                                                                .addComponent(jLabel3)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(symbolSizeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jButton1)
                                                        .addComponent(jLabel4))
                                                .addGap(14, 14, 14))))
        );
        ConfigLayout.setVerticalGroup(
                ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ConfigLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(jButton1)
                                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel3)
                                                .addComponent(symbolSizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel4)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                                .addContainerGap())
        );

        graph_VR.setLegendPosition(org.jfree.beans.LegendPosition.NONE);
        graph_VR.setPlotBackgroundAlpha(0.5F);
        graph_VR.setPlotBackgroundPaint(new java.awt.Color(204, 204, 255, 255));
        graph_VR.setShapesVisible(true);
        graph_VR.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        graph_VR.setSource("");
        graph_VR.setSubtitle("");
        graph_VR.setTitle("");
        graph_VR.setXAxisLabel("Time (sec)");
        graph_VR.setXAxisLabelFont(new java.awt.Font("Georgia", 0, 12)); 
        graph_VR.setXAxisPositiveArrowVisible(true);
        graph_VR.setXAxisScale(org.jfree.beans.AxisScale.INTEGER);
        graph_VR.setXAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 
        graph_VR.setYAxisLabel("Video rate (bps)");
        graph_VR.setYAxisLabelFont(new java.awt.Font("Georgia", 0, 12));
        graph_VR.setYAxisTickLabelFont(new java.awt.Font("Georgia", 0, 10)); 

        javax.swing.GroupLayout graph_VRLayout = new javax.swing.GroupLayout(graph_VR);
        graph_VR.setLayout(graph_VRLayout);
        graph_VRLayout.setHorizontalGroup(
                graph_VRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 373, Short.MAX_VALUE)
        );
        graph_VRLayout.setVerticalGroup(
                graph_VRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(Controller, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(Config, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(graph_VR, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
                                                        .addComponent(graph_Distributor, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(Summary)
                                                        .addComponent(graph_Delays, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                                                        .addComponent(graph_CodeRate, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(Controller, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(Config, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(Summary, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(graph_VR, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                                        .addComponent(graph_CodeRate, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(graph_Delays, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                                        .addComponent(graph_Distributor, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        pack();
    }
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        jFileChooser1.setSelectedFile(new java.io.File(jTextField1.getText()));
        int returnVal = jFileChooser1.showOpenDialog(this);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            filename = jFileChooser1.getSelectedFile().toString();
            jTextField1.setText(filename);
        }
        
        t_SendeInner.interrupt();

        t_SendeInner = new Thread(new SendInner());
    }
    private void symbolSizeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
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
        ServerG sg = new ServerG();
        sg.setVisible(true);


    }
    
    class WaitInner implements Runnable {

        public void run() {
            byte data[] = new byte[StructurePT.packetsize];
            jTextArea1.insert("waiting for client connection\n", 0);

            java.net.DatagramPacket receivePacket;
            while (true) {
                receivePacket = new java.net.DatagramPacket(data, data.length);


                try {
                    socket.receive(receivePacket);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                packet_queue.add(receivePacket);
            }
        }
    }
    
    class ProcessPackets implements Runnable {

        java.net.DatagramPacket receivePacket;      
        public void run() {
            while (true) {
                try {
                    receivePacket = packet_queue.take();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                if (receivePacket == null) {
                    continue;
                }
                StructurePacket p = null;

                p = (StructurePacket) Util.Tools.bytes_to_object_Real(receivePacket.getData());     

                if (p == null) {
                    continue;
                }

                if (p.pType == TypePacket.ping) {
                    int received_index = p.path_index;
                    int calculated_delay = (int) ((System.currentTimeMillis() - p.time) / 2.0);
                    paths[received_index].delay = calculated_delay;

                    series_delays[received_index].add(Util.Tools.getTime(absolte_time), calculated_delay);

                } 
                else if (p.pType == TypePacket.feedback) {
                    plr = p.plr;
                    for (int i = 0; i < num_addresses; i++) {
                        paths[i].bandwidth = p.bandwidth[i];

                        paths[i].costModel = p.costModel[i];
                    }
                    requested_gop = p.gop_index;
                } 
                else if (p.pType == TypePacket.request) {
                    if (started == false) {
                        started = true;
                        fType = p.fType;
                        num_addresses = p.num_addresses;
                        
                        constructGraphs();
                        constructPaths();
                        
                        for (int i = 0; i < num_addresses; i++)
                        {
                            paths[i].costModel = p.costModel[i];		
                        }

                        paths[num_path].packet = receivePacket;     
                        num_path++;
                        clientIP = receivePacket.getAddress().getHostAddress();
                        t_SendeInner.start();

                    } else if (started == true && num_path < p.num_addresses && paths[num_path].packet == null) {
                        paths[num_path].packet = receivePacket;     
                        num_path++;
                    }
                }
            }
        }
    }
    
    class Algorithm {
        
        private void get_g() {
            for (int i = 0; i < num_addresses; i++) {
                int remaining_time = StructurePT.delay_constraint - paths[i].delay;     
                double transmission_time = StructurePT.packetsize * 8.0 / paths[i].bandwidth;  


                paths[i].g = (int) Math.floor((remaining_time / 1000.0) / transmission_time);  
                paths[i].g = Util.Tools.clip(paths[i].g, Integer.MAX_VALUE, 0);

                System.out.println(paths[i].bandwidth + "   paths[i].g    " + paths[i].g);
            }
        }
        
        public int getVR(double coderate) {

            int sum = 0;
            for (int i = 0; i < num_addresses; i++) {
                sum += paths[i].g;
                series_distributors[i].add(Util.Tools.getTime(absolte_time), paths[i].g);
            }
                        
            int vr = (int) Math.max(sum * StructurePT.packetpayloadsize * 8.0 * coderate * StructurePT.framerate / StructurePT.frameperGOP, 50 * 1000);
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ " + vr + " @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
            return vr;
        }

        public int getVRHo(double coderate, int tpSum) {

            double distortion;
            
            int vr = (int) (tpSum * StructurePT.packetpayloadsize * 8.0 * coderate * StructurePT.framerate / StructurePT.frameperGOP);
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ " + vr + " @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");

            distortion = StructurePT.GAMMA * Math.pow(vr, StructurePT.EPSILON);
            
            if (distortion < StructurePT.D_Max) {
                return vr;
            } else {
                return -1;		
            }
        }
    }
    
    class SendInner implements Runnable {

        Algorithm al = new Algorithm();     
        process264Real.block_process.ProcessBlock sender;   
        byte[] source;     
        java.util.Random r_distributor = new java.util.Random();
        java.nio.MappedByteBuffer bb = null;   
        double coderate = 0;
        Integer tp[] = null;
        int tpSum = 0;	
        
        public SendInner() {
            setSendInner();
        }
        
        private void setSendInner() {
            java.io.FileInputStream fis = null;
            try {
                fis = new java.io.FileInputStream(new java.io.File(filename));
                java.nio.channels.FileChannel fc = fis.getChannel();
                int sz = (int) fc.size();
                videoInfo = new process264Real.block_process.TargetVideoInfo(sz, YUV_SIZE, framesPerGOP);
                videoInfo.Cal_FrameNum_GOPNum();
                videoInfo.printVideoInfo();
                bb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, sz);
                source = new byte[(int) sz / videoInfo.GOP_num];
                fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        private int wrr_distributor() {
            if (num_addresses == 1) {
                return 0;
            }
            int sum = 0;
            int selected = 0;
            for (int i = 0; i < num_addresses; i++) {
                if (paths[i].packet != null) {
                    sum++;
                    selected = i;
                }
            }
            if (sum == 1) {
                return selected;
            }

            int cumulative[] = new int[num_addresses];
            for (int i = 0; i < num_addresses; i++) {
                if (i == 0) {
                    cumulative[i] = paths[i].g;
                } else {
                    cumulative[i] = cumulative[i - 1] + paths[i].g;
                }
            }
            int tmp = r_distributor.nextInt(cumulative[num_addresses - 1]);
            for (int i = 0; i < num_addresses; i++) {
                if (tmp < cumulative[i] && paths[i].packet != null) {
                    return i;
                }
            }
            return selected;
        }

        private int calculateNetworkCost() {
            int cost = 0;

            for (int i = 0; i < num_addresses; i++) {
            }

            return cost;
        }

        void getTP(int index) {

            tpSum = 0;

            for (int i = 0; i < num_addresses - 1; i++) {
                int temp = 1;

                for (int j = i + 1; j < num_addresses; j++) {
                    temp = temp * (paths[j].g + 1);
                }

                tp[i] = index / temp;
                index %= temp;
            }

            tp[num_addresses - 1] = index;

            for (int i = 0; i < num_addresses; i++) {
                tpSum += tp[i];
            }
        }
        
        public void run() {
            if (tp == null) {
                tp = new Integer[num_addresses];	

                for (int i = 0; i < num_addresses; i++) {
                    tp[i] = 0;
                }
            }

            absolte_time = System.currentTimeMillis();
            sender = new process264Real.block_process.ProcessBlock(fType);
            
            int divisor = videoInfo.GOP_num;
            
            int repeat = jComboBox1.getSelectedIndex() + 1;
            byte[] b;
            StructurePacket p = new StructurePacket();

            boolean st = true;
            
            for (int i = 0; i < repeat * divisor; i++) {

                
                if (i != 0 && (i % divisor == 0)) {
                    bb.clear();
                    st = true;
                }
                bb.get(source);    
                sender.setSource(source, Integer.parseInt(symbolSizeTextField.getText()));
                sender.setPlr(plr);

                
                al.get_g();
                
                coderate = sender.getCoderate();		

                sender.q = al.getVR(coderate);     
                System.out.println("coderate : " + coderate + "        sender.q : " + sender.q);




                series_coderate.add(Util.Tools.getTime(absolte_time), coderate);    
                series_vr.add(Util.Tools.getTime(absolte_time), sender.q);
                try {
                    sender.process(coderate);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                while (true) {
                    if (requested_gop + StructurePT.gop_approval < i) {	
                        try {
                            System.out.print("11requested_gop." + requested_gop);

                            Thread.sleep(1);		
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }


                
                int symbol_number = 0;
                
                p.gop_index = i;
                p.k = sender.k;
                p.symbol_size = sender.symbol_size;
                if (fType == FEC.common.TypeFEC.RS223) {
                    p.t = (int) (sender.k * 255.0 / 223.0);
                } else {
                    p.t = sender.t;
                }
                
                p.plr = sender.plr;

                while (true) {
                    p.packet_index = packet_stamp++;
                    p.fountain_id_start = symbol_number;
                    p.payload = java.util.Arrays.copyOfRange(sender.fountain_encoded, symbol_number, symbol_number + StructurePT.packetpayloadsize);
                    symbol_number += StructurePT.packetpayloadsize;
                    
                    if (p.packet_index % 100 == 0) {
                        p.pType = TypePacket.ping;
                        p.time = System.currentTimeMillis();
                    } else {
                        p.pType = TypePacket.general;
                    }

                    int target = wrr_distributor();

                    p.path_index = target;
                    b = Util.Tools.object_to_bytes(p);
                    
                    java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(b, b.length, paths[target].packet.getAddress(), paths[target].packet.getPort());
                    try {
                        socket.send(sendPacket);
                        if (st == true && i == 0) {
                            st = false;
                        }
                    } catch (java.io.IOException ex) {
                    }
                    
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    
                    if (symbol_number > sender.fountain_encoded.length) {
                        break;
                    }
                }

            } 
            
            p.gop_index = repeat * divisor;
            p.pType = TypePacket.last;
            byte[] data = Util.Tools.object_to_bytes(p);
            for (int i = 0; i < num_addresses; i++) {
                java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(data, data.length, paths[i].packet.getAddress(), paths[i].packet.getPort());
                for (int j = 0; j < StructurePT.dummy_packet; j++) {
                    try {
                        socket.send(sendPacket);
                    } catch (java.io.IOException ex) {
                    }
                }
            }
            reportSummary();
            reset();
        }
        
        private void reportSummary() {
            String result = "";
            double sum = 0;
            int count = series_vr.getItemCount();
            for (int i = 0; i < count; i++) {
                sum += series_vr.getY(i).doubleValue();
            }
            double avg = sum / count;
            result += "Average bit rate: " + Util.Tools.fixedWidthDoubletoString(avg, 5, 2);
            jTextArea1.setText(result);
        }
    }

    private void reset() {
        t_SendeInner.interrupt();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        init();
        start();
    }
    
    private javax.swing.JPanel Config;
    private javax.swing.JToolBar Controller;
    private javax.swing.JScrollPane Summary;
    private org.jfree.beans.JLineChart graph_CodeRate;
    private org.jfree.beans.JLineChart graph_Delays;
    private org.jfree.beans.JLineChart graph_Distributor;
    private org.jfree.beans.JLineChart graph_VR;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private java.awt.Label label1;
    private javax.swing.JTextField symbolSizeTextField;
    
}
