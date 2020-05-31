# OVS bridge 만들기

```
ovs-vsctl add-br br0
ifconfig br0 up
ovs-vsctl set-controller br0 tcp:<IP>:6653
```

<br>

# wlan 포트 추가하기
SDN controller에 의해 관리되는 **wlan 포트** 추가  
**wlan 포트**의 IP를 초기화하고 **bridge**에 IP 부여  
```
ovs-vsctl add-port br0 wlan0
ifconfig br0 up
ifconfig wlan0 0
/* device가 AP일 경우 */
ifconfig br0 <ip>
/* device가 client일 경우 */
dhclient br0
```

<br>

# Proxy 포트 허용  
Proxy 서버(웹)를 사용하기 위해서는 외부에서 포트 80 접근을 허용해야 함  
```
iptables -A INPUT -p tcp --dport <port number> -j ACCEPT
```

<br>

# /etc/rc.local  
재부팅시 자동 셋팅 스크립트  
```
#!/bin/sh
#
# rc.local
#
# This script is executed at the end of each multiuser runlevel.
# Make sure that the script will "exit 0" on success or any other
# value on error.
#
# In order to enable or disable this script just change the execution
# bits.
#
# By default this script does nothing.

service ssh restart
iptables-restore < /etc/iptables.ipv4.nat

su root -c "ifconfig wlan0 0"
su root -c "ifconfig ap0 192.168.100.1"
su root -c "service apache2 stop"
su root -c "iptables -A INPUT -p tcp --dport 80 -j ACCEPT"

exit 0
```
