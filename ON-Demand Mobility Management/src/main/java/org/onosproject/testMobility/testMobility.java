
package org.onosproject.testMobility;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IPacket;

//TODO
import org.onosproject.testBCE.TestBCE;
import org.onosproject.testPrefix.TestPrefix;

import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
//import org.onosproject.net.topology.impl.PathManager;


//import java.awt.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import java.nio.ByteBuffer;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Skeletal ONOS application component.
 */

@Component(immediate = true)
public class testMobility {
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_PRIORITY = 10;
    public static final byte HEADER_LENGTH = 12; // bytes

    private final Logger log = getLogger(getClass());
   
    private Ip6Prefix prefix1;
    private Ip6Prefix prefix2;
    private ArrayList <ArrayList<String>> flowPrefix1 = new ArrayList <ArrayList<String>>();
    private ArrayList <ArrayList<String>> flowPrefix2 = new ArrayList <ArrayList<String>>();
 

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    private ApplicationId appId;

    @Property(name = "packetOutOnly", boolValue = false,
            label = "Enable packet-out only forwarding; default is false")
    private boolean packetOutOnly = false;

    @Property(name = "packetOutOfppTable", boolValue = false,
            label = "Enable first packet forwarding using OFPP_TABLE port " +
                    "instead of PacketOut with actual port; default is false")
    private boolean packetOutOfppTable = false;

    @Property(name = "flowTimeout", intValue = DEFAULT_TIMEOUT,
            label = "Configure Flow Timeout for installed flow rules; " +
                    "default is 10 sec")
    private int flowTimeout = DEFAULT_TIMEOUT;

    @Property(name = "flowPriority", intValue = DEFAULT_PRIORITY,
            label = "Configure Flow Priority for installed flow rules; " +
                    "default is 10")
    private int flowPriority = DEFAULT_PRIORITY;

    @Property(name = "ipv6Forwarding", boolValue = true,
            label = "Enable IPv6 forwarding; default is false")
    private boolean ipv6Forwarding = true;

    @Property(name = "matchDstMacOnly", boolValue = false,
            label = "Enable matching Dst Mac Only; default is false")
    private boolean matchDstMacOnly = false;

    @Property(name = "matchVlanId", boolValue = false,
            label = "Enable matching Vlan ID; default is false")
    private boolean matchVlanId = false;

    @Property(name = "matchIpv4Address", boolValue = false,
            label = "Enable matching IPv4 Addresses; default is false")
    private boolean matchIpv4Address = false;

    @Property(name = "matchIpv4Dscp", boolValue = false,
            label = "Enable matching IPv4 DSCP and ECN; default is false")
    private boolean matchIpv4Dscp = false;

    @Property(name = "matchIpv6Address", boolValue = true,
            label = "Enable matching IPv6 Addresses; default is false")
    private boolean matchIpv6Address = true;

    @Property(name = "matchIpv6FlowLabel", boolValue = false,
            label = "Enable matching IPv6 FlowLabel; default is false")
    private boolean matchIpv6FlowLabel = false;

    @Property(name = "matchTcpUdpPorts", boolValue = false,
            label = "Enable matching TCP/UDP ports; default is false")
    private boolean matchTcpUdpPorts = false;

    @Property(name = "matchIcmpFields", boolValue = false,
            label = "Enable matching ICMPv4 and ICMPv6 fields; " +
                    "default is false")
    private boolean matchIcmpFields = false;

    @Property(name = "ignoreIPv4Multicast", boolValue = false,
            label = "Ignore (do not forward) IPv4 multicast packets; default is false")
    private boolean ignoreIpv4McastPackets = false;
   
    private TestBCE  mobilityServer;

    private int flowNum =0;
    private int stype=0;
    private int cnt_1 =0;
    private int cnt_2 =0;
    //private DeviceId preApId;
    private String stPreApId = "of:0000000000000001";
    private String stNowApId = "";


//checking preApId =of:0000000000000001, nowApId=of:0000000000000001

    private MacAddress getHostid;
    private DeviceId getApid; 
    private Ip6Prefix getIp6Prefix1;
    private Ip6Prefix getIp6Prefix2;
    private ArrayList <ArrayList<String>> getFlowPrefix1;
    private ArrayList <ArrayList<String>> getFlowPrefix2;
    private String type1, type2;
    private ArrayList destList1 = new ArrayList();
    private ArrayList destList2 = new ArrayList();
    private ArrayList desApList1 = new ArrayList();
    private ArrayList desApList2 = new ArrayList();  
	    
 
//    private final TopologyListener topologyListener = new InternalTopologyListener();

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

   @Activate
    public void activate() {
       cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.test1");
	packetService.addProcessor(processor, PacketProcessor.director(2));
//	topologyService.addListener(topologyListener);
	requestIntercepts();
        mobilityServer.createmobilityServerMap();
        mobilityServer.clearmobilityServerEntry();
        log.info("*****Hello ONOS");
        log.info("Started with Application ID{}",appId.id());
    }


    @Deactivate
    public void deactivate() {
	cfgService.unregisterProperties(getClass(), false);
	withdrawIntercepts();
	packetService.removeProcessor(processor);
//	topologyService.removeListener(topologyListener);
	 mobilityServer.clearmobilityServerEntry();
        log.info("*****ByeBye ONOS");
	processor = null;
    }


    @Modified
    public boolean modified(ComponentContext context) {
        log.info("context={}", context);
	if(context == null){
	log.info("NONONONONO");
	return false;
	}
	
	readComponentConfiguration(context);
        requestIntercepts();
	return true;
    }



   /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        selector.matchEthType(Ethernet.TYPE_IPV6);
        if (ipv6Forwarding) {
            packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
        } else {
            packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        }
    }

   /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_IPV6);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

 /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        boolean packetOutOnlyEnabled =
                isPropertyEnabled(properties, "packetOutOnly");
        if (packetOutOnly != packetOutOnlyEnabled) {
            packetOutOnly = packetOutOnlyEnabled;
            log.info("Configured. Packet-out only forwarding is {}",

                     packetOutOnly ? "enabled" : "disabled");
        }
        boolean packetOutOfppTableEnabled =
                isPropertyEnabled(properties, "packetOutOfppTable");
        if (packetOutOfppTable != packetOutOfppTableEnabled) {
            packetOutOfppTable = packetOutOfppTableEnabled;
            log.info("Configured. Forwarding using OFPP_TABLE port is {}",
                     packetOutOfppTable ? "enabled" : "disabled");
        }
        boolean ipv6ForwardingEnabled =
                isPropertyEnabled(properties, "ipv6Forwarding");
        if (ipv6Forwarding != ipv6ForwardingEnabled) {
            ipv6Forwarding = ipv6ForwardingEnabled;
            log.info("Configured. IPv6 forwarding is {}",
                     ipv6Forwarding ? "enabled" : "disabled");
        }
        boolean matchDstMacOnlyEnabled =
                isPropertyEnabled(properties, "matchDstMacOnly");
        if (matchDstMacOnly != matchDstMacOnlyEnabled) {
            matchDstMacOnly = matchDstMacOnlyEnabled;
            log.info("Configured. Match Dst MAC Only is {}",
                     matchDstMacOnly ? "enabled" : "disabled");
        }
        boolean matchVlanIdEnabled =
                isPropertyEnabled(properties, "matchVlanId");
        if (matchVlanId != matchVlanIdEnabled) {
            matchVlanId = matchVlanIdEnabled;
            log.info("Configured. Matching Vlan ID is {}",
                     matchVlanId ? "enabled" : "disabled");
        }
        boolean matchIpv4AddressEnabled =
                isPropertyEnabled(properties, "matchIpv4Address");
        if (matchIpv4Address != matchIpv4AddressEnabled) {
            matchIpv4Address = matchIpv4AddressEnabled;
            log.info("Configured. Matching IPv4 Addresses is {}",
                     matchIpv4Address ? "enabled" : "disabled");
        }
        boolean matchIpv4DscpEnabled =
                isPropertyEnabled(properties, "matchIpv4Dscp");
        if (matchIpv4Dscp != matchIpv4DscpEnabled) {
            matchIpv4Dscp = matchIpv4DscpEnabled;
            log.info("Configured. Matching IPv4 DSCP and ECN is {}",
                     matchIpv4Dscp ? "enabled" : "disabled");
        }
        boolean matchIpv6AddressEnabled =
                isPropertyEnabled(properties, "matchIpv6Address");
        if (matchIpv6Address != matchIpv6AddressEnabled) {
            matchIpv6Address = matchIpv6AddressEnabled;
            log.info("Configured. Matching IPv6 Addresses is {}",
                     matchIpv6Address ? "enabled" : "disabled");
        }
        boolean matchIpv6FlowLabelEnabled =
                isPropertyEnabled(properties, "matchIpv6FlowLabel");
        if (matchIpv6FlowLabel != matchIpv6FlowLabelEnabled) {
            matchIpv6FlowLabel = matchIpv6FlowLabelEnabled;
            log.info("Configured. Matching IPv6 FlowLabel is {}",
                     matchIpv6FlowLabel ? "enabled" : "di packetService.addProcessor(processor, PacketProcessor.director(2));sabled");
        }
        boolean matchTcpUdpPortsEnabled =
                isPropertyEnabled(properties, "matchTcpUdpPorts");
        if (matchTcpUdpPorts != matchTcpUdpPortsEnabled) {
            matchTcpUdpPorts = matchTcpUdpPortsEnabled;
            log.info("Configured. Matching TCP/UDP fields is {}",
                     matchTcpUdpPorts ? "enabled" : "disabled");
        }
        boolean matchIcmpFieldsEnabled =
                isPropertyEnabled(properties, "matchIcmpFields");
        if (matchIcmpFields != matchIcmpFieldsEnabled) {
            matchIcmpFields = matchIcmpFieldsEnabled;
            log.info("Configured. Matching ICMP (v4 and v6) fields is {}",
                     matchIcmpFields ? "enabled" : "disabled");
        }
        Integer flowTimeoutConfigured =
                getIntegerProperty(properties, "flowTimeout");
        if (flowTimeoutConfigured == null) {
            flowTimeout = DEFAULT_TIMEOUT;
            log.info("Flow Timeout is not configured, default value is {}",
                     flowTimeout);
        } else {
            flowTimeout = flowTimeoutConfigured;
            log.info("Configured. Flow Timeout is configured to {}",
                     flowTimeout, " seconds");
        }
        Integer flowPriorityConfigured =
                getIntegerProperty(properties, "flowPriority");
        if (flowPriorityConfigured == null) {
            flowPriority = DEFAULT_PRIORITY;
            log.info("Flow Priority is not configured, default value is {}",
                     flowPriority);
        } else {
            flowPriority = flowPriorityConfigured;
            log.info("Configured. Flow Priority is configured to {}",
                     flowPriority);
        }

        boolean ignoreIpv4McastPacketsEnabled =
                isPropertyEnabled(properties, "ignoreIpv4McastPackets");
        if (ignoreIpv4McastPackets != ignoreIpv4McastPacketsEnabled) {
            ignoreIpv4McastPackets = ignoreIpv4McastPacketsEnabled;
            log.info("Configured. Ignore IPv4 multicast packets is {}",
                     ignoreIpv4McastPackets ? "enabled" : "disabled");
        }
    }

    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Integer getIntegerProperty(Dictionary<?, ?> properties,
                                              String propertyName) {
        Integer value = null;
        try {
            String s = Tools.get(properties, propertyName);
            value = isNullOrEmpty(s) ? value : Integer.parseInt(s);
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Check property name is defined and set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return true when the propertyName is defined and set to true
     */
    private static boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                             String propertyName) {
        boolean enabled = false;
        try {
            String flag = Tools.get(properties, propertyName);
            enabled = isNullOrEmpty(flag) ? enabled : flag.equals("true");
        } catch (ClassCastException e) {
            // No propertyName defined.
            enabled = false;
        }
        return enabled;
    }

  
    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
   private class ReactivePacketProcessor implements PacketProcessor{
	//stype = 0;
	byte typeByte = 0000;
	MacAddress hostId;
        DeviceId apId;
	

//	@Override
	public void ipv6process(PacketContext context, InboundPacket pkt){
	
	if(context.isHandled()){
		return;
	}
	Ethernet ethPkt = pkt.parsed();
	HostId id = HostId.hostId(ethPkt.getDestinationMAC());	
	IPv6 ipv6 = (IPv6) ethPkt.getPayload();
	IPacket pkt6 = ipv6;
	IPacket testPkt1 = ipv6;
	IPacket testPkt2 = ipv6;
	testPkt1 = pkt6.getPayload();
	pkt6 = pkt6.getPayload();
	
//	log.info("<2>PacketProcessor : packet info.."+pkt6);
	if(pkt6 != null && pkt6 instanceof ICMP6) {
//		log.info("<2>PacketProcessor : packet info.."+pkt6.toString());
		pkt6 = pkt6.getPayload();
//		log.info("<3>PacketProcessor : packet info.."+pkt6.toString());
		 
                 if(pkt6!= null && pkt6 instanceof RouterSolicitation) {
                        log.info("<>++I get a RS packet: coooool++<>");
			//RS message information
//			log.info("@final----RS messag:"+ testPkt1.toString());
			String rsInfo =  testPkt1.toString();
			String[] msgInfo = rsInfo.split(", ");
			String type = msgInfo[0];
			String code = msgInfo[1];
			String checksum = msgInfo[2];
//			log.info("test string :{}",type);
//			log.info("test string :{}",code);
//			log.info("test string :{}",checksum);
			
		        log.info("service classification is done.");
		
			if(code.equals("icmpCode=4")){
				stype = 0;
				return;
			}
			
			if(code.equals("icmpCode=3")){
				    stype = 3;
				    log.info("##This service uses real time and non-real service\n");
				    typeByte = 0003;
			       	    hostId = ethPkt.getSourceMAC();
                                    apId = pkt.receivedFrom().deviceId();

				for(int i =0; i<destList2.size();i++){
					log.info("test  type 2 cnt: {}",i);
					//if(((DeviceId)desApList2.get(i)).equals(dst.location().deviceId())){
						 Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), pkt.receivedFrom().deviceId(), (DeviceId)desApList2.get(i)); 
					    if (paths.isEmpty()){
						return;
					    }
					    Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
					    if (path == null) {
						log.warn("Don't know where to go from here");
						return;
					    }
					    log.info("Rule is installed.-3");
					    log.info("port()={}, path()={}", pkt.receivedFrom().port(), path);	
					   // }
				}

				   
			}

			//when type is 1
			else if(code.equals("icmpCode=1"))
			{ 
			       stype = 1;
			       typeByte = 0001;
			       log.info("##This service uses non-real time service\n");
                   	       log.info("Host id: " + ethPkt.getSourceMAC() +", Device ID: " + pkt.receivedFrom().deviceId());
			       hostId = ethPkt.getSourceMAC();
                               apId = pkt.receivedFrom().deviceId();
			       //Assigning prefix
			       TestPrefix aPrefix = new TestPrefix(stype);
			       prefix1 = aPrefix.getPrefix(stype);
			       log.info("flowNum ={}",flowNum);
			       if(flowNum==0){
			       	prefix2 = prefix2.valueOf("0000:000:0000:0000::/00");
			       }
				log.info("-------------------------------------------------");
			 TestBCE mService = new TestBCE(hostId, apId, prefix1,prefix2);
                         //mService.addEntry(hostId, mService.createEntry());
                         log.info("---updating RS message-----------------------------------------");
                	 flowNum++;
  			 
			//TODO : get a informarion from testBCE (ACK about updating BCE)
			getHostid = mService.hostid;
        		getApid = mService.apid;
			getIp6Prefix1 = mService.ip6Prefix1;
			getIp6Prefix2 = mService.ip6Prefix2;
			getFlowPrefix1 = mService.flowPrefix1;
			getFlowPrefix2 = mService.flowPrefix2;
			log.info("***** INFO 1) Updating BCE : {}, {}, {}, {}", getHostid, getApid, getIp6Prefix1, getIp6Prefix2);
		   
                	}else if(code.equals("icmpCode=2")){ 
                               stype = 2;
			       typeByte = 0002;
                               log.info("##This service uses real time service\n");
                               log.info("Host id: " + ethPkt.getSourceMAC() +", Device ID: " + pkt.receivedFrom().deviceId());
			
                               hostId = ethPkt.getSourceMAC();
                               apId = pkt.receivedFrom().deviceId();
			       //Assigning prefix
			       TestPrefix aPrefix = new TestPrefix(stype);
			       prefix2 = aPrefix.getPrefix(stype);
			       log.info("flowNum ={}",flowNum);
			       if(flowNum==0){
		              	 prefix1 = prefix1.valueOf("0000:000:0000:0000::/00");
			       }
				log.info("-------------------------------------------------");
			 TestBCE mService = new TestBCE(hostId, apId, prefix1,prefix2);
                         //mService.addEntry(hostId, mService.createEntry());
                         log.info("---updating RS message-----------------------------------------");
                	 flowNum++;
  			 
			//TODO : get a informarion from testBCE (ACK about updating BCE)
			getHostid = mService.hostid;
        		getApid = mService.apid;
			getIp6Prefix1 = mService.ip6Prefix1;
			getIp6Prefix2 = mService.ip6Prefix2;
			getFlowPrefix1 = mService.flowPrefix1;
			getFlowPrefix2 = mService.flowPrefix2;
			log.info("***** INFO 1) Updating BCE : {}, {}, {}, {}", getHostid, getApid, getIp6Prefix1, getIp6Prefix2);   
     
			}else if(stype == 0){
				log.info("***** watting\n");   
			}
			 		
			//TODO ::
	    		stNowApId = (apId).toString();
	    		log.info("cheking----------nowAp ={}\n",stNowApId);
				

		  // TODO: To make the RA message		 
		  //create the ethernet packet
                    Ethernet ethernet = new Ethernet();

                    ethernet.setEtherType(Ethernet.TYPE_IPV6)
                            .setDestinationMACAddress(ethPkt.getSourceMACAddress())
                            .setSourceMACAddress(ethPkt.getDestinationMACAddress());
                    //ethernet.setVlanID(eth.getVlanID());

                    //log.info("1. ethernet Dst: " + ethernet.getSourceMAC()
                    //        + " ethernet Scr: " + ethernet.getDestinationMAC());

		    //create the IPv6 packet
                    IPv6 ipv6ra = new IPv6();
                    ipv6ra.setSourceAddress(ipv6.getDestinationAddress());
                    ipv6ra.setDestinationAddress(ipv6.getSourceAddress());
                    ipv6ra.setHopLimit((byte) 255);

                    //create the ICMPv6 packet
                    ICMP6 icmpRA = new ICMP6();
                    icmpRA.setIcmpType(ICMP6.ROUTER_ADVERTISEMENT);
                    icmpRA.setIcmpCode((byte) 3);
                    icmpRA.setChecksum((short) 0);
		    
                    //TODO :: ERROR
                    //Ip6Prefix prefix1 = mobilityServer.getPrefix(stype);

                    RouterAdvertisement ra = new RouterAdvertisement();
                    ra.setCurrentHopLimit((byte) 64);
                    ra.setMFlag((byte) 0);
                    ra.setOFlag((byte) 0);
                    ra.setRouterLifetime((short) 6000);
                    ra.setReachableTime(0);
                    ra.setRetransmitTimer(0);

                    //Option field
                    byte preFixlength = (byte) 64;
                    byte prefixFlag = (byte) 0xe0;
                    byte[] validLifetime = {(byte) 0000, (byte) 0027, (byte) 008d, (byte) 0000};
                    byte[] preferredLifetime = {(byte) 0000, (byte) 0000, (byte) 008d, (byte) 0027};
                    byte[] reserved2 = {(byte) 00, (byte) 00, (byte) 00, (byte) 00};
                    byte[] optionData = {preFixlength, prefixFlag};
                    byte[] prefix = prefix1.address().toOctets();

                    int prefixLen = prefix.length + optionData.length + 8;
                    int validlen = optionData.length + 4;
                    int preferrdlen = validlen + 4;
                    int res2len = preferrdlen + 4;
                    byte[] prefixInfo = new byte[prefixLen];

                    int i;
                    for (i = 0; i < optionData.length; i++) {
                        prefixInfo[i] = optionData[i];
                    }
                    for (i = optionData.length; i < validlen; i++) {
                        prefixInfo[i] = validLifetime[i - optionData.length];
                    }
                    for (i = validlen; i < preferrdlen; i++) {
                        prefixInfo[i] = preferredLifetime[i - validlen];
                    }
                    for (i = preferrdlen; i < res2len; i++) {
                        prefixInfo[i] = reserved2[i - preferrdlen];
                    }
                    for (i = res2len; i < prefixLen; i++) {
                        prefixInfo[i] = prefix[i - res2len];
                    }

                    ra.addOption(NeighborDiscoveryOptions.TYPE_PREFIX_INFORMATION,
                            prefixInfo);

                    icmpRA.setPayload(ra);
                    ipv6ra.setPayload(icmpRA);
                    ethernet.setPayload(ipv6ra);

                    //sendOut Packet
                    ConnectPoint connectPoint = pkt.receivedFrom();
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                            setOutput(context.inPacket().receivedFrom().port()).build();
                    OutboundPacket packet = new DefaultOutboundPacket(connectPoint.deviceId(),
                            treatment, ByteBuffer.wrap(ethernet.serialize()));

                    packetService.emit(packet);
		    log.info("---sending RA message-----------------------------------------");
                  

  }		

	}

    }


	@Override
	 public void process(PacketContext context) {
            // Stop processing if the packet has been handled,
            // since we can't do any more to it.
	    InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            HostId id = HostId.hostId(ethPkt.getDestinationMAC());
/*
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
	    MacAddress hostId = ethPkt.getSourceMAC();
            HostId id = HostId.hostId(ethPkt.getDestinationMAC());
	    DeviceId apId = pkt.receivedFrom().deviceId();
*/
            // Do we know who this is for? If not, flood and bail.
	    //log.info("Host id : {}",id);

            Host dst = hostService.getHost(id);

	
            if (dst == null) {
               ipv6process(context, pkt);
	       
              return;
	    }
/*
	    if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {
		    log.info("Rule is installed.-1");
		    log.info("DeviceID: dst={}",  pkt.receivedFrom().deviceId(), dst.location().deviceId());
                    installRule(context, dst.location().port());
                }
                return;
            }
				
*/	
 	    if((stPreApId.equals(stNowApId) && stype ==1) || (stPreApId.equals(stNowApId) && stype ==2)){
		  log.info("##############test same ap -------------[ok]\n");
		  log.info("checking preApId ={}, nowApId={} \n",stPreApId, stNowApId);
		  //TODO ::			
	         Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), pkt.receivedFrom().deviceId(), dst.location().deviceId()); 
		    if (paths.isEmpty()) {
		        // If there are no paths, flood and bail.
		        //flood(context);
		        return;
		    }
		  
		    Path path = pickForwardPathIfPossible(paths,pkt.receivedFrom().port());
		    if (path == null) {
		        log.warn("Don't know where to go from here {} for {} -> {}",
		                 pkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
		        //flood(context);
		        return;
		    }
		    log.info("Rule is installed.-2");
		    log.info("port()={}, path()={}", pkt.receivedFrom().port(), path);
		    mkList(hostId, apId, flowNum, pkt.receivedFrom().port(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC(), stype);
		    saveFlowInfo(ethPkt.getDestinationMAC(),pkt.receivedFrom().deviceId(), stype);
		   // stype = 0;

	    }


	   if(! stPreApId.equals(stNowApId) && stype ==3){
		if(! stNowApId.equals("")){
			log.info("##############test handover -------------[ok]\n");
			log.info("checking preApId ={}, nowApId={} \n",stPreApId, stNowApId);
			log.info("test : {}",destList2.size());

			/*for(int i =0; i<destList2.size();i++){
				log.info("test  type 2 cnt: {}",i);
				//if(((DeviceId)desApList2.get(i)).equals(dst.location().deviceId())){
					 Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), pkt.receivedFrom().deviceId(), dst.location().deviceId()); 
				    if (paths.isEmpty()){
					return;
				    }
				    Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
				    if (path == null) {
					log.warn("Don't know where to go from here");
					return;
				    }
				    log.info("Rule is installed.-3");
				    log.info("port()={}, path()={}", pkt.receivedFrom().port(), path);	
				   // }
			}*/

			for(int i =0; i<destList1.size();i++){
				log.info("test  type 1 cnt: {}",i);
				//if(((DeviceId)desApList1.get(i)).equals(dst.location().deviceId())){
					 Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), pkt.receivedFrom().deviceId(), dst.location().deviceId());
				    if (paths.isEmpty()){
					return;
				    }
				    Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
				    if (path == null) {
					log.warn("Don't know where to go from here");
					return;
				    }
				    log.info("Rule is installed.-4");
				    log.info("port()={}, path()={}", pkt.receivedFrom().port(), path);	
				  //  }
			}
			
		}
		if (stype ==0) {
			log.info("###-----------what is packet?\n");
		}
		
	  }
		log.info("#############-------------[waitting]\n");
		
	} 
   }


  public void saveFlowInfo(MacAddress destinationMac, DeviceId desAp, int stype){
	if(stype == 2){
		 destList2.add(destinationMac);
		 desApList2.add(desAp);
		// log.info("List:{}\n",destList2.get(0));
		// log.info("List:{}\n",desApList2.get(0));				
	}else if(stype == 1){
		 destList1.add(destinationMac);
		 desApList1.add(desAp); 
	}
   }

   // Saving flow information at making flow time
   public void mkList(MacAddress hostId, DeviceId apId, int flowNum, PortNumber portNumber, MacAddress sourceMac, MacAddress destinationMac, int stype) {
	if(stype == 1){
		cnt_1++;
		type1 = "type1_"+cnt_1;
	}else if(stype == 2){
		cnt_2++;
		type2 = "type2_"+cnt_2;
	}

	ArrayList flow = new ArrayList();	
	flow.add("flow"+flowNum);
	flow.add(flowNum);
	flow.add(stype);
	flow.add(portNumber);
	flow.add(sourceMac);
	flow.add(destinationMac);
	flow.add(type1);
	flow.add(type2);
	log.info("***Putting flow info--------------ok");
	
	if(stype == 1){
	// non-realtime
		flowPrefix1.add(flow);
	}else if(stype == 2){
	// real time
		flowPrefix2.add(flow);
	}
	
	TestBCE mService = new TestBCE(hostId, apId, prefix1, prefix2, flowPrefix1, flowPrefix2);
   }

   private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        Path lastPath = null;
        for (Path path : paths) {
            lastPath = path;
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return lastPath;
    }
	
    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    // Indicated whether this is an IPv6 multicast packet.
    private boolean isIpv6Multicast(Ethernet eth) {
        return eth.getEtherType() == Ethernet.TYPE_IPV6 && eth.isMulticast();
    }


   // Floods the specified packet if permissible.
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    } 

  // Install a rule forwarding the packet to the specified port.
    // Should update this function for changing the split point per service flow
    //----------------------------------------change------------------------------
    private void installRule(PacketContext context, PortNumber portNumber) {
        //
        // We don't support (yet) buffer IDs in the Flow Service so
        // packet out first.
        //
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        // If PacketOutOnly or ARP packet than forward directly to output port
        if (packetOutOnly || inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            packetOut(context, portNumber);
            return;
        }

        //
        // If matchDstMacOnly
        //    Create flows matching dstMac only
        // Else
        //    Create flows with default matching and include configured fields
        //
        if (matchDstMacOnly) {
            selectorBuilder.matchEthDst(inPkt.getDestinationMAC());
        } else {
            selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                    .matchEthSrc(inPkt.getSourceMAC())
                    .matchEthDst(inPkt.getDestinationMAC());

            // If configured Match Vlan ID
            if (matchVlanId && inPkt.getVlanID() != Ethernet.VLAN_UNTAGGED) {
                selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));
            }

            //
            // If configured and EtherType is IPv4 - Match IPv4 and
            // TCP/UDP/ICMP fields
            //
            if (matchIpv4Address && inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
                byte ipv4Protocol = ipv4Packet.getProtocol();
                Ip4Prefix matchIp4SrcPrefix =
                        Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                                          Ip4Prefix.MAX_MASK_LENGTH);
                Ip4Prefix matchIp4DstPrefix =
                        Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                                          Ip4Prefix.MAX_MASK_LENGTH);
                selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPSrc(matchIp4SrcPrefix)
                        .matchIPDst(matchIp4DstPrefix);

                if (matchIpv4Dscp) {
                    byte dscp = ipv4Packet.getDscp();
                    byte ecn = ipv4Packet.getEcn();
                    selectorBuilder.matchIPDscp(dscp).matchIPEcn(ecn);
                }

                if (matchTcpUdpPorts && ipv4Protocol == IPv4.PROTOCOL_TCP) {
                    TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv4Protocol)
                            .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                            .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                }
                if (matchTcpUdpPorts && ipv4Protocol == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv4Protocol)
                            .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                            .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                }
                if (matchIcmpFields && ipv4Protocol == IPv4.PROTOCOL_ICMP) {
                    ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv4Protocol)
                            .matchIcmpType(icmpPacket.getIcmpType())
                            .matchIcmpCode(icmpPacket.getIcmpCode());
                }
            }

            //
            // If configured and EtherType is IPv6 - Match IPv6 and
            // TCP/UDP/ICMP fields
            //
            if (matchIpv6Address && inPkt.getEtherType() == Ethernet.TYPE_IPV6) {
                IPv6 ipv6Packet = (IPv6) inPkt.getPayload();
                byte ipv6NextHeader = ipv6Packet.getNextHeader();
                Ip6Prefix matchIp6SrcPrefix =
                        Ip6Prefix.valueOf(ipv6Packet.getSourceAddress(),
                                          Ip6Prefix.MAX_MASK_LENGTH);
                Ip6Prefix matchIp6DstPrefix =
                        Ip6Prefix.valueOf(ipv6Packet.getDestinationAddress(),
                                          Ip6Prefix.MAX_MASK_LENGTH);
                selectorBuilder.matchEthType(Ethernet.TYPE_IPV6)
                        .matchIPv6Src(matchIp6SrcPrefix)
                        .matchIPv6Dst(matchIp6DstPrefix);

                if (matchIpv6FlowLabel) {
                    selectorBuilder.matchIPv6FlowLabel(ipv6Packet.getFlowLabel());
                }

                if (matchTcpUdpPorts && ipv6NextHeader == IPv6.PROTOCOL_TCP) {
                    TCP tcpPacket = (TCP) ipv6Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv6NextHeader)
                            .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                            .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                }
                if (matchTcpUdpPorts && ipv6NextHeader == IPv6.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv6Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv6NextHeader)
                            .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                            .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                }
                if (matchIcmpFields && ipv6NextHeader == IPv6.PROTOCOL_ICMP6) {
                    ICMP6 icmp6Packet = (ICMP6) ipv6Packet.getPayload();
                    selectorBuilder.matchIPProtocol(ipv6NextHeader)
                            .matchIcmpv6Type(icmp6Packet.getIcmpType())
                            .matchIcmpv6Code(icmp6Packet.getIcmpCode());
                }
            }
        }
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                                     forwardingObjective);

        //
        // If packetOutOfppTable
        //  Send packet back to the OpenFlow pipeline to match installed flow
        // Else
        //  Send packet direction on the appropriate port
        //
        if (packetOutOfppTable) {
            packetOut(context, PortNumber.TABLE);
        } else {
            packetOut(context, portNumber);
        }
    }


// Wrapper class for a source and destination pair of MAC addresses
    private final class SrcDstPair {
        final MacAddress src;
        final MacAddress dst;

        private SrcDstPair(MacAddress src, MacAddress dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SrcDstPair that = (SrcDstPair) o;
            return Objects.equals(src, that.src) &&
                    Objects.equals(dst, that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }
    }
	
}
