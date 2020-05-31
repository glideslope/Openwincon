#! /bin/bash

. /home/mmlab/Openwincon/NetController/apscript/func.sh


rate=$(sudo tc qdisc  |grep "qdisc netem" | grep "$1 " | awk '{print $15}' FS=" ")


if [ -n "$rate" ]; then
	echo "ERROR:: QOS exist!!"
	exit 1
fi

sudo tc qdisc add dev $1 root netem rate $2
