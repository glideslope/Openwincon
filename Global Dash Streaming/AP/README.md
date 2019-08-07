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