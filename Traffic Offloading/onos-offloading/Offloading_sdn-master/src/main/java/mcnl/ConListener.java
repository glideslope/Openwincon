package mcnl;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ConListener implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    // Message types
    private final String MSG_CLIENT_INFO = "client";
    private final String MSG_CLIENT_DISCONNECT = "clientdisconnect";
    private final String MSG_CLIENT_SCAN = "scan";

    private final int SERVER_PORT;
    InetAddress ClientIP;
    int clientPort;
    private DatagramSocket controllerSocket;
    private final ExecutorService executor;
    private final SdnOffloadingAppComponent master;
    boolean ServerStarted;


    public ConListener (SdnOffloadingAppComponent m, int port, ExecutorService executor) {
        this.master = m;
        this.SERVER_PORT = port;
        this.executor = executor;
        ServerStarted=true;
        // this.controllerSocket=controllerSocket;

    }
    /*    public void finalize()
        {

        }
    */
    @Override
    public void run() {
        try {
            controllerSocket = new DatagramSocket(SERVER_PORT);
        } catch (IOException e) {
            log.info("create new controllerSocket fail: " + SERVER_PORT);
            e.printStackTrace();
        }

        log.info("--------------Start APManger--------------");
        while(ServerStarted/*!Thread.interrupted()*/){
            try {
                final byte[] receiveData = new byte[1280]; // probably this could be smaller
                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                controllerSocket.receive(receivedPacket);
                log.info("packet receive from ");
                log.info("packet receive from "+ receivedPacket.getAddress());

                executor.execute(new ConnectionHandler(receivedPacket));
                receivedPacket.getAddress();
            }
            catch (Exception e) {

                log.info("controllerSocket.accept() failed: " + SERVER_PORT);
                ServerStarted=false;

                //e.printStackTrace();

            }
        }
        controllerSocket.close();
    }

    public void endConnection()
    {
        ServerStarted=false;
    }


    /** Protocol handlers **/
    /*
    receiveClientRequest :  request + client + url
     */


    private void receiveClientInfo(final InetAddress agentAddr,
                                   final int agentPort, final String clientMacAddr, final String ConnectedAP) {
        //master.receiveClientInfo(agentAddr, agentPort, clientMacAddr, ConnectedAP, controllerSocket);
    }


    private void clientDisconnect(final InetAddress agentAddr,
                                  final String clientEthAddr) {
        //master.clientDisconnect(agentAddr, clientEthAddr);
    }

    private void receiveScanResult(String[] fields) {
        //master.receiveScanResult(fields);
    }

    private class ConnectionHandler implements Runnable {
        final DatagramPacket receivedPacket;

        public ConnectionHandler(final DatagramPacket dp) {
            receivedPacket = dp;
        }

        // AP Agent message handler
        public void run() {
            final String msg = new String(receivedPacket.getData()).trim().toLowerCase();
            final String[] fields = msg.split("\\|");
            final String msg_type = fields[0];
            final InetAddress agentAddr = receivedPacket.getAddress();
            log.info("handler get data"+msg);
            if (msg_type.equals(MSG_CLIENT_INFO)) {
                final int agentPort = receivedPacket.getPort() /*fields[1]*/;
                final String clientMacAddr = fields[1];
                String clientIP = fields[2];
                String ConnectedAP = fields[3];
                log.info("find public ap/private ap(0)="+agentAddr+", "+fields[2]);

                String[] ipfields = clientIP.split("\\.");
                String[] ipAPfields = agentAddr.getHostAddress().split("\\.");
                log.info("find public ap/private ap="+ipfields[1]);
                if(ipAPfields[0].equals("/192"))// && ipfields[1].equals("168") && ipfields[2].equals("100"))
                {
                    InetAddress clip = null;
                    try {
                        clip = InetAddress.getByName(clientIP);
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    receiveClientInfo(clip, agentPort, clientMacAddr,ConnectedAP);
                }
                else
                    receiveClientInfo(agentAddr, agentPort, clientMacAddr,ConnectedAP);
                log.info("receive client info");

            } else if (msg_type.equals(MSG_CLIENT_DISCONNECT)) {
                final String clientEthAddr = fields[1];
                clientDisconnect(agentAddr, clientEthAddr);

            } else if (msg_type.equals(MSG_CLIENT_SCAN)) {
                log.info("receive AP singnal from "+ fields[1]);

                System.out.println("\n");
                for (int i = 0; i < fields.length; i++) {
                    String[] info = fields[i].split("&");
                    log.info("info length: "+ info.length);
                    if(info.length>=2&&!info[0].equals("")){
                        String ssid = info[0];
                        String bssid = info[1].toLowerCase();
                        int level = Integer.parseInt(info[2]);
                        System.out.println("SSID: "+ ssid+"\tBSSID: "+bssid+"\tSignalStrength:"+level);
                    }

                }
                receiveScanResult(fields);
            }

        }
    }


}