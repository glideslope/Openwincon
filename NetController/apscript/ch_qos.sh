#! /bin/bash

. /home/mmlab/Openwincon/NetController/apscript/func.sh


rate=$(sudo tc qdisc  |grep "qdisc netem" | grep "$1 " | awk '{print $15}' FS=" ")


if [ -z "$rate" ]; then
	echo "ERROR:: QOS Not exist!!"
	exit 1
fi

sudo tc qdisc change dev $1 root netem rate $2
