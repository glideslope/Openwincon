from operator import eq
from os import system
from time import sleep
import socket
import threading
import commands
import ftplib

UDP_IP = '172.16.0.24'
UDP_PORT = 10987
sock_UDP = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock_UDP.bind(('', UDP_PORT))
userList = []
vnfList = []

class sending(threading.Thread):
    def run(self):
        system("clear")
        while True:
            msg = raw_input()
            if eq(msg, "q"):
                pid = commands.getoutput("ps | grep python | head -1 | awk '{print $1}'")
                system("kill -9 " + pid)
            else:
                if "-" in msg:
                    print(msg)
                    sock_UDP.sendto(msg.encode(), (UDP_IP, UDP_PORT))
                else:
                    print("Type Error")

class recving(threading.Thread):
    def run(self):
        while True:
            data, addr = sock_UDP.recvfrom(1024)
            recvData = data.decode()
            if eq(recvData.split("-")[0], "exist"):
                funcData = recvData.split("-")[1]
                userMAC = funcData.split("/")[0]
                reqIP = funcData.split("/")[1]
                vnfName = funcData.split("/")[2]
                msg = "req-/home/ntl/vmwareStorage/" + vnfName + "@" + userMAC
                sock_UDP.sendto(msg.encode(), (reqIP, UDP_PORT))
            elif eq(recvData.split("-")[0], "req"):
                responseAddr = addr[0]
                contents = recvData.split("-")[1]
                filePath = contents.split("@")[0]
                userMAC = contents.split("@")[1]
                vnfName = filePath.split("/")[4]
                print(commands.getoutput("virsh shutdown " + vnfName))
                print(commands.getoutput("virsh undefine " + vnfName))
                ftp = ftplib.FTP(responseAddr, 'ntl', '1')
                imgData = open(filePath + '.img', 'rb')
                xmlData = open(filePath + '.xml', 'rb')
                print("FTP sending...")
                ftp.storbinary('STOR ' + filePath + '.img', imgData)
                ftp.storbinary('STOR ' + filePath + '.xml', xmlData)
                imgData.close()
                xmlData.close()
                ftp.quit()
                vnfName = filePath.split("/")[4]
                msg = "res-" + userMAC + "/" + vnfName
                sock_UDP.sendto(msg.encode(), (responseAddr, UDP_PORT))
                system("rm -rf /home/ntl/vmwareStorage/" + vnfName + "*")
                print("Done")
            elif eq(recvData.split("-")[0], "res"):
                contents = recvData.split("-")[1]
                userMac = contents.split("/")[0]
                vnfName = contents.split("/")[1]
                updateMsg = "update-" + userMAC + "@" + vnfName
                system("chown root /home/ntl/vmware/" + vnfName + ".img")
                system("chgrp root /home/ntl/vmware/" + vnfName + ".img")
                system("chown root /home/ntl/vmware/" + vnfName + ".xml")
                system("chgrp root /home/ntl/vmware/" + vnfName + ".xml")
                print(commands.getoutput("virsh define /home/ntl/vmware/" + vnfName + ".xml"))
                print(commands.getoutput("virsh start " + vnfName))
                sock_UDP.sendto(updateMsg.encode(), (UDP_IP, UDP_PORT))
            else:
                print(recvData)

class searchTask(threading.Thread):
    def run(self):
        global userList
        global vnfList
        while True:
            resultUser = commands.getoutput("iw dev wlan0 station dump | awk '/Station/{print $2}'")
	    resultVnf = commands.getoutput("virsh list --all | awk '/running/{print $2}'")
            user = []
            if resultUser != '':
                user = resultUser.split("\n")
                if len(user) > len(userList):
                    addList = list(set(user) - set(userList))
                    userList = user
                    for i in addList:
                        msg = "new-" + i
                        sock_UDP.sendto(msg.encode(), (UDP_IP, UDP_PORT))
                elif len(user) < len(userList):
                    userList = user
            else:
                userList = []

            if resultVnf != '':
                vnf = resultVnf.split("\n")
                if len(vnf) > len(vnfList):
                    addList2 = list(set(vnf) - set(vnfList))
                    vnfList = vnf
                    for i in addList2:
                        print(i)
                        msg = "vnf-" + i
                        sock_UDP.sendto(msg.encode(), (UDP_IP, UDP_PORT))
                elif len(vnf) < len(vnfList):
                    vnfList = vnf
            sleep(0.5)

sendThread = sending()
recvThread = recving()
searchThread = searchTask()

sendThread.start()
recvThread.start()
searchThread.start()
