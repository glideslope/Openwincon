# OVS bridge �����

```
ovs-vsctl add-br br0
ifconfig br0 up
ovs-vsctl set-controller br0 tcp:<IP>:6653
```

<br>

# wlan ��Ʈ �߰��ϱ�
SDN controller�� ���� �����Ǵ� **wlan ��Ʈ** �߰�  
**wlan ��Ʈ**�� IP�� �ʱ�ȭ�ϰ� **bridge**�� IP �ο�  
```
ovs-vsctl add-port br0 wlan0
ifconfig br0 up
ifconfig wlan0 0
/* device�� AP�� ��� */
ifconfig br0 <ip>
/* device�� client�� ��� */
dhclient br0
```