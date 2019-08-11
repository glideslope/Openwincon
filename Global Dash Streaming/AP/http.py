from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
import urllib2
import re
import os
from socket import *

p = re.compile(r'[0-9]+kbit')
dic_application = None
str_mac = None

def getMac(interface):
	file_in, file_out, file_error = os.popen3('ifconfig ' + interface)
	str_line = file_out.read().split('\n')[0]
	p = re.compile('[a-f\d]{0,2}:.{0,2}:.*:.*:.*:.{0,2}')
	m = p.search(str_line)
	return m.group().replace(':','').rjust(16,'0')


def changeBitrate(str_url):
	if p.search(str_url):
		str_raw = p.search(str_url).group()
		str_before = str_after = str_raw
		
		try:
			socket_client = socket(AF_INET, SOCK_STREAM)
			socket_client.connect((dic_application["IP"],int(dic_application["PORT"])))
			str_msgs = socket_client.recv(1024).split(' ')
			for str_msg in str_msgs:
				if str_msg.split('/')[0] == str_mac:
					str_after = str_msg.split('/')[1] + 'kbit'
					break
		except:
			pass

		print("\nAdusted: "+str_after)
		socket_client.close()
		return str_url.replace(str_before, str_after)
	else:
		return str_url 

class Handler(BaseHTTPRequestHandler):
	def setup(self):
		BaseHTTPRequestHandler.setup(self)
		self.request.settimeout(2)
	def _set_headers(self):
	       	self.send_response(200)
	       	self.send_header('Content-type', 'text/html')
	        self.end_headers()

	def do_GET(self):
		str_url = changeBitrate(self.path)
		self._set_headers()
		try:
			data = urllib2.urlopen(str_url).read()
			self.wfile.write(data)
		except:
			print('abc')
			pass

def runServer(proxy, application, interface):
	global dic_application, str_mac
	dic_application = application
	str_mac = "of:" + getMac(interface)
	HTTPServer(('',int(proxy["PORT"])), Handler).serve_forever()
