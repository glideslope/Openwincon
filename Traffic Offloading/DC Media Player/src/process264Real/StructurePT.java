package process264Real;

public class StructurePT {
    static final int server_port = 50003;     
    static final int packetsize = 1024;    
    public static final int packetpayloadsize = 512;       
    static final String laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    static final int gop_approval = 0;      
    static final int dummy_packet = 3;     
 
    static final int delay_constraint = 500;      
    static final int delay = 1;        
    static final int interframetime = 40;      
   
    static final int timeout = 1;           
    static final boolean is_blink = false;      
    static final int blink_interval = 50;       
    static final int receiver_buffer_size = packetsize * 20;    
    static final int initial_bandwidth = 1000 * 1000;          
    static final int initial_bandwidth_step = 1000 * 1000;       
    static final String repository_video_sequence = "c:/testdata/";    
    public static final double blr_constraint = 0.01;     
    public static final double coderate_max = 0.9;     
    public static final double coderate_min = 0.2;      
    public static final double coderate_step = 0.01;    
   
    public static final int symbolSize = 64;

    public static String file = "c:/testdata/output.yuv";  
    public static String TFile = "c:/testdata/sample.yuv";  

    public static int QCIF = 0;        
    public static int CIF = 1;         
    public static int FCIF = 2;          
    public static int width = 704;     
    public static int height = 576;    
    public static int framerate = 25;  
    public static int frameperGOP = 25; 
    public static int YUV_SIZE = FCIF;   
    public static String original = "sample.yuv";

    public static final int bandwidth_max_eth0 = 12 * 1000 * 1000;		
    public static final int bandwidth_min_eth0 = (int) (2.7 * 1000 * 1000);
    public static final int bandwidth_max_eth1 = 5 * 1000 * 1000;
    public static final int bandwidth_min_eth1 = (int) (1.7 * 1000 * 1000);
    public static final int bandwidth_max_eth = 11 * 1000 * 1000;
    public static final int bandwidth_min_eth = 2 * 1000 * 1000;
    public static final int bandwidth_max_ppp = (int) (14.4 * 1000 * 1000);
    public static final int bandwidth_min_ppp = (int) (1.8 * 1000 * 1000);
    public static final int bandwidth_max_others = (int) (14.4 * 1000 * 1000);
    public static final int bandwidth_min_others = (int) (1.8 * 1000 * 1000);

    public static final int ethernetCost = 0;
    public static final int WiFiCost = 0;
    public static final int LTEBasicCost = 13000;    
    public static final int LTEBasicData = 700 * 1000 * 1000;   

    public static final int EthernetStartData=0;
    public static final int WiFiStartData = 0;    
    public static final int LTEStartData = 0;    

    public static final int Ethernet = 0;
    public static final int WiFi = 1;
    public static final int LTE = 2;
    public static final int HSDPA = 3;


    public static final int VideoEncodingRate[] = {200,300,400,500,600,700,800,900};
    public static final double Distortion[] = {57.11,43.73,36.18,31.24,27.70,25.03,22.92,21.21};

    public static double D_Max = 57.11;
    public static double D_Min = 21.21;

    public static double ALPHA =0.5;


    public static double GAMMA = 1.7674;
    public static double EPSILON = -0.65848;
}
