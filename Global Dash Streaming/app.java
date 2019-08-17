/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.ac.postech;

import org.apache.felix.scr.annotations.*;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    protected DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
    protected FlowRuleService flowRuleService = DefaultServiceDirectory.getService(FlowRuleService.class);

    private final int PRIORITY_DEVICE = 10;

    private final int UNIT_T = 5;
    private static final int UNIT_SECOND = 1000;
    private final int UNIT_K = 1024;
    private final int UNIT_B = 8;

    private static final int NUM_PORT = 7777;

    /* estimated rate */
    private HashMap<String, Long> hash_est_rate;
    /* estimated connection */
    private static HashMap<String, String> hash_est_connect;
    /* estimated AP */
    private static HashMap<String, Integer> hash_est_ap;

    /* control rate */
    private static HashMap<String, Integer> hash_ctrl_rate;
    /* control connection */
    private static HashMap<String, String> hash_ctrl_connect;
    /* control AP */
    private static HashMap<String, Integer> hash_ctrl_ap;



    private static long test_start;

    @Activate
    protected void activate() {

        test_start = System.currentTimeMillis();

        Iterable<Device> devices = deviceService.getDevices();
        hash_est_rate = new HashMap<>();
        hash_est_connect = new HashMap<>();
        hash_est_ap =  new HashMap<>();

        hash_ctrl_rate = new HashMap<>();
        hash_ctrl_connect = new HashMap<>();

        for (Device device : devices) {
            try {

                for (PortStatistics statistics : deviceService.getPortDeltaStatistics(device.id())) {
                    hash_est_rate.put(device.id().toString(), new Long(statistics.bytesSent()));
                    hash_ctrl_rate.put(device.id().toString(), new Integer(2000));
                }

            }catch(java.lang.ArrayIndexOutOfBoundsException e){
                // ignore
            }
        }

        try {
            Thread.sleep(UNIT_T * UNIT_SECOND);
        } catch (InterruptedException e) {
            // ignore
        }

        MSGThread thread = new MSGThread();
        thread.start();

        ScheduledExecutorService executor_monitor = Executors.newSingleThreadScheduledExecutor();
        executor_monitor.scheduleAtFixedRate(this::monitorTraffic, 1, UNIT_T, TimeUnit.SECONDS);
        
    }

    @Deactivate
    protected void deactivate() {
    }

    private void monitorTraffic() {
        Iterable<Device> devices = deviceService.getDevices();
        String msg_rate = "";
        String str_ue = "";
        for (Device device : devices) {

            try {
                System.out.println(device.id().toString());
                for(FlowEntry flow: flowRuleService.getFlowEntries(device.id())){
                    if(flow.priority() == PRIORITY_DEVICE){
                        Iterator<Criterion> iter = flow.selector().criteria().iterator();
                        while(iter.hasNext()){
                            str_ue = iter.next().toString();
                            if(str_ue.contains("ETH_DST")) {
                                str_ue = str_ue.replace("ETH_DST", "");
                                str_ue = str_ue.replace(":", "");
                                str_ue = "of:0000" + str_ue.toLowerCase();
                                hash_est_connect.put(str_ue, device.id().toString());

                                //System.out.println(device.id().toString() + " of:0000" + str_ue);

                            }
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            hash_est_ap.clear();
            for(String ue: hash_est_connect.keySet()){
                String ap = hash_est_connect.get(ue);
                if(hash_est_ap.containsKey(ap) == false)
                    hash_est_ap.put(ap, 1);
                else
                    hash_est_ap.put(ap, hash_est_ap.get(ap) + 1);
            }

            int int_ap = hash_est_ap.size();
            int int_ue = hash_est_connect.size();

            int int_balance = int_ue / int_ap;
            int int_remain = int_ue % int_ap;

            hash_ctrl_connect.clear();
            hash_ctrl_ap.clear();
            for(String ue: hash_est_connect.keySet()) {
                String ap = hash_est_connect.get(ue);

                if(hash_ctrl_ap.containsKey(ap) == false) {
                    hash_ctrl_ap.put(ap, 1);
                    hash_ctrl_connect.put(ue, ap);
                }
                else if(hash_ctrl_ap.get(ap) == int_balance && int_remain > 0) {
                    int_remain--;
                    hash_ctrl_ap.put(ap, hash_ctrl_ap.get(ap) + 1);
                    hash_ctrl_connect.put(ue, ap);
                }
                else {
                    if(hash_ctrl_ap.get(ap) < int_balance) {
                        hash_ctrl_ap.put(ap, hash_ctrl_ap.get(ap) + 1);
                        hash_ctrl_connect.put(ue, ap);
                    }else {
                        for(String temp_ap: hash_est_ap.keySet()) {
                            if(hash_est_ap.get(temp_ap) < int_balance) {
                                if(hash_ctrl_ap.containsKey(ap) == false) {
                                    hash_ctrl_ap.put(temp_ap, 1);
                                    hash_ctrl_connect.put(ue, temp_ap);
                                    break;
                                }else if(hash_ctrl_ap.get(temp_ap) < int_balance){
                                    hash_ctrl_ap.put(temp_ap, hash_ctrl_ap.get(temp_ap) + 1);
                                    hash_ctrl_connect.put(ue, temp_ap);
                                    break;
                                }
                                else if(hash_ctrl_ap.get(temp_ap) == int_balance && int_remain > 0) {
                                    int_remain--;
                                    hash_ctrl_ap.put(temp_ap, hash_ctrl_ap.get(temp_ap) + 1);
                                    hash_ctrl_connect.put(ue, temp_ap);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static class MSGThread extends Thread {
        private Socket client;
        PrintWriter writer;
        String msg;

        public void run() {
            ServerSocket server;
            try {
                server = new ServerSocket(NUM_PORT);
                while (true) {
                    if(hash_est_ap.size() == 0)
                        continue;
                    try {
                        client = server.accept();
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
