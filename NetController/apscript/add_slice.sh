#! /bin/bash

. /home/mmlab/func.sh


dhcp_ap_exist=$(grep -n "^interface $1$" $dhcp_file | awk '{print $1}' FS=":")
dhcp_ip_exist=$(grep -n "^static ip_address=$ip_prefix$ip.1/24$" $dhcp_file | awk '{print $1}' FS=":")
dnsmasq_ap_exist=$(grep -n "^interface=$1$" $dnsmasq_file | awk '{print $1}' FS=":")
dnsmasq_ip_exist=$(grep -n "^dhcp-range=$1,$ip_prefix$ip.2" $dnsmasq_file | awk '{print $1}' FS=":")
slice_conf_exist=$(ls $slice_conf_path$1 2> /dev/null)
virtual_ap_exist=$(iw dev  |grep "Interface $1")


if [ -n "$virtual_ap_exist" ]; then
	echo "ERROR:: Virtual AP name exist"
	exit 1
fi

if [ -n "$slice_conf_exist" ]; then
	echo "ERROR:: Virtual AP configure file exist"
	exit 1
fi

if [ -n "$dhcp_ap_exist" ]; then
	echo "ERROR:: AP name exist in dhcpcd.conf!!"
	exit 1
fi

if [ -n "$dhcp_ip_exist" ]; then
	echo "ERROR:: IP address exist in dhcpcd.conf!!"
	exit 1
fi

if [ -n "$dnsmasq_ap_exist" ]; then
	echo "ERROR:: AP name exist in dnsmasq.conf!!"
	exit 1
fi

if [ -n "$dnsmasq_ip_exist" ]; then
	echo "ERROR:: IP address exist in dnsmasq.conf!!"
	exit 1
fi

echo "interface "$1										| sudo tee -a  $dhcp_file > /dev/null
echo "static ip_address=192.168."$(($2+100))".1/24" 	| sudo tee -a  $dhcp_file > /dev/null
echo "static domain_name_servers=8.8.8.8"				| sudo tee -a  $dhcp_file > /dev/null
echo "nohook wpa supplicant"							| sudo tee -a  $dhcp_file > /dev/null
echo "" 												| sudo tee -a  $dhcp_file > /dev/null


echo "interface="$1 | sudo tee -a $dnsmasq_file > /dev/null
echo "dhcp-range="$1",192.168."$ip".2,192.168."$ip".100,255.255.255.0,24h" | sudo tee -a $dnsmasq_file > /dev/null
echo "" | sudo tee -a $dnsmasq_file > /dev/null

reload_dhcp_dnsmasq

echo "interface="$1				| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "driver=nl80211"			| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "ssid="$1					| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "hw_mode=g"				| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "channel=7"				| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "wmm_enabled=0"			| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "macaddr_acl=0"			| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "auth_algs=1"				| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "ignore_broadcast_ssid=0"	| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "wpa=2"					| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "wpa_passphrase="$3		| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "wpa_key_mgmt=WPA-PSK"		| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "wpa_pairwise=TKIP"		| sudo tee -a  $slice_conf_path$1 > /dev/null
echo "rsn_pairwise=CCMP"		| sudo tee -a  $slice_conf_path$1 > /dev/null

sudo iw dev wlan0 interface add $1 type __ap
sleep 1
sudo killall wpa_supplicant > /dev/null
sudo hostapd $slice_conf_path$1 &>/dev/null &
