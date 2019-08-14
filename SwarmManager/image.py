# -*- coding: utf-8 -*-

import docker

from common import *

host_client = load_host_client()
host_swarm = host_client.info()['Swarm']


def list_image(name=None, filters=None):
    """
    List images in the local docker daemon.
    """

    image_lst = host_client.images.list(name, filters)
    return image_lst



