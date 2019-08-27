# -*- coding: utf8 -*-

import os, os.path as op, sys
from os.path import expanduser
import time
import threading
import warnings
import subprocess as sp
import json
warnings.filterwarnings('ignore')
from itertools import groupby

sys.path.append(op.dirname(os.getcwd()))

import docker

from machine_wrapper import get_machine_urls, get_machine_ssh
from node import get_hostname_by_id
from perf.node_monitor import NodeMonitor
import service


class ServiceMonitor:
    def __init__(self, node_monitor):
        client = docker.from_env()
        self.services = client.services.list()
        self.machine_urls = get_machine_urls()
        self.node_monitor = node_monitor
        self.task_dic = {}
        self.first_run = False

        t = threading.Thread(target=self.monitoring_thread)
        t.start()

    def monitoring_thread(self):
        while True:
            task_lst = []
            for service in self.services:
                service_id = service.id
                tasks = service.tasks(filters={'desired-state': 'running'})

                for task in tasks:
                    node_id = task['NodeID']
                    hostname = get_hostname_by_id(node_id)
                    container_id = task['Status']['ContainerStatus']['ContainerID']

                    task_lst.append((hostname, service_id, container_id))
            
            print(task_lst)
            task_by_node = groupby(sorted(task_lst, key=lambda x: x[0]), key=lambda x: x[0])
            self.task_dic = {} 
            
            for hostname, tasks in task_by_node:
                cpu_total = self.node_monitor.read_perf(hostname)['cpu_total']
                #print(hostname, tasks)

                local = False

                try:
                    base_url = self.machine_urls[hostname]
                    base_path = op.join(expanduser('~'), '.docker', 'machine', 'machines', hostname)

                    client_cert = (op.join(base_path, 'cert.pem'), op.join(base_path, 'key.pem'))
                    tls_config = docker.tls.TLSConfig(
                            client_cert = client_cert,
                            ca_cert=op.join(base_path, 'ca.pem'))
                    #print(tls_config)

                    node_client = docker.DockerClient(base_url=base_url,
                            tls = tls_config)
                    node_api_client = docker.APIClient(base_url=base_url,
                            tls=tls_config)
                    ssh_url = get_machine_ssh(hostname)

                except KeyError:
                    node_client = docker.from_env()
                    node_api_client = docker.APIClient(base_url='unix://var/run/docker.sock')
                    local = True

                raw_containers = node_api_client.containers(size=True)
                sizes = {}
                for raw_container in raw_containers:
                    if 'SizeRw' in raw_container:
                        sizes[raw_container['Id']] = raw_container['SizeRw']
                    else:
                        sizes[raw_container['Id']] = 0
                
                for task in tasks:
                    service_id = task[1]
                    container_id = task[2]
                    #print(hostname, container_id)
                    con = node_client.containers.get(container_id)
                    stat = con.stats(stream=False)
                    pre_cpu = stat['precpu_stats']
                    cur_cpu = stat['cpu_stats']
                    cpu_usage_perc = 100 * (cur_cpu['cpu_usage']['total_usage'] - pre_cpu['cpu_usage']['total_usage']) / (cur_cpu['system_cpu_usage'] - pre_cpu['system_cpu_usage'])
                    cpu_usage = cpu_usage_perc * cpu_total 

                    mem_usage = stat['memory_stats']['usage']

                    disk_usage = sizes[container_id]

                        #ssh = sp.Popen(['ssh'] + ssh_url.split(' ') + [cmd], 
                        #        stdout=sp.PIPE)
                        #results = ssh.stdout.readlines()
                        #print(results)

                    self.task_dic[(service_id, hostname, container_id)] = [cpu_usage, mem_usage, disk_usage]
                    #print(hostname, cpu_usage, mem_usage, disk_usage)
            
            #print(task_dic)
            self.first_run = True

            time.sleep(30)

    def get_task_perf(self):
        while not self.first_run:
            time.sleep(1)
        return self.task_dic


if __name__ == '__main__':
    os.chdir(op.dirname(os.getcwd()))
    monitor = ServiceMonitor(NodeMonitor())
