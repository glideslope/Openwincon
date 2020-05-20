from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.request import urlopen
from socket import *

import os
import sys
import re
import time
import traceback
import logging
logging.basicConfig(level = logging.ERROR)

PATTERN_BITRATE = re.compile("\d+K|\d+M")

CONST_KB = 1024

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
	
def getInterface():
	os.system("ipconfig /all > ip")
	file = open("ip", "r")
	
	dic_idx = {}
	list_lines = file.readlines()
	len_lines = len(list_lines) - 1
	idx_line = -1
	while idx_line < len_lines:
		idx_line += 1
		text = list_lines[idx_line].strip()
		if text == "":
			continue

		# 무선 LAN 찾기, 대신 로컬은 제외
		if "무선 LAN 어댑터" in text and (not "로컬" in text):
			dic_idx[text] = idx_line

	# 무선 LAN 파싱
	list_adaptor = []
	for name in dic_idx:
		idx = dic_idx[name]
		while True:
			# 연결 안된 인터페이스 발견시
			if "미디어 상태" in list_lines[idx]:
				break
			if "물리적 주소" in list_lines[idx]:
				mac = list_lines[idx].split(":")[1].strip()
			if "IPv4 주소" in list_lines[idx]:
				ip = list_lines[idx].split(":")[1].strip().replace("(기본 설정)", "")
				list_adaptor.append( (mac, ip) )
				break
			idx += 1
	list_adaptor.sort()	
	file.close()
	os.remove("ip")

	return list_adaptor

def getPort():
	while True:
		try:
			socket_gen = socket()
			socket_gen.connect((dic_device["gen"]["ip"], dic_device["gen"]["port"]))
			port = int(socket_gen.recv(CONST_KB).decode())
			print("Data port:", port, port + 1)
			break
		except Exception as e :
			print(e)
			time.sleep(0.1)

	return port

class HandlerProxy(BaseHTTPRequestHandler):
	def setup(self):
		BaseHTTPRequestHandler.setup(self)
		self.request.settimeout(2)

	def _set_headers(self, code):
		self.send_response(code)
		self.send_header('Content-type', 'text/html')
		self.end_headers()

	def do_GET(self):
		global list_bitrate, int_seg_finished

		if ".m4s" in self.path:
			while True:
				try:
					socket_control = socket()
					socket_control.connect((dic_device["control"]["ip"], dic_device["control"]["port"] + int(list_adaptor[0][1][-1])))

					str_bitrate_origin = PATTERN_BITRATE.search(self.path).group()
					mac_ue = list_adaptor[0][0].replace("-", "").lower()
					str_type = self.path.split("/")[3]
					int_seg = int(self.path.split("dash")[1].split(".")[0])
					if int_seg_finished + 1 != int_seg:
						str_origin = "dash%d" % int_seg
						
						int_seg = int_seg_finished + 1
						str_change = "dash%d" % int_seg
						self.path = self.path.replace(str_origin, str_change)

					str_message = "%s/%s/%s/%d\n" % (str_bitrate_origin, mac_ue, str_type, int_seg)
					socket_control.sendall((str_message).encode())
			
					list_data = socket_control.recv(CONST_KB).decode().split("/")
					str_bitrate_adjusted = list_data[0]
					str_ratio = list_data[1]
					socket_control.close()

					query = self.path.replace(str_bitrate_origin, str_bitrate_adjusted) + "?x=" + str_ratio
					print("query:", query)
					
					# 데이터 손실 체크
					if (str_bitrate_adjusted[:-1] in list_bitrate) is False:
						print("bitrate was wrong")
						raise Exception

					break
				except Exception as e:
					print(e)
					time.sleep(0.1)

			while True:
				try:
					sum_data = bytes(b"")
					for i, element in enumerate(list_adaptor):
						mac_ue = element[0]
						ip_ue = element[1]

						socket_interface = socket()
						socket_interface.bind((ip_ue, 0))
						socket_interface.connect((dic_device["server"]["ip"], dic_device["server"]["port"] + i))
						
						socket_interface.sendall(query.encode())
        
						int_size = int(socket_interface.recv(CONST_KB).decode())
						byte_data = socket_interface.recv(int_size)
				
						print("%s: %d bytes" % (mac_ue, int_size))
						if int_size != 0:
							sum_data += byte_data

						# 데이터 손실 체크
						check_byte = len(byte_data)
						if int_size != 0 and abs(check_byte - int_size) > 33:
							print("media file was corrupted (%d / %d bytes)" % (check_byte, int_size))
							socket_interface.sendall(bytes(b"re"))
							socket_interface.close()
							raise Exception
						else:
							socket_interface.sendall(bytes(b"next"))
							socket_interface.close()
					break
				except Exception as e :
					print(e)
					time.sleep(0.1)

			self._set_headers(200)
			self.wfile.write(sum_data)	
			int_seg_finished += 1

		else:
			try:
				self._set_headers(200)
				byte_data = urlopen(self.path).read()
				if ".mpd" in self.path:

					list_element = str(byte_data).replace("<", "").split("/>")
					for element in list_element:
						if "Representation id" in element:
							list_bitrate.append(PATTERN_BITRATE.search(element).group()[:-1])

				self.wfile.write(byte_data)
			except FileNotFoundError:
				self._set_headers(404)
				self.wfile.write(bytes(b"404 Not Found"))			
			except Exception as e:
				print(e)

if __name__ == "__main__":
	global dic_device, list_adaptor, list_bitrate, int_seg_finished

	dic_device = readCSV()
	if dic_device == None:
		sys.exit()

	list_adaptor = getInterface()
	len_adaptor = len(list_adaptor)

	if len_adaptor == 0:
		print("무선 LAN 연결을 확인해주세요")
		sys.exit()
	if len_adaptor == 1:
		print("무선 LAN을 하나 더 장착해주세요")
		sys.exit()
	elif len_adaptor > 2:
		print("지원가능한 무선 LAN의 갯수는 2개 입니다.")
		sys.exit()

	dic_device["server"]["port"] = getPort()

	int_seg_finished = 0
	list_bitrate = []
	server_proxy = HTTPServer(("", dic_device["client"]["port"]), HandlerProxy)
	server_proxy.serve_forever()