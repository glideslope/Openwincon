# -*- coding: utf-8 -*-

import time
import threading
import docker

from perf.node_monitor import NodeMonitor
from perf.static_allocator import StaticServiceAllocator
from perf.service_monitor import ServiceMonitor
from node import _node_update_label

class ServiceModule:
    def __init__(self):
        self.host_client = docker.from_env()
        self.node_monitor = NodeMonitor()
        self.service_monitor = ServiceMonitor(self.node_monitor)
        self.s_alloc = StaticServiceAllocator(self.node_monitor)
                
        audit_thr = threading.Thread(target=self.audit_thread)
        audit_thr.start()

    def audit_thread(self):
        while True:
            audit_dic = self.node_monitor.node_audit()

            if len(audit_dic) == 0:
                #print('Audit check')
                pass
            
            else:
                print(audit_dic)
                
            #audit_dic = {'mmlab': True}
            for hostname, _ in audit_dic.items():
                self.relocate(hostname)

            time.sleep(1)
                

    def service_create(self, cmd=None):
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

        service_lst = list_service()

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

    def relocate(self, hostname):
        perf_dic = self.node_monitor.read_perf(hostname)
        task_dic = self.service_monitor.get_task_perf()

        total_score_dic = {}
        for k in filter(lambda x: x[1] == hostname, task_dic):
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
        new_hostname = self.s_alloc.allocate(target_service_spec)[0]
        target_service_name = target_service.attrs['Spec']['Name']

        _node_update_label(hostname, [target_service_name], True)
        _node_update_label(new_hostname, [target_service_name])


    def list_service(self, filters=None):
        """
        List currently active services.
        """

        service_lst = self.host_client.services.list(filters=filters)
        return service_lst

