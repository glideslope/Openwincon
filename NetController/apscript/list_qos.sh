#! /bin/bash

. /home/mmlab/Openwincon/NetController/apscript/func.sh

dev_list=$(iw dev |grep Interface |grep -v "Interface wlan0$" | awk '{print $2}' FS=" ")


json="{ \"slices\": [ "
for i in $dev_list; do
    rate=$(sudo tc qdisc  |grep "qdisc netem" | grep "$i " | awk '{print $15}' FS=" ")
    if [ -n "$rate" ]; then
        json=$json" { \"ssid\": \""$i"\", \"rate\": \""$rate""\ " },"
    fi
done


json=${json::-1}
json=$json"  ] }"
echo $json 
