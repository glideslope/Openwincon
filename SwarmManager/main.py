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
from service import ServiceModule


host_client = load_host_client()
host_swarm = host_client.info()['Swarm']

#service_mod = ServiceModule()

class CmdShell(Cmd):
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
            service_mod.service_print()

        elif inp == 'create':
            service_mod.service_create()

        elif inp == 'delete':
            service_mod.service_delete()

        elif inp == 'update':
            service_mod.service_update()

        else:
            print('Wrong command')

    do_EOF = do_exit


def main():
    print('OpenWinCon Docker controller v0.1')
    print('')

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

