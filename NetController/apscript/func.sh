#!/bin/bash

ip=$(($2+100))
ip_prefix="192.168."

dhcp_file="/etc/dhcpcd.conf"
dnsmasq_file="/etc/dnsmasq.conf"
slice_conf_path="/home/mmlab/slice_conf/"


function reload_dhcp_dnsmasq()
{
    dhcp_demon= $(sudo systemctl restart dhcpcd)
    dnsmasq_demon= $(sudo systemctl restart dnsmasq.service)



    if [ -n "$dhcp_demon" ]; then
	    echo "ERROR:: dhcpcd Failed!!"
	    exit 1
    fi

    if [ -n "$dnsmasq_demon" ]; then
	    echo "ERROR:: dnsmasq Failed!!"
	    exit 1
    fi
}
