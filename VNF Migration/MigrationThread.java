package org.ntl.migration.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class MigrationThread extends Thread{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private HashMap<String, InetAddress> userList; //user list
    private HashMap<String, InetAddress> vnfList; //vnf list
    private HashMap<String, String> userVnfList; //vnf list of users
    private Boolean isActive = false;
    private DatagramSocket dSock;

    public MigrationThread(int port) {
        log.info("Thread started");
        try{
            isActive = false;
            this.dSock = new DatagramSocket(port);
            this.userList = new HashMap<>();
            this.vnfList = new HashMap<>();
            this.userVnfList = new HashMap<>();
            log.info("UDP Server is started");
        }catch(Exception e){
            log.error("Migration Thread initialization ERROR : " + e.getMessage());
        }
    }

    public void run(){
        try{
            byte[] buffer = new byte[1024];
            DatagramPacket rPack;
            InetAddress client;
            while(!isActive){
                rPack = new DatagramPacket(buffer, buffer.length);
                dSock.receive(rPack);
                String strIn = new String(rPack.getData(), 0, rPack.getLength());

                client = rPack.getAddress();
                //cPort = rPack.getPort();

                if(strIn.length() != 0){
                    String[] strSplit = strIn.split("-");
                    if(strSplit.length > 1){
                        log.info("Migration : " + client + " - " + strIn);
                        String type = strSplit[0];
                        String contents = strSplit[1];
                        switch(type){
                            case "new": //new user come
                                newUser(contents, client);
                                break;
                            case "all": //send all information to requested node
                                switch(strSplit[1]){
                                    case "user":
                                        udpMsg("all:user", "all", userList.toString(), client);
                                        break;
                                    case "vnf":
                                        udpMsg("all:vnf", "all", vnfList.toString(), client);
                                        break;
                                    case "use":
                                        udpMsg("all:use", "all", userVnfList.toString(), client);
                                    default:
                                        break;
                                }
                                break;
                            case "vnf": //add new vnf
                                newVnf(contents, client);
                                break;
                            case "use": //update userVnfList
                                if(contents.contains("/")){
                                    String[] userVnf = contents.split("/");
                                    userVnfList.put(userVnf[0], userVnf[1]);
                                }else{
                                    udpMsg("use", "err", "Error", client);
                                }
                                break;
                            case "update":
                                String[] userVnf = contents.split("@");
                                userList.put(userVnf[0], client);
                                newVnf(userVnf[1], client);
                                break;
                            default:
                                log.info("Migration Unknown msg : " + client + " - " + strIn);
                                break;
                        }
                    }else{
                        log.info("Migration Length Error : " + client + " - " + strIn);
                    }
                }
            }
        }catch(Exception e){
            log.error("Migration run ERROR : " + e.getMessage());
        }
    }

    @Override
    public void interrupt() {
        isActive = true;
        dSock.close();
        log.info("UDP Server is stopped");
        log.info("Thread interrupted");
        super.interrupt();
    }

    private void newUser(String userMac, InetAddress client){
        if (userList.containsKey(userMac)) {
            InetAddress nodeIP = userList.get(userMac);
            String vnf = userVnfList.get(userMac);
            udpMsg("newUser", "exist", userMac + nodeIP + "/" + vnf, client);
        } else {
            userList.put(userMac, client);
            udpMsg("newUser", "new", userMac, client);
        }
    }

    private void newVnf(String vnf, InetAddress client){
        vnfList.put(vnf, client);
        udpMsg("newVnf","done", vnf, client);
    }

    private void udpMsg(String funcName, String type, String contents, InetAddress client){
        try {
            String msg = type + "-" + contents;
            byte[] encodedMsg = msg.getBytes();
            DatagramPacket sPack = new DatagramPacket(encodedMsg, encodedMsg.length, client, 10987);
            dSock.send(sPack);
        }catch (Exception e){
            log.error(funcName + " - UDP send ERROR : " + e.getMessage());
        }
    }
}
