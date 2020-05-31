# -*- coding: utf8 -*-

import os, os.path as op
from os.path import expanduser
import docker

from machine_wrapper import get_machine_urls

def get_client(node_id):
    host_client = docker.from_env()
    remote_node = host_client.nodes.list(filters={'id': node_id})[0]
    remote_node_hostname = remote_node.attrs['Description']['Hostname']
   
    try:
        remote_url = get_machine_urls()[remote_node_hostname]

    except KeyError:
        return docker.from_env()

    remote_tls_path = op.join(expanduser('~'), '.docker', 'machine', 'machines', remote_node_hostname)

    client_cert = (op.join(remote_tls_path, 'cert.pem'), op.join(remote_tls_path, 'key.pem'))
    tls_config = docker.tls.TLSConfig(
            client_cert=client_cert,
            ca_cert=op.join(remote_tls_path, 'ca.pem'))

    remote_client = docker.DockerClient(base_url=remote_url, tls=tls_config)

    return remote_client




