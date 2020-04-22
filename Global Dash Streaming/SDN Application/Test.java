import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Test {
	final static int CONST_PORT = 20000;
	
	public static void main(String[] args) {
		
		while(true) {
			ServerSocket server = null;
			try {
				server = new ServerSocket(CONST_PORT);
				Socket client = server.accept();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
				
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
				client.close();
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}