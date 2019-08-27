# -*- coding: utf8 -*-


import os, sys
import time
import threading
sys.path.append(os.path.dirname(os.getcwd()))

import numpy as np

import node as node_module


class NodeMonitor:
    def __init__(self):
        swarm_nodes = node_module.get_nodes()
        self.swarm_nodes = swarm_nodes
        self.node_dic = {}
        self.read_cnt = 0
        for node in swarm_nodes:
            hostname = node.attrs['Description']['Hostname']

            with open('perf/csv/perf_%s.csv' % hostname) as fp:
                lines = fp.read().splitlines()

            measure_lst = []
            for line in lines:
                info = list(map(float, line.split(',')))
                info_dic = {'cpu_total': info[0], 'cpu_cur': info[1],
                        'mem_total': info[2], 'mem_cur': info[3],
                        'disk_total': info[4], 'disk_cur': info[5]}
                measure_lst.append(info_dic)

            self.node_dic[hostname] = measure_lst
        
        t = threading.Thread(target=self.monitor_thread)
        t.start()

    def monitor_thread(self):
        while True:
            self.read_cnt += 1
            for node in self.swarm_nodes:
                hostname = node.attrs['Description']['Hostname']
                
                with open('perf/csv/perf_%s.csv' % hostname) as fp:
                    lines = fp.read().splitlines()

                measure_lst = []
                for line in lines:
                    info = list(map(float, line.split(',')))
                    info_dic = {'cpu_total': info[0], 'cpu_cur': info[1],
                            'mem_total': info[2], 'mem_cur': info[3],
                            'disk_total': info[4], 'disk_cur': info[5]}
                    measure_lst.append(info_dic)

                self.node_dic[hostname] = measure_lst

            time.sleep(1)


    def node_audit(self):
        # TODO: container sholud be relocated upon each case of performance metrics
        audit_dic = {}
        
        for hostname in self.node_dic.keys():
            measure_lst = self.node_dic[hostname]

            cpu_avg = np.average([x['cpu_cur'] for x in measure_lst[-30:]])
            mem_avg = np.average([x['mem_cur'] for x in measure_lst[-30:]])
            disk_avg = np.average([x['disk_cur'] for x in measure_lst[-30:]])
            
            cpu_audit_score = cpu_avg / float(measure_lst[0]['cpu_total']) 
            mem_audit_score = mem_avg / measure_lst[0]['mem_total'] 
            disk_audit_score = disk_avg / measure_lst[0]['disk_total'] 

            #print(hostname, cpu_audit_score, mem_audit_score, disk_audit_score)

            if cpu_audit_score > .9 or mem_audit_score > .9 or disk_audit_score > .9:
                audit_dic[hostname] = True

        return audit_dic

    def read_perf(self, hostname):
        measure_lst = self.node_dic[hostname]
        total_dic = {'cpu': measure_lst[0]['cpu_total'],
                'mem': measure_lst[0]['mem_total'],
                'disk': measure_lst[0]['disk_total']
        }

        cpu_avg = np.average([x['cpu_cur'] for x in measure_lst[-30:]])
        mem_avg = np.average([x['mem_cur'] for x in measure_lst[-30:]])
        disk_avg = np.average([x['disk_cur'] for x in measure_lst[-30:]])

        change_dic = {}
        for target in ['cpu', 'mem', 'disk']:
            abs_change = sum([abs(measure_lst[i+1]['%s_cur' % target] - measure_lst[i]['%s_cur' % target]) 
                for i in range(len(measure_lst) - 1)]) / (len(measure_lst) - 1) / total_dic[target]
            trend = sum([(measure_lst[i+1]['%s_cur' % target] - measure_lst[i]['%s_cur' % target]) 
                for i in range(len(measure_lst) - 1)]) / (len(measure_lst) - 1) / total_dic[target]

            if abs_change > 30:
                unstable = True
            else:
                unstable = False

            if trend < -20:
                trend_ret = -2
            elif trend < -10:
                trend_ret = -1
            elif trend < 10:
                trend_ret = 0
            elif trend < 20:
                trend_ret = 1
            else:
                trend_ret = 2

            change_dic[target] = [unstable, trend_ret]

        ret_dic = {'cpu_total': total_dic['cpu'], 'cpu_avg': cpu_avg, 
                'mem_total': total_dic['mem'], 'mem_avg': mem_avg, 
                'disk_total': total_dic['disk'], 'disk_avg': disk_avg, 'change_dic': change_dic}
        return ret_dic

    def get_read_cnt(self):
        return self.read_cnt


if __name__ == '__main__':
    node_monitor = NodeMonitor()
    #node_monitor.monitor_thread()
    perf = node_monitor.read_perf('manager2')
    print(perf)
    


