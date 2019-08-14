# -*- coding: utf-8 -*-

import docker

from common import *
from swarm import is_in_swarm


host_client = load_host_client()
host_swarm = host_client.info()['Swarm']


def get_nodes():
    nodes = host_client.nodes.list()
    return nodes


def get_hostname_by_id(node_id):
    node = host_client.nodes.get(node_id)
    return node.attrs['Description']['Hostname']


def _node_update_label(hostname, labels, delete=False):
    node = host_client.nodes.get(hostname)
    node_spec = node.attrs['Spec']
    node_spec['Name'] = node.attrs['Description']['Hostname']
    try:
        if not delete:
            node_spec['Labels'].update({x: '1' for x in labels})
        else:
            for label in labels:
                node_spec['Labels'].pop(label, None)

    except KeyError:
        if not delete:
            node_spec['Labels'] = {x: '1' for x in labels}

    node.update(node_spec)

def node_update_label():
    """
    Update node labels 
    """
    # TODO: Function to delete labels

    node_name_str = input('Node name? (separate by space) ')
    node_name_lst = node_name_str.split(' ')

    for node_name in node_name_lst:
        try:
            node = host_client.nodes.get(node_name)

        except docker.errors.NotFound:
            print(node_name + ' does not exist')
            return

    label_str = input('Label list? (separate by space) ')
    label_lst = label_str.split(' ')

    for node_name in node_name_lst:
        node = host_client.nodes.get(node_name)
        node_spec = node.attrs['Spec']
        node_spec['Name'] = node.attrs['Description']['Hostname']
        try:
            node_spec['Labels'].update({x: 'default' for x in label_lst})
        except KeyError:
            node_spec['Labels'] = {x: 'default' for x in label_lst}

        node.update(node_spec)


def node_update_role():
    """
    Update node role
    """
    # TODO: Need to support demote

    node_name_str = input('Node name? ')
    node_name_lst = node_name_str.split(' ')

    for node_name in node_name_lst:
        try:
            node = host_client.nodes.get(node_name)

        except docker.errors.NotFound:
            print(node_name + ' does not exist')
            return

    for node_name in node_name_lst:
        node = host_client.nodes.get(node_name)
        node_spec = node.attrs['Spec']
        node_spec['Name'] = node.attrs['Description']['Hostname']
        node_spec['Role'] = 'manager'

        node.update(node_spec)


def node_update_avail():
    """
    Update node role
    """
    # TODO: Function to modify multiple nodes simultaneously

    node_name_str = input('Node name? ')
    node_name_lst = node_name_str.split(' ')

    for node_name in node_name_lst:
        try:
            node = host_client.nodes.get(node_name)

        except docker.errors.NotFound:
            print(node_name + ' does not exist')
            return

    state = input('Desired state? ')
    
    if state not in ('active', 'pause', 'drain'):
        print('%s state is not supported')
        return

    for node_name in node_name_lst:
        node = host_client.nodes.get(node_name)
        node_spec = node.attrs['Spec']
        node_spec['Name'] = node.attrs['Description']['Hostname']
        node_spec['Availability'] = 'pause'

        node.update(node_spec)


def node_print(filters=None):
    """
    List nodes participating in the swarm.
    """

    if not is_in_swarm(host_client):
        print('This node is not the member of swarm')
        return -1

    node_lst = host_client.nodes.list(filters=filters)

    for node in node_lst:
        if node.attrs['Status']['State'] != 'down':
            print(node.attrs['Description']['Hostname'], node.attrs['Status']['Addr'])
            print(node.attrs['Spec'])
