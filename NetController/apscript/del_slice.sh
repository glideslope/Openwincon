#! /bin/bash

. /home/mmlab/Openwincon/NetController/apscript/func.sh


dhcp_ap_exist=$(grep -n "^interface $1$" $dhcp_file | awk '{print $1}' FS=":")
dhcp_ip_exist=$(grep -n "^static ip_address=$ip_prefix$ip.1/24$" $dhcp_file | awk '{print $1}' FS=":")
dnsmasq_ap_exist=$(grep -n "^interface=$1$" $dnsmasq_file | awk '{print $1}' FS=":")
dnsmasq_ip_exist=$(grep -n "^dhcp-range=$1,$ip_prefix$ip.2" $dnsmasq_file | awk '{print $1}' FS=":")
slice_conf_exist=$(ls $slice_conf_path$1 2> /dev/null)
virtual_ap_exist=$(iw dev  |grep "Interface $1")


if [ -z "$virtual_ap_exist" ]; then
	echo "ERROR:: Virtual AP name exist"
	exit 1
fi

if [ -z "$slice_conf_exist" ]; then
	echo "ERROR:: Virtual AP configure file exist"
	exit 1
fi

if [ -z "$dhcp_ap_exist" ]; then
	echo "ERROR:: AP name NOT exist in dhcpcd.conf!!"
	exit 1
fi

if [ -z "$dhcp_ip_exist" ]; then
	echo "ERROR:: IP address NOT exist in dhcpcd.conf!!"
	exit 1
fi

if [ -z "$dnsmasq_ap_exist" ]; then
	echo "ERROR:: AP name NOT exist in dnsmasq.conf!!"
	exit 1
fi

if [ -z "$dnsmasq_ip_exist" ]; then
	echo "ERROR:: IP address NOT exist in dnsmasq.conf!!"
	exit 1
fi

sudo sed -i "/^interface $1$/,/^$/d" $dhcp_file
sudo sed -i "/^interface=$1$/,/^$/d" $dnsmasq_file


sudo iw $1 del
sudo rm -rf $slice_conf_path$1

PID=$(ps -ef | grep "hostapd" | grep $1 |grep -v 'grep' | awk '{print $2}' )
for i in $PID
do
	kill -9 $i
done

reload_dhcp_dnsmasq

