from socket import *
import sys
import time

def readCSV():
	dic_device = {}
	try:
		file = open("list_device.csv")
	except:
		print("list_device.csv를 찾을 수 없습니다.")
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

if __name__ == "__main__":
	global dic_device

	dic_device = readCSV()
	if dic_device == None:
		sys.exit()

	while True:
		client = socket(AF_INET, SOCK_STREAM)
		client.connect((dic_device["collector"]["ip"], dic_device["collector"]["port"]))
		client.sendall(bytes("hello world", "utf-8"))
		client.close()

		time.sleep(2)
