import os
import sys
import json

import http as HTTP

def getIP():
	f = open('server.json', 'r')
	json_data = json.load(f)
	f.close()
	return json_data["Server"] 

def openServer(proxy, optimizer):
        file_in, file_out, file_error = os.popen3('iwconfig ' + sys.argv[1])
        if file_error.read().find("No such device") < 0:
                HTTP.runServer(proxy, optimizer, sys.argv[1])

if __name__ == '__main__':
	if len(sys.argv) != 2:
		print('Help > python proxy.py <wlan interface>')
	else:
		dic_server = getIP()
		openServer(dic_server["Proxy"], dic_server["SDN_Application"])
