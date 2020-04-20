# AP  
python 폴더  
- 아래 사항들 설정
- collector_ap.py 실행

<br>

# OVS bridge 만들기
- 최초 한번 설정
- \<IP\> 에 SDN Controller IP 입력
```
ovs-vsctl add-br br0
ifconfig br0 up
ovs-vsctl set-controller br0 tcp:<IP>:6653
```

<br>

# wlan 포트 추가하기
- 일부는 부팅시 실행해야함 (밑의 스크립트에 첨부)
- SDN controller에 의해 관리되는 **wlan 포트** 추가  
- **wlan 포트**의 IP를 초기화하고 **bridge**에 IP 부여  
```
ovs-vsctl add-port br0 wlan0
ifconfig br0 up
ifconfig wlan0 0
ifconfig br0 192.168.100.1
```

<br>

# /etc/rc.local  
- 재부팅시 자동 셋팅 스크립트  
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

exit 0
```
