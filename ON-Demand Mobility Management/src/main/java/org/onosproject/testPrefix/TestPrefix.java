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
package org.onosproject.testPrefix;

import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;
import org.onosproject.testMobility.testMobility;

//import java.awt.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Store IPv6 host information
 * Host-ID, AP-ID, Prefix address
 * Host-ID and AP-ID information received from appcomponent.java
 * Prefix Address created from this app.
 */

public class TestPrefix {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private MacAddress hostid;
    private DeviceId apid;
    private Ip6Prefix ip6Prefix;
    private Ip6Prefix ip6Prefix1;
    private Ip6Prefix ip6Prefix2;
    private int stype;

    //Mapping between ReactiveForwarding.java value and MobilityServer value
    public TestPrefix(int stype) {
        this.stype = stype;
    }
    public Ip6Prefix getPrefix(int stype){
	if(stype == 1){
		// this service is non-real time
		this.ip6Prefix1 = ip6Prefix1.valueOf("2001:DB8:2222:0000::/64");
		//this.ip6Prefix2 = ip6Prefix2.valueOf("0000:000:0000:0000::/00");
		log.info("BCE-----Non-real time service Prefix is info: Prefix1 ={}, prefix2 ={}",this.ip6Prefix1, this.ip6Prefix2);
		ip6Prefix = ip6Prefix1;
	} else if(stype == 2){
		 // this service is real time
                this.ip6Prefix2 = ip6Prefix2.valueOf("2002:DB8:2222:0000::/64");
		//this.ip6Prefix1 = ip6Prefix1.valueOf("0000:000:0000:0000::/00");
                log.info("BCE-----Real time service Prefix is info: Prefix1 ={}, Prefix2 ={}",this.ip6Prefix1, this.ip6Prefix2);
		this.ip6Prefix = this.ip6Prefix2;
		ip6Prefix = ip6Prefix2;
	}
        return ip6Prefix;
    }
}


