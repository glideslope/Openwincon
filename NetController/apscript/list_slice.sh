#! /bin/bash

. /home/mmlab/Openwincon/NetController/apscript/func.sh

dev_list=$(iw dev |grep Interface |grep -v "Interface wlan0$" | awk '{print $2}' FS=" ")

json="{ \"slices\": [ "
for i in $dev_list; do
    password=$(grep "wpa_passphrase" $slice_conf_path$i | awk '{print $2}' FS="=")
    id=$(grep "dhcp-range=$i," /etc/dnsmasq.conf | awk '{print $3}' FS="." )
    rate=$(sudo tc qdisc  |grep "qdisc netem" | grep "$i " | awk '{print $15}' FS=" ")
    json=$json" { \"ssid\": \""$i"\", \"passwd\": \""$password"\" , \"id\": "$(($id-100))", \"rate\": \""$rate"\" },"
done


json=${json::-1}
json=$json"  ] }"
echo $json 
