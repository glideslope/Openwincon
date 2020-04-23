import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Test {
	final static int CONST_PORT_RSSI = 20000;
	final static int CONST_PORT_CONTROL = 30000;
	
	final static int[] ARRAY_BITRATE = {50, 100, 150, 200, 250, 300, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400, 1450, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 8000};
	final static double[] PARA_PSNR[] = new double[][]{{5.5774, 1.72}, {5.6129, 2.1793}, {4.538, 11.865}};
	
	private static ArrayList<String> array_ue;
	private static ArrayList<String> array_ap;
	
	/* <UE name, UE class> */
	private static Map<String, UE> map_ue;
	
	/* <UE, rate> */
	private static Map<String, Integer> map_rate;
	
	/* <UE, Video> 	  *
	 * Bunny:		0 *
	 * Elephant:	1 *
	 * Sintel:		2 */
	private static Map<String, Integer> map_video;
	
	public static int getBandwidth(int rssi) {
		int bandwidth = (int) (2460.672 * (1 - Math.exp(-0.11 * (rssi + 81.7))));
		if (bandwidth <= 0)
			bandwidth = 1;
		return bandwidth;
	}
	
	public static double getPSNR(int bitrate, int video) {
		return PARA_PSNR[video][0] * Math.log(bitrate) + PARA_PSNR[video][1];
	}
	
	public static void main(String[] args) {
		new ThreadRSSI().start();
		new ThreadControl().start();
		
		array_ue = new ArrayList<String>();
		array_ap = new ArrayList<String>();
		
		map_ue = new HashMap<String, UE>();
		map_rate = new HashMap<String, Integer>();
		map_video = new HashMap<String, Integer>();
		
		System.out.println(getBandwidth(-60));
		System.out.println(getPSNR(1000, 1));
	}
	
	public static class UE{
		
		/* <AP, RSSI> */
		private Map<String, Integer> map_rssi;
		
		/* <AP, x ratio> */
		private Map<String, Double> map_ratio;
		
		public UE() {
			map_rssi = new HashMap<String, Integer>();
			map_ratio = new HashMap<String, Double>();
		}
		
		public int getRSSI(String ap) {
			return map_rssi.get(ap);
		}
		
		public void setRSSI(String ap, int rssi) {
			map_rssi.put(ap, rssi);
		}
		
		public double getRatio(String ap) {
			return map_ratio.get(ap);
		}
		
		public void setRatio(String ap, double ratio) {
			map_ratio.put(ap, ratio);
		}
	}
	
	public static class ThreadRSSI extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				ServerSocket socket_server = null;
				try {
					socket_server = new ServerSocket(CONST_PORT_RSSI);
					Socket socket_client = socket_server.accept();
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket_client.getInputStream(), "UTF-8"));
					
					String str_recv = reader.readLine();
					String array_data[] = str_recv.split("/");
					String str_ap = array_data[0];
					
					int len_data = array_data.length;
					System.out.println("AP: " + str_ap);
					for (int i = 1; i < len_data; i++) {
						String array_client[] = array_data[i].split(",");
						System.out.println(array_client[0] + " " + array_client[1]);
					}
					
					/* 임시로 넣음 - RSSI 로그 */
					PrintWriter pw = null;
			        try {
			            pw = new PrintWriter(new FileWriter("test.txt", true));
			            pw.write(str_recv + "\n");
			            pw.close();
			            
			        }catch(Exception e) {
			        	e.printStackTrace();
			        }
					
					reader.close();
					socket_client.close();
					socket_server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	

	public static class ThreadControl extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				ServerSocket socket_server = null;
				try {
					socket_server = new ServerSocket(CONST_PORT_CONTROL);
					Socket socket_client = socket_server.accept();

					BufferedReader reader = new BufferedReader(new InputStreamReader(socket_client.getInputStream(), "UTF-8"));
					String str_bitrate_origin = reader.readLine();
					System.out.println(str_bitrate_origin);
					
					socket_client.getOutputStream().write("200K/0.6".getBytes());

					reader.close();
					socket_client.close();
					socket_server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
