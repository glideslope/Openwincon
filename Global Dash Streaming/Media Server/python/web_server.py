from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.request import urlopen

import sys
import threading
import os
import time
from socket import *

CONST_KB = 1024
NUM_AP = 2

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

def generatePort(port):

	while True:
		
		socket_gen = socket()
		socket_gen.bind(("", dic_device["gen"]["port"]))
		socket_gen.listen(1)   

		con, addr = socket_gen.accept()
		con.sendall(str(port).encode())
			
		print("New session port:", end = "")
		for ap in range(NUM_AP):
			print(" %d"% port, end = "")
			thread_divide = threading.Thread(target = divideChunk, args = (port, ))
			thread_divide.daemon = True
			thread_divide.start()
			port += 1

		print()
		time.sleep(1)

def divideChunk(port):
	while True:
		try:
			socket_server = socket()
			socket_server.bind(('', port))
			socket_server.listen(1)   
			
			con, addr = socket_server.accept()
			con.settimeout(2000)
			
			# Query 분석
			raw = con.recv(CONST_KB).decode("utf-8")
			type = raw.split("/")[3]
			query = raw.split("/")[-1]
			object = "../media/" + type + "/" + query.split("?")[0]
			file = open(object, "rb")
			byte_data = file.read()

			# OFFSET 계산하기
			divide = float(query.split("?")[1].split("=")[1])
			length = sys.getsizeof(byte_data)
			offset = int(length * divide)

			if port % 2 == 0:
				con.sendall(str(offset).encode())
				con.sendall(byte_data[0: offset])
				str_log = "To session %d, %s, %d bytes" % (port, object.split("/")[-1], offset)
			else:
				con.sendall(str(length - offset).encode())
				con.sendall(byte_data[offset: length])
				str_log = "To session %d, %s, %d bytes" % (port, object.split("/")[-1], length - offset)
					
			print(str_log)

			str_ack = con.recv(CONST_KB).decode()
				
			# 응답 받고 소켓 닫기			
			con.close()
			socket_server.close()

			# 재요청시
			if str_ack == "re":
				raise Exception

		except Exception as e:
			print("Initialize session port: %d" % port)

class HandlerHTTP(BaseHTTPRequestHandler):

	def setup(self):
		BaseHTTPRequestHandler.setup(self)
		self.request.settimeout(2)

	def _set_headers(self, code):
		self.send_response(code)
		self.send_header('Content-type', 'text/html')
		self.end_headers()

	def do_GET(self):
		try:
			if self.path == "/":
				object = "../html/index.html"
			elif self.path == "/index":
				object = "../html/index.html"
			elif self.path == "/index.html":
				object = "../html/index.html"

			elif self.path[-3:] == ".js":
				object = "../html" + self.path

			elif self.path[-4:] == ".mpd":
				object = "../media" + self.path
			elif self.path[-4:] == ".mp4":
				object = "../media" + self.path

			else:
				raise FileNotFoundError
				
			self._set_headers(200)
			file = open(object, "rb")
			self.wfile.write(file.read())

		except FileNotFoundError:
			self._set_headers(404)
			self.wfile.write(bytes(b"404 Not Found"))			

		except Exception as e:
			print(e)

if __name__ == "__main__":
	global dic_device

	dic_device = readCSV()
	if dic_device == None:
		sys.exit()

	# 포트 생성기
	thread_gen = threading.Thread(target = generatePort, args = (dic_device["session"]["port"], ))
	thread_gen.daemon = True
	thread_gen.start()

	# HTTP 서버
	sever_http = HTTPServer(("", dic_device["server"]["port"]), HandlerHTTP)
	sever_http.serve_forever()