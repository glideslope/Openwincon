# -*- coding: utf-8 -*-

import docker
import json

from machine_wrapper import * 
from common import *


host_client = load_host_client()


def is_in_swarm(client):
    swarm_dic = client.info()['Swarm']
    active_state = swarm_dic['LocalNodeState']

    if active_state == 'inactive':
        return False

    else:
        return True


def swarm_start(client, advertise_addr='eth0'):
    '''
    Start swarm with given advertise interface (or the address)
    '''

    if is_in_swarm(client):
        print('Swarm init failed. This node is already part of the swarm.')
        return -1

    client.swarm.init(advertise_addr=advertise_addr)
    print('Swarm initiated as %s' % advertise_addr)

    #TODO: Manager address 처리
    manager_addr = registry_addr = LOCAL_ADDR
    node_monitor_port = 42664
    service = host_client.services.create(
            image=registry_addr + '/perf',
            name='perf_monitor',
            mode='global',
            env=['manager_addr=%s' % manager_addr, 'manager_port=%s' % node_monitor_port],
            hostname='{{ .Node.ID }}'
    )

    return 0 


def swarm_destroy():
    '''
    Remove all nodes from swarm
    '''

    manager_node_lst = host_client.nodes.list(filters={'role': 'manager'})
    worker_node_lst = host_client.nodes.list(filters={'role': 'worker'})
    machine_url_lst = get_machine_urls()
    current_node = None

    for node in manager_node_lst:
        node_addr = node.attrs['Status']['Addr']

        if node_addr == '127.0.0.1':
            current_node = node

    if current_node is not None:
        manager_node_lst.remove(current_node)

    for node in worker_node_lst + manager_node_lst:
        node_addr = node.attrs['Status']['Addr']
        node_name = node.attrs['Description']['Hostname']

        machine_url = find_matching_machine_url(node_name, machine_url_lst)
        if machine_url == -1:
            print('There is no matching machine with node (%s, %s)' % (node_name, node_addr))

        tls_config = docker.tls.TLSConfig(
                client_cert=(op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/cert.pem" % node_name),
                    op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/key.pem" % node_name)),
                ca_cert=op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/ca.pem" % node_name),
                verify=True
        )

        machine_client = docker.DockerClient(base_url=machine_url, tls=tls_config)
        machine_client.swarm.leave(force=True)

        print('%s node removed. (addr: %s)' % (node_name, machine_url))

    host_client.swarm.leave(force=True)

    print('Successfully removed all nodes from swarm')

    return 0


def swarm_join_nodes(filepath=None):
    """
    Join remote nodes based on remote_nodes.json

    """
    # TODO: Join nodes based on their roles (manager or worker)

    if not is_in_swarm(host_client):
        swarm_start(host_client, 'enp1s0')

    if filepath is None:
        filepath = 'remote_nodes.json'

    data = open(filepath).read()
    json_data = json.loads(data)
    
    remote_nodes = json_data['nodes']
    machine_url_lst = get_machine_urls()

    join_token_manager = host_client.swarm.attrs['JoinTokens']['Manager']
    join_token_worker = host_client.swarm.attrs['JoinTokens']['Worker']

    for remote_node in remote_nodes:
        node_name = remote_node['name']
        node_addr = remote_node['addr']

        if 'ssh_port' not in remote_node:
            remote_node['ssh_port'] = 22

        if 'engine_port' not in remote_node:
            remote_node['engine_port'] = 2376

        if 'engine' not in remote_node:
            remote_node['engine'] = 'aufs'

        if 'role' not in remote_node or remote_node['role'] == 'worker':
            join_token = join_token_worker            
        else:
            join_token = join_token_manager

        if not is_machine_registered(node_name):
            if register_machine(remote_node) == -1:
                print('Machine register failed: ' + str(remote_node))
                return 
            
        machine_url_lst = get_machine_urls()
        machine_url = find_matching_machine_url(node_name, machine_url_lst)

        tls_config = docker.tls.TLSConfig(
                client_cert=(op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/cert.pem" % node_name),
                    op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/key.pem" % node_name)),
                ca_cert=op.join(DOCKER_MACHINE_CONFIG_DIR, "%s/ca.pem" % node_name),
                verify=True
        )

        machine_client = docker.DockerClient(base_url=machine_url, tls=tls_config)

        machine_client.swarm.join(
                remote_addrs=[host_client.info()['Swarm']['RemoteManagers'][0]['Addr']],
                join_token=join_token
        )

        if join_token == join_token_manager:
            install_docker_machine_remote(node_name)
            run_service_module(node_name)
            
        print('%s node joined the swarm. (addr: %s)' % (node_name, machine_url))


