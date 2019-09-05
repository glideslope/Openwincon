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
package mcnl;

import com.google.common.collect.Lists;
//import javafx.application.Application;
//import jdk.nashorn.internal.ir.annotations.Reference;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.*;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.intent.*;

import org.onosproject.net.intent.constraint.ProtectedConstraint;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.intent.constraint.ProtectionConstraint.protection;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class SdnOffloadingAppComponent implements ForwardingMapService{

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference (cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference (cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference (cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private InternalHostListener hostListener = new InternalHostListener();
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ThreadPoolExecutor conExecutor ;

    private List<Host> hosts = Lists.newArrayList();
    private ApplicationId appId;

    Map<HostId, HostId> endPoints = null;

    int numOfContent = 4;
    int numOfNode = 4;
    int numOfPort = 5;

    String[][] OVS;


    private void initSetting()
    {
        // TODO
        // setting the MAC address and port number of OVSs

        OVS = new String[numOfNode+1][numOfPort+1];

        // (example) OVS[1][1] = "of:000000e04c36006f/1";
    }

    @Activate
    protected void activate() {
        log.info("Started");
        appId = coreService.registerApplication("mcnl-Offloading");
        hostService.addListener(hostListener);

        executor.scheduleAtFixedRate(this::trafficMonitoring, 1, 5, TimeUnit.SECONDS);
        conExecutor= (ThreadPoolExecutor)Executors.newCachedThreadPool();
        conExecutor.execute(new ConListener(this, 1622, conExecutor));

        initSetting();
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent hostEvent) {
            switch (hostEvent.type()) {
                case HOST_ADDED:
                    addHostConnectivity(hostEvent.subject());
                    hosts.add(hostEvent.subject());
                    break;
                case HOST_REMOVED:
                    break;
                case HOST_UPDATED:
                    break;
                case HOST_MOVED:
                    break;
            }
        }
    }

    public void addHostConnectivity(Host host) {
        for (Host dst : hosts) {
            HostToHostIntent intent = HostToHostIntent.builder().appId(appId).one(host.id()).two(dst.id()).build();
            intentService.submit(intent);
        }
    }

    public void addPointConnectivity(TrafficSelector selector,
                                     FilteredConnectPoint ingressPoint,
                                     FilteredConnectPoint egressPoint,
                                     int priority) {
        PointToPointIntent intent = PointToPointIntent.builder().appId(appId).filteredIngressPoint(ingressPoint).filteredEgressPoint(egressPoint).build();
        intentService.submit(intent);
    }




    private void trafficMonitoring(){

    }

    private void getMinTrafficPath(/*Device src, Device dst,String connectPoint*/int mRoute) {
        long maxRate=0;

        int[] route13_1 = {1,3};
        int[] route13_2 = {1,2,4,3};

        if( mRoute == 0 )
        {
            for(int i = 0;i<route13_1.length-1;i++){
                int src = route13_1[i];
                int dst = route13_1[i+1];
                long curRate = getRate(OVS[src][dst]);
                if(maxRate < curRate)
                    maxRate = curRate;
            }

        }
        else if( mRoute == 1 )
        {
            for(int i = 0;i<route13_2.length-1;i++){
                int src = route13_2[i];
                int dst = route13_2[i+1];
                long curRate = getRate(OVS[src][dst]);
                if(maxRate < curRate)
                    maxRate = curRate;
            }
        }


        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();


        // TODO
        // read the flow rule from .txt file
        // and parse it
        if( mRoute == 0)
        {
            // Set the Flow Rule
            // (example) setUpConnectivity(getConnectPoint("MAC addr and port num of OVS"), getConnectPoint("MAC addr and port num of OVS"), priority);
        }



        else if( mRoute == 1 )
        {
            // Set the Flow Rule
            // (example) setUpConnectivity(getConnectPoint("MAC addr and port num of OVS"), getConnectPoint("MAC addr and port num of OVS"), priority);
        }


    }

    private long getRate(String connectPoint)
    {
        StatisticService service = DefaultServiceDirectory.getService(StatisticService.class);
        DeviceId ingressDeviceId = deviceId(getDeviceId(connectPoint));
        PortNumber ingressPortNumber = portNumber(getPortNumber(connectPoint));
        ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);

        Load load = service.load(cp);
        return load.rate();
    }

    private ConnectPoint getConnectPoint(String connectPoint)
    {
        DeviceId ingressDeviceId = deviceId(getDeviceId(connectPoint));
        PortNumber ingressPortNumber = portNumber(getPortNumber(connectPoint));
        ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);
        return cp;
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private String getPortNumber(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(slash + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }


    private void setUpConnectivity(ConnectPoint ingress, ConnectPoint egress, int priority) {

        /*
         * priority type : int
         * resourceGroupId : string
         */
        String resourceGroupId = null;

        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        Intent intent = PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(ingress)
                .egressPoint(egress)
                .priority(priority)
                .build();
        intentService.submit(intent);
    }

    public void send(String message){
        // send message to agent ap
        byte[] buf = new byte[1200];


        InetAddress server = null;
        try {
            server = InetAddress.getByName("141.223.65.119"); // server IP address
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        buf = message.getBytes();


        DatagramPacket packet = new DatagramPacket(buf, buf.length, server, 6999);

        try {
            DatagramSocket socket = new DatagramSocket();

            socket.send(packet);
            socket.close();


        } catch (IOException e) {
            log.error("can not send udp message to agent: " + message);
            e.printStackTrace();
        }

    }

    @Override
    public Map<HostId, HostId> getEndPoints() {
        // Return our map as a read-only structure.
        //return Collections.unmodifiableMap(endPoints);
        return Collections.unmodifiableMap(endPoints);
    }

    @Override
    public void test(String connectPoint) {

        if(connectPoint.equals("3"))
            send("bash initialization.sh");
        else
            getMinTrafficPath(Integer.parseInt(connectPoint));

    }

}
