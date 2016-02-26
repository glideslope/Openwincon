from socket import *
import sys
import time
import os

def readCSV():
	dic_device = {}
	try:
		file = open("list_device.csv")
	except:
		print("Can not find list_device.csv")
		return None

	for i, line in enumerate(file.readlines()):
		if i == 0:
			continue
		list_item = line.split(",")
		name = list_item[0].strip()
		ip = list_item[1].strip()
		try:
			port = int(list_item[2].strip())
		except:
			port = 0

		dic_device[name] = {}
		dic_device[name]["ip"] = ip
		dic_device[name]["port"] = port
		
	return dic_device

def getMac():
	os.system("ifconfig wlan0 > temp")
	file = open("temp", "r")

	str_mac = None
	for line in file.readlines():
		if "HWaddr" in line:
			str_mac = line.split("HWaddr")[1].strip()
			break

	file.close()
	os.remove("temp")

	if str_mac == None:
		print("Check your network interface")

	return str_mac

def makeMessage(str_mac_ap):
	os.system("iw dev wlan0 station dump > temp")
	file = open("temp", "r")
	list_lines = file.readlines()
	len_lines = len(list_lines) - 1
	idx_line = -1

	dic_client = {}

	str_mac_client = None
	str_rssi = None
	while idx_line < len_lines:
		idx_line += 1
		text = list_lines[idx_line].strip()
		if "Station" in text:
			str_mac_client = text.split(" ")[1]
			
		if "signal" in text:
			str_rssi = text.split(" ")[2].strip()
			dic_client[str_mac_client] = str_rssi
	
	file.close()
	os.remove("temp")
	if str_mac_client == None:
		return None

	str_message = "%s/" % str_mac_ap
	
	for client in dic_client:
		str_message += "%s,%s/" % (client, dic_client[client])

	return str_message[:-1]

if __name__ == "__main__":
	global dic_device

	dic_device = readCSV()
	if dic_device == None:
		sys.exit()

	str_mac = getMac()
	if str_mac == None:
		sys.exit()

	while True:
		socket_ap = socket(AF_INET, SOCK_STREAM)
		socket_ap.connect((dic_device["collector"]["ip"], dic_device["collector"]["port"]))
		
		str_message = makeMessage(str_mac)
		socket_ap.sendall(str_message.encode())
		socket_ap.close()

		time.sleep(2)
