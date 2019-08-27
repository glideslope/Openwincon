# -*- coding: utf8 -*-

from collections import defaultdict as dd
import subprocess as sp
import docker
import json
import os, sys

sys.path.append(os.path.dirname(os.getcwd()))
from machine_wrapper import *
from swarm import is_in_swarm


def find_matching_node(remote_nodes, hostname):
    for remote_node in remote_nodes:
        if remote_node['name'] == hostname:
            return remote_node


with open(os.path.dirname(os.getcwd()) + '/remote_nodes.json') as fp:
    remote_nodes = json.loads(fp.read())

host_client = docker.from_env()

node_lst = host_client.nodes.list(filters=None)
machine_urls = get_machine_urls()

for node in node_lst:
    hostname = node.attrs['Description']['Hostname']
    if hostname == 'mmlab':
        continue

    target_node = find_matching_node(remote_nodes['nodes'], hostname)
    target_url = target_node['addr']
    target_path = target_node['user'] + '@' + target_url + ':/etc/docker/perf.csv'

    cmd = ['scp', '-P', str(target_node['ssh_port']), target_path, 'csv/perf_%s.csv' % hostname]

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()


perf_dic = dd(lambda: [])
for node in node_lst:
    hostname = node.attrs['Description']['Hostname']
    if hostname == 'mmlab':
        continue

    target_node = find_matching_node(remote_nodes['nodes'], hostname)
    with open('perf_%s.csv' % hostname, 'r') as fp:
        lines = fp.readlines()

    for line in lines:
        perf_dic[hostname].append(list(map(float, line[:-1].split(','))))

