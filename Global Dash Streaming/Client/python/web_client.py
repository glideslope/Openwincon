from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.request import urlopen
import os
import sys
from socket import *

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
	socket_gen = socket()
	socket_gen.connect((dic_device["gen"]["ip"], dic_device["gen"]["port"]))
	port = int(socket_gen.recv(CONST_KB).decode())
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
		print(self.path)

		if ".m4s" in self.path:
			query = self.path + "?x=0.5"

			byte_data = bytes(b"")
			for i, element in enumerate(list_adaptor):
				mac_ap = element[0]
				ip_ap = element[1]

				socket_interface = socket()
				socket_interface.bind((ip_ap, 0))
				socket_interface.connect((dic_device["server"]["ip"], dic_device["server"]["port"]))
				# 쿼리는 처음 한번만
				if i == 0:
					socket_interface.sendall(query.encode())
        
				str_size = socket_interface.recv(CONST_KB).decode()
        
				socket_interface.sendall(bytes(b"ok"))

				byte_data += socket_interface.recv(int(str_size))
				print("%s: %s bytes" % (mac_ap, str_size))

			self._set_headers(200)
			self.wfile.write(byte_data)

		else:
			try:
				self._set_headers(200)
				byte_data = urlopen(self.path).read()
				self.wfile.write(byte_data)
			except FileNotFoundError:
				self._set_headers(404)
				self.wfile.write(bytes(b"404 Not Found"))			
			except Exception as e:
				print(e)

if __name__ == "__main__":
	global dic_device, list_adaptor

	dic_device = readCSV()
	if dic_device == None:
		sys.exit()

	list_adaptor = getInterface()

	dic_device["server"]["port"] = getPort()

	server_proxy = HTTPServer(("", dic_device["client"]["port"]), HandlerProxy)
	server_proxy.serve_forever()