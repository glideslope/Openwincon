# -*- coding: utf8 -*-


import os, sys
sys.path.append(os.path.dirname(os.getcwd()))

import node
from perf.node_monitor import NodeMonitor


class StaticServiceAllocator:
    def __init__(self, node_monitor):
        self.node_monitor = node_monitor 
        self.nodes = node.get_nodes()

    def allocate(self, service_spec, replicas=1):
        """
        """
        node_dic = {}
        for node in self.nodes:
            hostname = node.attrs['Description']['Hostname']
            node_perf = self.node_monitor.read_perf(hostname)
            node_dic[hostname] = node_perf

        task_nodes = []
        while replicas > 0:
            node_scores = []
            for node_hostname, node_perf in node_dic.items():
                try:
                    cpu_score = service_spec['cpu'] / (node_perf['cpu_total'] - node_perf['cpu_avg'])
                    mem_score = service_spec['mem'] / (node_perf['mem_total'] - node_perf['mem_avg'])
                    disk_score = service_spec['disk'] / (node_perf['disk_total'] - node_perf['disk_avg'])

                    node_score = cpu_score + mem_score + disk_score
                    print(node_hostname, node_score)

                except ZeroDivisionError:
                    continue

                if node_score > 1:
                    continue

                for target in ['cpu', 'mem', 'disk']:
                    if node_perf['change_dic'][target][0]:
                        node_score *= 1.2
                    
                    if node_perf['change_dic'][target][1] < 0:
                        node_score *= .9
                    elif node_perf['change_dic'][target][1] > 0:
                        node_score *= 1.1

                node_scores.append((node_hostname, node_score))

            sorted_node_scores = list(sorted(node_scores, key=lambda x: x[1]))
            target_node = sorted_node_scores[0][0]

            node_dic[target_node]['cpu_avg'] += service_spec['cpu']
            node_dic[target_node]['mem_avg'] += service_spec['mem']
            node_dic[target_node]['disk_avg'] += service_spec['disk']

            task_nodes.append(target_node)
            replicas -= 1

        return task_nodes

            
if __name__ == '__main__':
    allocator = StaticServiceAllocator()
    nodes = allocator.allocate({'cpu': 500, 'mem': 400, 'disk': 300}, 5)
    print(nodes)
