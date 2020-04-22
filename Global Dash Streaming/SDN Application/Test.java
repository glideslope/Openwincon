import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Test {
	final static int CONST_PORT_RSSI = 20000;
	final static int CONST_PORT_CONTROL = 30000;
	
	public static void main(String[] args) {
		new ThreadRSSI().start();
		new ThreadControl().start();
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
					
					socket_client.getOutputStream().write("300K/0.6".getBytes());
					
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
