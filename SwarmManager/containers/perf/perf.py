# -*- coding: utf8 -*-

from datetime import datetime as dt
import threading, time
import os
import socket
import psutil
import requests as req


class PerformanceTracker:
    def __init__(self):
        # TODO: 가상머신의 경우 CPU frequncy 정확히 확인 필요
        self.cpu_freqs = [freq.max if freq.max != 0.0 else freq.current for freq in psutil.cpu_freq(True)]
        self.total_freqs = sum(self.cpu_freqs)
        self.mem_total = psutil.virtual_memory().total
        self.disk_total = psutil.disk_usage('/').total
        #self.fp = open('result/perf.csv', 'a')
        self.manager_url = 'http://' + os.environ['manager_addr'] + ':' + os.environ['manager_port']
        #self.manager_url = 'http://147.46.215.184:42664'
        self.node_id = socket.gethostname()

    def writer_func(self):
        '''
        while True:
            s = ','.join(self.total_freqs, self.cpu_load(), 
                    self.mem_total, self.memory_load(), 
                    self.disk_total, self.disk_load())
        '''
        while True:
            # print('Writer_func')

            #self.fp.write('%s,%s,%s,%s,%s,%s\n' % (
            #    self.total_freqs, self.cpu_load(),
            #    self.mem_total, self.memory_load(),
            #    self.disk_total, self.disk_load()
            #))
            #self.fp.write(str(dt.now()))
            data = {'cpu_total': self.total_freqs, 'cpu_cur': self.cpu_load(), 
                    'mem_total': self.mem_total, 'mem_cur': self.memory_load(),
                    'disk_total': self.disk_total, 'disk_cur': self.disk_load(),
                    'node_id': self.node_id}
            req.get(self.manager_url, params=data) 
            time.sleep(1)
            #self.fp.flush()

    def cpu_load(self):
        total_freq = sum(self.cpu_freqs)
        return sum(psutil.cpu_percent(interval=.1, percpu=True)) / (len(self.cpu_freqs) * 100)  * total_freq

    def memory_load(self):
        return psutil.virtual_memory().used

    def disk_load(self):
        return psutil.disk_usage('/').used

    
if __name__ == '__main__':
    tracker = PerformanceTracker()
    if True:
        print(tracker.cpu_load())
        print(tracker.memory_load())
        print(tracker.disk_load())

    tracker.writer_func()
