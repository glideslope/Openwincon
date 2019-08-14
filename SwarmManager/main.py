# -*- coding: utf-8 -*-

import os.path as op
from os.path import expanduser
import traceback

from pprint import pprint
import docker
import json

from machine_wrapper import *

from swarm import *
from service import ServiceModule 
from image import *
from node import *
from common import *


host_client = load_host_client()
host_swarm = host_client.info()['Swarm']

service_mod = ServiceModule()

def cmd_help():
    """
    List available commands.
    """

    print('Commands:')
    print('\thelp -- List commands')
    print('\tswarm -- Swarm operations')
    print('\tnode -- Node operations')
    print('\timage -- image operations')
    print('\tservice -- service operations')
    return


def cmd_swarm(cmd_args):
    if cmd_args[1] == 'init':
        swarm_start(host_client, 'enp1s0')

    elif cmd_args[1] == 'destroy':
        swarm_destroy()

    elif cmd_args[1] == 'join':
        swarm_join_nodes()

    elif cmd_args[1] == 'help':
        print('Commands:')
        print('\tinit: initiate docker swarm')
        print('\tjoin: join remote nodes to swarm')
        print('\tdestroy: remove all nodes from the swarm')
        print('\thelp: print commands')
        print()

    else:
        raise Exception('Wrong command')


def cmd_node(cmd_args):
    if cmd_args[1] == 'list' or cmd_args[1] == 'ls':
        node_print()

    elif cmd_args[1] == 'help':
        print('Commands:')
        print('\tlist: print current nodes')
        print('\tupdate (label | role | availability): update the node depends on the second argument')
        print('\thelp: print commands')

    elif cmd_args[1] == 'update' and cmd_args[2] == 'label':
        node_update_label()

    elif cmd_args[1] == 'update' and cmd_args[2] == 'role':
        node_update_role()

    elif cmd_args[1] == 'update' and cmd_args[2] == 'availability':
        node_update_avail()

    else:
        raise Exception('Wrong command')


def cmd_service(cmd_args):
    if cmd_args[1] == 'list':
        service_mod.service_print()

    elif cmd_args[1] == 'create':
        service_mod.service_create()

    elif cmd_args[1] == 'delete':
        service_mod.service_delete()

    elif cmd_args[1] == 'update':
        service_mod.service_update()

    elif cmd_args[1] == 'help':
        print('Command:')
        print('\tcreate: create service')
        print('\tdelete: delete service')
        print('\tlist: list services')
        print('\thelp: print commands')

    else:
        raise Exception('Wrong command')

def main():
    print('OpenWinCon Docker controller v0.1')
    print('')

    while True:
        try:
            cmd = input('command> ')

        except (KeyboardInterrupt, EOFError):
            print('\nQuitting program...')
            break

        cmd_args = cmd.split(' ')

        try:
            if cmd == 'help':
                cmd_help()

            elif cmd_args[0] == 'swarm':
                cmd_swarm(cmd_args)
 
            elif cmd_args[0] == 'node':
                cmd_node(cmd_args)

            elif cmd_args[0] == 'service':
                cmd_service(cmd_args)

            elif cmd_args[0] == 'image':
                if cmd_args[1] == 'push':
                    image_remote_push()

                elif cmd_args[1] == 'help':
                    print('Command:')
                    print('\tpush: push image in the remote node')
                    print('\thelp: print commands')

            elif cmd_args[0] == 'quit' or cmd_args[0] == 'exit':
                print('Qutting program...')
                break

        except IndexError as e:
            print('Input is miss-configured')
            traceback.print_exc()
            continue

        except Exception as e:
            print('Error: ' + str(e))
            continue


if __name__ == '__main__':
    main()

