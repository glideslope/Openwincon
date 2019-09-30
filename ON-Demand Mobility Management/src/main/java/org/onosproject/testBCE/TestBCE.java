/**
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.testBCE;

import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

//import java.awt.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import org.onosproject.testMobility.testMobility;

/**
 * Store IPv6 host information
 * Host-ID, AP-ID, Prefix address
 * Host-ID and AP-ID information received from appcomponent.java
 * Prefix Address created from this app.
 */

public class TestBCE {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public MacAddress hostid;
    public DeviceId apid; 
    public Ip6Prefix ip6Prefix;
    public Ip6Prefix ip6Prefix1;
    public Ip6Prefix ip6Prefix2;
   // private int stype;
    private ArrayList flow; 
    private ArrayList fakeFlow;
    public ArrayList <ArrayList<String>> flowPrefix1;
    public ArrayList <ArrayList<String>> flowPrefix2;  
   

    //private String prefixFlow2;
    private Entry entry;
    private Entry newEntry;
    private Entry FlowEntry;
    private static ConcurrentMap<MacAddress, Entry> mobilityServerEntry;
  //  private int nonRealRegCnt = 0;
   // private int realRegCnt = 0;
   


    //Mapping between ReactiveForwarding.java value and MobilityServer value
    public TestBCE(MacAddress hostId, DeviceId apId, Ip6Prefix ip6Prefix1, Ip6Prefix ip6Prefix2) {
        this.hostid = hostId;
        this.apid = apId;
	this.ip6Prefix1 = ip6Prefix1;
	this.ip6Prefix2 = ip6Prefix2;
    	 //   this.splitpoint = null;
	if(hostid == null || apid == null || ip6Prefix1 == null || ip6Prefix2 == null){
		return ;
	} else {
	log.info("@----BCE log: hostid={} / apid={} /prefix1={}/prefix2={}",hostId, apId, ip6Prefix1, ip6Prefix2);
	}
    }

//TODO : classification of flowprefix
//TODO : modification of flowPrefix1
    public TestBCE(MacAddress hostId, DeviceId apId, Ip6Prefix ip6Prefix1, Ip6Prefix ip6Prefix2, ArrayList<ArrayList<String>> flowPrefix1, ArrayList<ArrayList<String>> flowPrefix2) {
	//log.info("@----BCE log: flow info ={}",flow);
	this.hostid = hostId;
        this.apid = apId;
	this.ip6Prefix1 = ip6Prefix1;
	this.ip6Prefix2 = ip6Prefix2;
	this.flowPrefix1 = flowPrefix1;
	this.flowPrefix2 = flowPrefix2;
	log.info("info AP={}", apid);
	log.info("info Prefix1={}",ip6Prefix1);
	log.info("info Prefix2={}",ip6Prefix2);
	//log.info("info flowPrefix1={}",flowPrefix1.get(0));  
        //log.info("info flowPrefix2={}",flowPrefix2.get(0));  

  }
 
    //Create new Mobility Server Entry
    public Entry createEntry() {
		Entry entry = new Entry(apid, ip6Prefix1, ip6Prefix2,  fakeList(), fakeList());
     //   	mobilityServerEntry = new ConcurrentHashMap<MacAddress, Entry>();
	 	log.info("@---BCE log: I am successful creating new Entry");
	return entry;
    }

/* 
    public static void createmobilityServerMap() {
        mobilityServerEntry = new ConcurrentHashMap<MacAddress, Entry>();
    }
*/
    public void addEntry(MacAddress hostId, Entry enTry) {
        checkNotNull(mobilityServerEntry, "Mobility Server Entry is null");
        // There is no entry for MN, it makes new
        if (!mobilityServerEntry.containsKey(hostId)) {
            this.hostid = hostId;
            this.entry = enTry;
            log.info("@----BCE: Mobiltiy Server Entry(addEntry): Host id = {}, AP id = {}, Prefix1 = {}, Prefix2 = {}", this.hostid.toString(), this.entry.getApid().toString(), this.entry.getPrefix1().toString(), this.entry.getPrefix2().toString());
            mobilityServerEntry.put(this.hostid, this.entry);
            log.info("----New entry info: {}", mobilityServerEntry.get(hostId));
            //if there is entry for MN, it will maintain or update.
        }else if(mobilityServerEntry.containsKey(hostId)) {
            //it will maintain, cuz Packet received from same AP
           Entry getEntry = mobilityServerEntry.get(hostId);
	   log.info("@----BCE: When MN still stay at same AP!!");	
	   //log.info("getEntry info: {}",getEntry.toString());
//TODO: check if part
            if (getEntry.getApid().equals(enTry.getApid())) { 
 		 log.info("TEST1.");
		if (getEntry.getPrefix1().toString().equals(enTry.getPrefix1().toString())||getEntry.getPrefix2().toString().equals(enTry.getPrefix2().toString())){
 		  log.info("TEST2.");
		 //When same type comes 
		   log.info("@----BCE: User uses same service type.");
		   log.info("Using same prfix at getEntry info: {}",getEntry.toString());	   
		  //TODO : Add Flow info
	       }else {
		   // When it maintains and comes different type
		   log.info("@----BCE: User uses different service type.");
		 //Updating BCE per coming different prefix
		   if(getEntry.getPrefix1().toString().equals("2001:db8:2222::/64")){
			//TODO: Update prefix1 at BCE
			log.info("test about using prefix1");
			newEntry = new Entry(getEntry.getApid(), getEntry.getPrefix1(), enTry.getPrefix2(),fakeList(), fakeList());
			mobilityServerEntry.put(hostid, newEntry);
			log.info("----Updating entry info at using different service type: {}", mobilityServerEntry.get(hostId));	
		
		   }else if(getEntry.getPrefix2().toString().equals("2002:db8:2222::/64")){
		   	//TODO: Update prefix2 at BCE
			log.info("test about using prefix2");
			newEntry = new Entry(getEntry.getApid(), enTry.getPrefix1(), getEntry.getPrefix2(),fakeList(), fakeList());
			mobilityServerEntry.put(hostid, newEntry);
			log.info("----Updating entry info at using different service type: {}", mobilityServerEntry.get(hostId));	
		   }
            	}
	    }
	}
//TODO: Send info to Management about finishing update BCE

      }

    public static void createmobilityServerMap() {
        mobilityServerEntry = new ConcurrentHashMap<MacAddress, Entry>();
    }

    public static void clearmobilityServerEntry() {
        mobilityServerEntry.clear();
    }

    public MacAddress getHostid() {
        return this.hostid;
    }

    public Entry getEntry() {
        return this.entry;
    }
   
    public ArrayList fakeList() {
	fakeFlow = new ArrayList();
	fakeFlow.add("null");
	log.info("flow info:", fakeFlow);
	return fakeFlow;
   }

}


//Value of Map which include AP-ID and Prerix information.
class Entry {
    private DeviceId apid;
    private Ip6Prefix prefix1;
    private Ip6Prefix prefix2;
    private ArrayList <ArrayList<String>> flowPrefix1;
    private ArrayList <ArrayList<String>> flowPrefix2;

    //To get and set AP-ID information
    public DeviceId getApid() {
        return apid;
    }

    public void setApid(DeviceId apId) {
        this.apid = apId;
    }

    //To get and set Prefix information
    public Ip6Prefix getPrefix1() {
        return prefix1;
    }
    public void setPrefix1(Ip6Prefix prefix1) {
        this.prefix1 = prefix1;
    }
    public Ip6Prefix getPrefix2() {
        return prefix2;
    }
    public void setPrefix2(Ip6Prefix prefix2) {
        this.prefix2 = prefix2;
    }
    public ArrayList <ArrayList<String>> getFlowPrefix1() {
        return flowPrefix1;
    }
    public void setFlowPrefix1(ArrayList <ArrayList<String>> flowPrefix1) {
        this.flowPrefix1 = flowPrefix1;
    }
    public ArrayList <ArrayList<String>> getFlowPrefix2() {
        return flowPrefix2;
    }
    public void setFlowPrefix2(ArrayList <ArrayList<String>> flowPrefix2) {
        this.flowPrefix2 = flowPrefix2;
    }


//TODO: extend my BCE
    public Entry(DeviceId hostLocation, Ip6Prefix ip6Prefix1, Ip6Prefix ip6Prefix2, ArrayList <ArrayList<String>> flowPrefix1, ArrayList <ArrayList<String>> flowPrefix2){
        this.setApid(hostLocation);
        this.setPrefix1(ip6Prefix1);
	this.setPrefix2(ip6Prefix2);
	this.setFlowPrefix1(flowPrefix1);
	this.setFlowPrefix2(flowPrefix2);
    }

 /*   public String toString() {
    //    return getApid().toString() + ", " + getPrefix().toString() + ", " + getServicetype() + ", " + getSplitid().toString();
   	  return getApid().toString() + ", " + getPrefix1().toString() + ", " + getPrefix2().toString()+ ", " + getFlowPrefix1().toString()+","+getFlowPrefix2().toString();
    }*/
}

