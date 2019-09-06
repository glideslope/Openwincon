# -*- coding: utf-8 -*-

import os.path as op
from os.path import expanduser
from cmd import Cmd
import traceback

from pprint import pprint
import docker
import json

from machine_wrapper import *
from swarm import *
from image import *
from node import *
from common import *
from service import ServiceHelper


host_client = load_host_client()
host_swarm = host_client.info()['Swarm']

service_helper = None

class CmdShell(Cmd):
    service_helper = ServiceHelper()
    prompt = 'command> '
    intro = 'Starting docker swarm manager'

    def do_exit(self, inp):
        """
        Terminate the program
        """

        print('Qutting program...')
        return True

    def do_list(self, inp):
        """
        List available commands.
        """
        print('Commands:')
        print('\thelp -- List commands')
        print('\tswarm -- Swarm operations')
        print('\tnode -- Node operations')
        print('\timage -- image operations')
        print('\tservice -- service operations')

    def do_swarm(self, inp):
        """
        init: initiate docker swarm on this node
        join: join remote nodes to swarm
        destroy: remove all nodes from the swarm
        """
        if inp == 'init':
            swarm_start(host_client, 'enp1s0')
        elif inp == 'destroy':
            swarm_destroy()
        elif inp == 'join':
            swarm_join_nodes()
        else:
            print('Wrong command')


    def do_node(self, inp):
        """
        list: print current nodes
        update (label | role | availability): update the node depends on the second argument
        """
        cmd_args = inp.split(' ')
        if cmd_args[0] == 'list' or cmd_args[0] == 'ls':
            node_print()

        elif cmd_args[0] == 'update' and cmd_args[1] == 'label':
            node_update_label()

        elif cmd_args[0] == 'update' and cmd_args[1] == 'role':
            node_update_role()

        elif cmd_args[0] == 'update' and cmd_args[1] == 'availability':
            node_update_avail()

        else:
            print('Wrong command')

    def do_service(self, inp):
        """
        create: create service
        delete: delete service
        list: list services
        """
        if inp == 'list':
            service_helper.service_print()

        elif inp == 'create':
            service_helper.service_create()

        elif inp == 'delete':
            service_helper.service_delete()

        elif inp == 'update':
            service_helper.service_update()

        else:
            print('Wrong command')

    do_EOF = do_exit


def get_monitor_port():
    return 42664
    '''
    import random, socket
    retry_cnt = 0
    while True:
        port_num = random.randint(10000, 50000)
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        result = sock.connect_ex(('127.0.0.1', port_num))
        if result != 98:
            sock.close()
            return port_num

        retry_cnt += 1
        if retry_cnt > 100:
            raise Exception('Monitoring server port assign failed')
    '''


def main():
    global service_helper

    print('OpenWinCon Docker controller v0.1')
    print('')

    # TODO: Host address, registry address 획득 로직 필요
    host_addr = LOCAL_ADDR 
    #if is_in_swarm(host_client):
    monitor_port = get_monitor_port()
    service_helper = ServiceHelper()

    cshell = CmdShell()
    cshell.cmdloop()

    '''
            elif cmd_args[0] == 'image':
                if cmd_args[1] == 'push':
                    image_remote_push()

                elif cmd_args[1] == 'help':
                    print('Command:')
                    print('\tpush: push image in the remote node')
                    print('\thelp: print commands')

        except IndexError as e:
            print('Input is miss-configured')
            traceback.print_exc()
            continue

        except Exception as e:
            print('Error: ' + str(e))
            continue
    '''

if __name__ == '__main__':
    main()

