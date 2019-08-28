# -*- coding: utf-8 -*-


# File to provide common functionalites

import os.path as op
from os.path import expanduser
import json
import docker

DOCKER_MACHINE_CONFIG_DIR = op.join(expanduser("~"), '.docker', 'machine', 'machines')

def find_matching_machine_url(machine_name, url_dic):
    """
    Finds the matching machine url from docker-machine
    with given address of the docker node

    Args:
        addr: Node address (ex. 192.177.10.1)
        url_lst: List of machine urls (ex. [tcp://192.177.10.1:3772])

    Return:
        machine URL (ex. tcp://192.177.10.1:3772) or -1
    """

    if machine_name in url_dic:
        return url_dic[machine_name]

    return -1


def load_host_client_env():
    with open('host_client.json', 'r') as fp:
        env_data = json.loads(fp.read())
    return env_data


def load_host_client():
    # env_data = load_host_client_env()
    host_client = docker.from_env()
    # host_client.login(username=env_data['username'], password=env_data['password'], registry=env_data['url'])

    return host_client

