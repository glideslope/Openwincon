# -*- coding: utf-8 -*-

import os, sys
import os.path as op
from os.path import expanduser
import socket
import time
import threading
import http
import docker
import json
import subprocess as sp
import ast
from urllib.parse import urlparse

from machine_wrapper import *
from perf.node_monitor import NodeMonitor
from perf.static_allocator import StaticServiceAllocator
from perf.service_monitor import ServiceMonitor
from node import _node_update_label

HOME_DIR = expanduser('~')

def handler_factory(serv_mod):
    class ServReqHandler(http.server.SimpleHTTPRequestHandler):
        def do_GET(self):
            parsed_path = urlparse(self.path).path.split('/')[1:]
            # print(parsed_path)
            if 'read_perf' in parsed_path[0]:
                perf_metrics = serv_mod.node_monitor.read_perf(parsed_path[1])
                # print(perf_metrics)
                if perf_metrics is None:
                    self.send_response(404)
                    self.end_headers()

                else:
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps(perf_metrics).encode())

            elif 'perf_lst' in parsed_path[0]:
                perf_lst = serv_mod.node_monitor.get_perf_lst(parsed_path[1])

                if perf_lst is None:
                    self.send_response(404)
                    self.end_headers()

                else:
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps(perf_lst).encode())

            elif 'task_perf' in parsed_path[0]:
                task_dic = self.service_monitor.get_task_perf()
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(task_dic).encode())

            elif 'node_con_perf' in parsed_path[0]:
                node_id = parsed_path[1]
                container_id = parsed_path[2]
                con_perf_lst = serv_mod.service_monitor.get_task_perf(node_id, container_id)
                print(con_perf_lst)
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(con_perf_lst).encode())

        def do_POST(self):
            parsed_path = urlparse(self.path).path.split('/')[1:]
            if 'alloc' in parsed_path[0]:
                content_length = int(self.headers['Content-Length'])
                post_data = json.loads(self.rfile.read(content_length).decode())
                num_replica = int(post_data.pop('cnt'))
                alloc_lst = serv_mod.s_alloc.allocate(post_data, num_replica)

                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(alloc_lst).encode())

    return ServReqHandler


class ServiceModule:
    def __init__(self, registry_addr, manager_addr, node_monitor_port, daemon=True):
        if daemon:
            self.daemon_mode()
        self._prep(node_monitor_port)

    def daemon_mode(self):
        sock = socket.socket()
        try:
            sock.bind(('127.0.0.1', 42665))

        except OSError:
            exit(-1)

        pid = os.fork()
        if pid > 0:
            # parent procrss
            # just exit
            exit(0)
        else:
            # decouple from parent environment
            os.chdir('/')
            os.setsid()
            os.umask(0)

            # second fork
            pid = os.fork()
            if pid > 0:
                exit(0)
            else:
                sys.stdout.flush()
                sys.stderr.flush()

                si = open(os.devnull, 'r')
                so = open(op.join(HOME_DIR, 'Openwincon/SwarmManager/err_service.txt'), 'a+')
                se = open(op.join(HOME_DIR, 'Openwincon/SwarmManager/err_service.txt'), 'a+')

                os.dup2(si.fileno(), sys.stdin.fileno())
                os.dup2(so.fileno(), sys.stdout.fileno())
                os.dup2(se.fileno(), sys.stderr.fileno())

    def _prep(self, node_monitor_port):
        while True:
            cmd = 'docker node inspect self'
            cmd = cmd.split(' ')
            cmd = [x for x in cmd if x != '']

            proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
            out, err = proc.communicate()
            err_code = proc.returncode
           
            result = out.decode().replace('true', 'True')
            self_dic = ast.literal_eval(result)[0]
            if 'Leader' not in self_dic['ManagerStatus']:
                print('Not leader... waiting...')
                time.sleep(1)

            else:
                break

        print('Starting service module...')

        reset_service_on_change()
        register_machine_on_start()

        self.host_client = docker.from_env()
        self.node_monitor = NodeMonitor(node_monitor_port)
        self.service_monitor = ServiceMonitor(self.node_monitor)
        self.s_alloc = StaticServiceAllocator(self.node_monitor)        

        audit_thr = threading.Thread(target=self.audit_thread)
        audit_thr.start()
        # audit_thr.join()

        req_handle_thr = threading.Thread(target=self.serve_forever)
        req_handle_thr.start()

    def audit_thread(self):
        while True:
            audit_dic = self.node_monitor.node_audit()

            if len(audit_dic) == 0:
                #print('Audit check')
                pass
            
            else:
                print(audit_dic)
                
            #audit_dic = {'mmlab': True}
            for node_id, _ in audit_dic.items():
                self.relocate(node_id)

            time.sleep(1)

    def serve_forever(self):
        handler = handler_factory(self)
        httpd = http.server.HTTPServer(('127.0.0.1', 42665), handler)
        httpd.serve_forever()

    def relocate(self, node_id):
        # TODO: Hostname --> node id
        resp = req.get('http://127.0.0.1:42665/read_perf/%s' % node_id)
        perf_dic = json.loads(resp.text)
        task_dic = self.service_monitor.get_task_perf()

        total_score_dic = {}
        for k in filter(lambda x: x[1] == node_id, task_dic):
            task_perf = task_dic[k]
            cpu_score = task_perf[0] / float(perf_dic['cpu_total'])
            mem_score = task_perf[1] / float(perf_dic['mem_total'])
            disk_score = task_perf[2] / float(perf_dic['disk_total'])

            total_score = cpu_score + mem_score + disk_score
            total_score_dic[(k[0], k[2])] = total_score

        reloc_serv = list(sorted(total_score_dic.items(), key=lambda x: x[1], reverse=True))[0]
        reloc_serv_id = reloc_serv[0][0]
        target_service = self.host_client.services.get(reloc_serv_id)
        target_service_spec = {
                'cpu': float(target_service.attrs['Spec']['Labels']['cpu']),
                'mem': float(target_service.attrs['Spec']['Labels']['mem']),
                'disk': float(target_service.attrs['Spec']['Labels']['disk'])
        }
        new_node_id = self.s_alloc.allocate(target_service_spec)[0]
        target_service_name = target_service.attrs['Spec']['Name']

        _node_update_label(node_id, [target_service_name], True)
        _node_update_label(new_node_id, [target_service_name])


class ServiceHelper:
    def service_create(self, cmd=None, cli=True):
        """
        Create service under given conditions.
        """

        image_name = input('Image name? ')
        service_name = input('Service name? ')

        service_cmd = input('Service cmd? (Default: tail -f /dev/null) ')
        if service_cmd == '':
            service_cmd = 'tail -f /dev/null'

        service_mode = input('Service mode? (Default: replicated) ')
        if service_mode == '' or service_mode == 'replicated':
            service_cnt = input('How many? (Default: 1) ')
            if service_cnt != '':
                service_cnt = int(service_cnt)
            else:
                service_cnt = 1
            service_mode = docker.types.ServiceMode('replicated', int(service_cnt))

        elif service_mode == 'global':
            service_mode = docker.types.ServiceMode('global')

        else:
            print('Wrong service mode')
            return

        service_constraints = []
        service_role = input('Service role? (Default: All) ')
        if service_role != '':
            if service_role in ['worker', 'manager']:
                service_constraints.append('node.role==%s' % service_role)
            
            else:
                return

        service_spec_cpu = input('CPU requirement? (Default: 1GHz) ')
        if service_spec_cpu == '':
            service_spec_cpu = 1000
        service_spec_mem = input('Memory requirement? (Default: 1GB) ')
        if service_spec_mem == '':
            service_spec_mem = 1000000000
        service_spec_disk = input('Disk requirement? (Default: 1GB) ')
        if service_spec_disk == '':
            service_spec_disk = 1000000000
        service_spec = {'cpu': service_spec_cpu, 'mem': service_spec_mem, 'disk': service_spec_disk} 

        if service_mode != 'global':
            alloc_lst = self.s_alloc.allocate(service_spec, service_cnt)

        else:
            alloc_lst = None

        service_label_lst = []
        '''
        service_label_str = input('Label constarint (multiple labels separated by space or press Enter to pass) ')

        if service_label_str != '':
            service_label_lst = service_label_str.split(' ')
            service_label_lst = ['node.labels.' + x + '==default' for x in service_label_lst]
        '''

        service_constraints += service_label_lst
        service_spec = {'cpu': str(service_spec_cpu), 
                'mem': str(service_spec_mem), 
                'disk': str(service_spec_disk)}

        try:
            if alloc_lst is None:
                service = self.host_client.services.create(
                        image=image_name,
                        labels=service_spec,
                        command=service_cmd,
                        name=service_name,
                        mode=service_mode,
                        constraints=service_constraints
                        )
            else:
                #print(alloc_lst)
                #alloc_lst = ['mmlab', 'manager2', 'node2']
                for i, hostname in enumerate(alloc_lst):
                    _node_update_label(hostname, [service_name + '-%d' % i])
                    service = self.host_client.services.create(
                            image=image_name,
                            labels=service_spec,
                            command=service_cmd,
                            name=(service_name + '-%d' % i),
                            constraints=['node.labels.%s==1' % (service_name + '-' + str(i))]
                    )

        except docker.errors.APIError as e:
            print(e)
            return -1

        return 0 

    def service_print(self):
        """
        Print currently active services
        """

        service_lst = self.list_service()

        print('Current Services')
        print('=======================================')

        for service in service_lst:
            serv_name = service.attrs['Spec']['Name']
            serv_img_name = service.attrs['Spec']['TaskTemplate']['ContainerSpec']['Image'].split('@')[0]
            serv_mode = list(service.attrs['Spec']['Mode'].keys())[0]
            
            if serv_mode == 'Replicated':
                serv_replicas = service.attrs['Spec']['Mode'][serv_mode]['Replicas']

            else:
                serv_replicas = '-'

            serv_created_at = service.attrs['CreatedAt']

            print(serv_name, serv_img_name, serv_mode, serv_replicas, serv_created_at)

    def service_delete(self):
        """
        Delete service with its name
        """

        service_name = input('Service name? ')
        service_lst = self.host_client.services.list(filters={'name': service_name})

        if len(service_lst) == 0:
            print('%s service does not exist' % service_name)
            return

        else: 
            for service in service_lst:
                service.remove()

        print('%s service removed' % service_name)

    def service_update(self):
        """
        Update service with given conditions
        """

        service_name = input('Service name? ')
        service_lst = self.host_client.services.list(filters={'name': service_name})

        if len(service_lst) == 0:
            print('%s service does not exist' % service_name)

        elif len(service_lst) == 1:
            service = service_lst[0]

        update_target = input('Which update? ')

        if update_target == 'scale':
            if list(service.attrs['Spec']['Mode'].keys())[0] != 'Replicated':
                print('Current mode is not replicated')
                return

            replica_cnt = input('How many? ')

            try:
                replica_cnt = int(replica_cnt)

            except ValueError:
                print('Not a number')
                return

            if replica_cnt < 0 or replica_cnt > 1000:
                print('Too small or too large replica count')
                return

            service.update(mode=docker.types.ServiceMode('replicated', replica_cnt))

        #elif update_target == 'mode':
        #    service_mode == input('Mode? ')

        #    if service_mode not in ('replicated', 'global'):
        #        print('%s mode is not supported' % service_mode)
        #        return

            # TODO: Need replica count if service mode is replicated

        #    service.update(docker.types.ServiceMode(service_mode, 1))


    def list_service(self, filters=None):
        """
        List currently active services.
        """

        service_lst = self.host_client.services.list(filters=filters)
        return service_lst


def reset_service_on_change():
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    manager_addr = s.getsockname()[0]
    node_monitor_port = 42664
    s.close()
    
    host_client = docker.from_env()
    for service in host_client.services.list():
        service.update(env=['manager_addr=%s' % manager_addr, 'manager_port=%s' % node_monitor_port])
        # service.force_update()


def register_machine_on_start(filepath=None):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    manager_addr = s.getsockname()[0]

    if filepath is None:
        filepath = op.join(HOME_DIR, 'Openwincon/SwarmManager', 'remote_nodes.json')

    host_client = docker.from_env()

    data = open(filepath).read()
    json_data = json.loads(data)
    
    remote_nodes = json_data['nodes']
    machine_url_lst = get_machine_urls()

    join_token_manager = host_client.swarm.attrs['JoinTokens']['Manager']
    join_token_worker = host_client.swarm.attrs['JoinTokens']['Worker']

    for remote_node in remote_nodes:
        node_name = remote_node['name']
        node_addr = remote_node['addr']

        if node_addr == manager_addr:
            continue

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
            register_machine(remote_node)


if __name__ == '__main__':
    inst = ServiceModule(LOCAL_ADDR, LOCAL_ADDR, LOCAL_PORT, False)
