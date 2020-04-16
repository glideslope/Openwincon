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

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////      algorithm       ///////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    static final int START_TIME = 10;
    static final double KAPPA = 0.0018;	
    static final double MAX_RATE_LTE[] = {10 * 1024 , 20 * 1024 };	// 20 Mbps, 20 Kbps SIMULATION
    static final int PATH_MONITORING_DURATION = 1; // 4s		// auction monitoring duration
    static final int N_UE = 25; // the total number of UEs

    static double omega = 0.01;

    static Vector<Player> players = new Vector<Player>();

//    static NetDeviceContainer ueDevices; // TODO: from ns3
    static int [] ueCellid = {2, 1, 2, 1, 1, 2, 2, 1, 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 2, 2, 2, 1, 2, 1};

    static double targetQual = 0;

    static double k = 1024; // k = 10 * 1024; // R&R revision : 10

    private void initSetting()
    {
        for(int i = 0; i < N_UE; i++) {
            Player player = new Player(i);
            players.add(player);
        }

        /* enrollProxyOVS */
        OVS = new String[numOfNode+1][numOfPort+1];
        // TODO:
        // setting the MAC address and port number of OVSs
        //ContentArray = new String[numOfContent+1][numOfNode+1];

        //setUpConnectivity(getConnectPoint(OVS[1][1]),getConnectPoint(OVS[1][2]), 50);
        //setUpConnectivity(getConnectPoint(OVS[1][2]),getConnectPoint(OVS[1][1]), 50);

        //setUpConnectivity(getConnectPoint(OVS[2][1]),getConnectPoint(OVS[2][2]), 50);
        //setUpConnectivity(getConnectPoint(OVS[2][2]),getConnectPoint(OVS[2][1]), 50);

        //setUpConnectivity(getConnectPoint(OVS[2][1]),getConnectPoint(OVS[2][2]), 50);
        //setUpConnectivity(getConnectPoint(OVS[2][2]),getConnectPoint(OVS[2][1]), 50);
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

    private void allocateResource(short cell){
        ///////////////////////////////////////////////////////////////////////////////
        //////////////////                 algorithm                ///////////////////
        ///////////////////////////////////////////////////////////////////////////////
        //  UE's cell location check

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);

//            Ptr<LteUeNetDevice> ueLteDevice = ueDevices.Get(i)->GetObject<LteUeNetDevice> ();
//            Ptr<LteUeRrc> ueRrc = ueLteDevice->GetRrc();
//
//            if (ueRrc.GetCellId() - 1 == cell) { // cell id start from 1
//                if(cell == 0)
//                    NS_LOG_FUNCTION("id" << it.getID() << it.getUEAddr() << " belongs to eNB " << cell); //TODO: getUEADDR is used only once...
//                it.setCellID(cell);
//            }
//            else {
//                it.setCellID((cell + 1) % 2); // 0->1, 1->0
//            }
            if(ueCellid[i] - 1 == cell)
                it.setCellID(cell);
            else
                it.setCellID((short)((cell + 1) % 2)); // 0->1, 1->0
        }

        int gopIndex;

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (cell == 0 && it.getCellID() == cell) {
                gopIndex = (int) (System.currentTimeMillis()/1000 - START_TIME + it.getJoinTime()) % 60;
//                std::cout << "USER ID : " << it.getID() << " PSNR : " << it.ratetoPSNR(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000)), gopIndex) << std::endl;
            }
        }

        //  Algorithm 2
        targetQual = 0;
        double minQual = 0;
        double maxQual = 0;
        double newQual = 0;
        double rQual = 0; // resource

        // get max PSNR
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            gopIndex = (int) (System.currentTimeMillis()/1000 - START_TIME + it.getJoinTime()) % 60;
            if (it.ratetoPSNR((it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) + MAX_RATE_LTE[cell]), gopIndex) >= maxQual)
                maxQual = it.ratetoPSNR((it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) + MAX_RATE_LTE[cell]), gopIndex);
        }

        // determine targetQual (newQual)
        int iter = 0;
        do {
            rQual = 0;
            newQual = (minQual + maxQual)/2;

            if(cell == 0)
//                NS_LOG_FUNCTION("New Quality : " << newQual);

                for (int i=0; i<players.size(); i++) {
                    Player it = players.get(i);
                    if (it.getCellID() == cell) {
                        gopIndex = (int) (System.currentTimeMillis()/1000 - START_TIME + it.getJoinTime()) % 60;
                        if (newQual > it.ratetoPSNR(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000), gopIndex)) {
                            double coef = (newQual-it.ratetoPSNR(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000), gopIndex)) / (10.0 * it.getBETA(gopIndex)*(1.0 + KAPPA));
                            rQual += it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) * ((1.0 / Math.pow(10, coef)) -1 );
                            //NS_LOG_FUNCTION( "ID : " << it.getID() << ",  #### Required Resources : " <<  rQual);
                        }
                    }
                }
            //NS_LOG_FUNCTION("@@@@ Required Resources : " << rQual << ", @@@@ MAX_RATE_LTE : " <<  MAX_RATE_LTE[cell]);
//            if(cell == 0)
//                std::cout << ":: ALGORITM 1 :: " << " Stage : " << iter <<  " , NewQuality : " << newQual << " , Resource : " << rQual << std::endl;

            if(rQual < MAX_RATE_LTE[cell])
                minQual = newQual;
            else
                maxQual = newQual;
            iter ++;
        } while (iter !=20 );//(abs(MAX_RATE_LTE[cell] - rQual) / MAX_RATE_LTE[cell] > ebsilon); //(iter !=20 );  //(abs(MAX_RATE_LTE[cell] - rQual) / MAX_RATE_LTE[cell] > ebsilon);

        targetQual = newQual;

//        if(cell == 0){
//            std::cout << ":: ALGORITM 1 :: " << " Target video quality level : " << targetQual << std::endl << std::endl;
//            NS_LOG_FUNCTION("Cell ID : " << cell << ",  Target video quality level : " << targetQual);
//        }

        // bargaining power for video quality fairness
        double totalQualBp = 0;
        double myQualBp = 0;

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell){
                gopIndex = (int) (System.currentTimeMillis()/1000 - START_TIME + it.getJoinTime()) % 60;
                double denomiator = it.getBETA(gopIndex)*(1+KAPPA);
                double onlyAP_PSNR = it.ratetoPSNR(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000), gopIndex);
                totalQualBp += (it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000)/denomiator)
                        * Math.pow(10, (onlyAP_PSNR-targetQual)/(10*denomiator));
            }
        }

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell){
                gopIndex = (int) (System.currentTimeMillis()/1000- START_TIME + it.getJoinTime()) % 60;
                double myDenomiator = it.getBETA(gopIndex)*(1+KAPPA);
                double myOnlyAP_PSNR = it.ratetoPSNR(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000), gopIndex);
                myQualBp = (it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000)/myDenomiator) * Math.pow(10, (myOnlyAP_PSNR-targetQual)/(10*myDenomiator));
                it.setQualBP(myQualBp/totalQualBp);
            }
        }

        if(cell == 0)
            System.out.println();

        // check total sum 1
        double sum = 0;
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                sum += it.getQualBP();
//                if(cell == 0){
//                    std::cout << ":: BP for Qual :: " << " USER ID : " << it.getID() << " , BP : " << it.getQualBP() << std::endl;
//                    NS_LOG_FUNCTION("Quality Bargaining Power : " << it.getQualBP());
//                }
            }
        }
//        if(cell == 0)
//            std::cout <<  std::endl;

        // bargaining power for highest social welfare
        int numOfUsers = 0;
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                numOfUsers ++;
            }
        }
//        if(cell == 0)
//            NS_LOG_FUNCTION("Total Number of Users : " << numOfUsers);

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                it.setSocialBP(1.0/numOfUsers);
//                if(cell == 0) {
//                    std::cout << ":: BP for Social :: " << " USER ID : " << it.getID() << " , BP : " << it.getSocialBP() << std::endl;
//                    NS_LOG_FUNCTION("Social Bargaining Power : " << it.getSocialBP());
//                }
            }
        }
        // bargaining power (omega) control
        //omega = 1 / (1 + exp( C1 * (targetQual - C2)));
        omega = 0;

        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                it.setBP(omega);
            }
        }
        // check whether total sum of BP is one
        sum = 0;
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                sum += it.getBP();
            }
        }
        //NS_LOG_FUNCTION("Total Sum of Bargaining Power : " << sum);

//        if(cell == 0)
//            NS_LOG_FUNCTION("@@@@@@@@@@@@@@@ Radio Resource allocation @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");


        // Sequential-Splitting

        double m = 0;
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                it.setMacroRsc(0);
            }
        }

        int j = players.get(0).getID();
        double temp, minimum;
        gopIndex = (int) System.currentTimeMillis()/1000;
        minimum = 0;
        //std::sort(players.begin(), players.end(), cmp);
        while (k != m) {
            for (int i=0; i<players.size(); i++) {
                Player it = players.get(i);
                if (it.getCellID() == cell) {
                    if(it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) >= minimum &&
                            it.ratetoPSNR((it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) + it.getMacroRsc()), gopIndex) < 35){
                        temp = it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000);
                        j = it.getID();
                    }
                }
            }

            for (int i=0; i<players.size(); i++) {
                Player it = players.get(i);
                if (it.getID() == j) {
                    double rsc = it.getMacroRsc() + MAX_RATE_LTE[cell] / k;
                    it.setMacroRsc(rsc);
                    break;
                }
            }
            m++;
        }

        // check the amount of radio resource
        double total_sum_rsc = 0;
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell) {
                total_sum_rsc += it.getMacroRsc();
                if(cell == 0){
                    gopIndex = (int) (System.currentTimeMillis()/1000 -START_TIME + it.getJoinTime()) % 60;
//                    std::cout <<  " USER ID : " << it.getID() << ", Small Cell Resource : " << it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000)
//						<< " , Small Cell PSNR : " << it.ratetoPSNR((it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000)), gopIndex)
//						<< " , Macro Resource : " << it.getMacroRsc()
//                            << ", With Macro PSNR : " << it.ratetoPSNR((it.getSmallRsc(it.getID(), (int) System.currentTimeMillis()/1000) + it.getMacroRsc()), gopIndex) << std::endl;
//                    NS_LOG_FUNCTION("User ID : " << it.getID() << ", resource : " << it.getMacroRsc());
                }
            }
        }


//        if(cell == 0) {
//            std::cout << ":: Total Amount of Macro Resource :: " << total_sum_rsc << std::endl;
//            NS_LOG_FUNCTION("Total amount of allocated resources  : " << total_sum_rsc);
//        }

        //currentRound[cell] = 0;
        //transmitVideo(round, cell);
        for (int i=0; i<players.size(); i++) {
            Player it = players.get(i);
            if (it.getCellID() == cell)
                it.initialize();
        }
//        Simulator::Schedule(Seconds(PATH_MONITORING_DURATION), &bargaining, cell); // start time, gopIndex, round // TODO: apply resource allocation

        ///////////////////////////////////////////////////////////////////////////////
        //////////////////            end of algorithm              ///////////////////
        ///////////////////////////////////////////////////////////////////////////////
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


        // TODO:
        // read the flow rule from .txt file
        // and parse it
        if( mRoute == 0)

        {
            // Set the Flow Rule
            // (example) setUpConnectivity(getConnectPoint("MAC addr and port num of OVS"), getConnectPoint("MAC addr and port num of OVS"), priority);

            // setUpConnectivity(getConnectPoint(OVS[3][3]),getConnectPoint(OVS[3][1]), 50);
            // setUpConnectivity(getConnectPoint(OVS[3][1]),getConnectPoint(OVS[3][3]), 50);

            // setUpConnectivity(getConnectPoint(OVS[1][3]),getConnectPoint(OVS[1][2]), 50);
            // setUpConnectivity(getConnectPoint(OVS[1][2]),getConnectPoint(OVS[1][3]), 50);

            // setUpConnectivity(getConnectPoint(OVS[2][1]),getConnectPoint(OVS[2][2]), 50);
            // setUpConnectivity(getConnectPoint(OVS[2][2]),getConnectPoint(OVS[2][1]), 50);
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
        else {
            getMinTrafficPath(Integer.parseInt(connectPoint));
            allocateResource((short) 0); // test
            allocateResource((short) 1);
        }

    }

}
