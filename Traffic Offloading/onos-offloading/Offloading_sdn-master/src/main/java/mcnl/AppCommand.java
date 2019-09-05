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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.ProtectedConstraint;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.intent.constraint.ProtectionConstraint.protection;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos", name = "sdnOffloading",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "connectPoint",
            description = "Device/Port Description",
            required = true, multiValued = false)
    String connectPoint = null;

    @Argument(index = 1, name = "option",
            description = "Device/Port Description",
            required = true, multiValued = false)
    String option = null;


    private ForwardingMapService fwd_service;


    @Override
    protected void execute() {

        fwd_service = get(mcnl.ForwardingMapService.class);

     //   print("hello");

        if(connectPoint.equals("caching"))
        {
            fwd_service.test("3");
            print("caching start");
        }

        if(connectPoint.equals("routing"))
        {
            print("routing start");
            if(option.equals("start"))
               fwd_service.test("1");
            else
               fwd_service.test("0");

        }




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



//    @Argument(index = 0, name = "uri", description = "Device ID",
//            required = false, multiValued = false)
//    String uri = null;
//

//    @Override
//    protected void execute() {
//        print("Hello %s", "World");
//        LinkService service = get(LinkService.class);
//        Iterable<Link> links = uri != null ?
//                service.getDeviceLinks(deviceId(uri)) : service.getLinks();
//
//        links.forEach(link-> {
//            print(link.src() + " " + link.dst() + " " + link.dst().port());
//        });
//
//
//    }

}
