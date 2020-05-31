# -*- coding: utf8 -*-

import os, sys
sys.path.append('..')
import requests as req
import json

import docker
from flask import Flask, render_template, request

from client import get_client
from swarm import swarm_start, swarm_destroy, swarm_join_nodes, is_in_swarm
from node import _node_update_label

app = Flask(__name__)
host_client = docker.from_env()


@app.route('/')
def base_page():
    nodes, services = [], []

    if is_in_swarm(host_client):
        nodes = host_client.nodes.list()
        services = host_client.services.list()
    return render_template('base.html', message='Main page', nodes=nodes, services=services)


@app.route('/swarm/init')
def gui_swarm_init():
    if is_in_swarm(host_client):
        nodes = host_client.nodes.list()
        services = host_client.services.list()
        return render_template('base.html', message='Already activated', nodes=nodes, services=services)

    swarm_start(host_client, 'enp1s0')
    nodes = host_client.nodes.list()
    services = host_client.services.list()

    return render_template('base.html', message='Swarm initiated', nodes=nodes, services=services)


@app.route('/swarm/join')
def gui_swarm_join():
    swarm_join_nodes('../remote_nodes.json')

    nodes = host_client.nodes.list()
    services = host_client.services.list()

    return render_template('base.html', message='Node joined to the swarm', nodes=nodes, services=services)


@app.route('/swarm/destroy')
def gui_swarm_destroy():
    if not is_in_swarm(host_client):
        return render_template('base.html', message='Swarm does not exist, cannot be destroyed')

    swarm_destroy()
    return render_template('base.html', message='Swarm destroyed')


@app.route('/node/<node_id>')
def node_info(node_id):
    node_client = get_client(node_id)
    containers = node_client.containers.list()

    resp = req.get('http://127.0.0.1:42665/perf_lst/%s' % node_id)
    lst = json.loads(resp.text)

    cpu_values = [x['cpu_cur'] for x in lst]
    mem_values = [x['mem_cur'] for x in lst]
    disk_values = [x['disk_cur'] for x in lst]

    labels = list(range(0, 60))
    return render_template('node_info.html', message='Node %s information' % node_id, node_id=node_id,
            labels=labels, 
            display_container=True,
            cpu_values=cpu_values, mem_values=mem_values, disk_values=disk_values,
            containers=containers)


@app.route('/node/<node_id>/<container_id>')
def node_service_info(node_id, container_id):
    resp = req.get('http://127.0.0.1:42665/node_con_perf/%s/%s' % (node_id, container_id))
    lst = json.loads(resp.text)

    cpu_values = [x[0] for x in lst]
    mem_values = [x[1] for x in lst]
    disk_values = [x[2] for x in lst]

    # print(lst)
    return render_template('node_info.html', 
            message='Performance Graph: %s - %s' % (node_id[:10], container_id[:10]), 
            display_container=False,
            labels = list(range(0, 60*5, 5)),
            cpu_values=cpu_values, mem_values=mem_values, disk_values=disk_values
            )
    

@app.route('/service/create', methods=["GET", "POST"])
def service_create():
    if request.method == 'GET':
        return render_template('service_create.html', message='Creating a service')

    elif request.method == 'POST':
        form = request.form
        serv_name = form['service_name']
        image_name = form['image_name']
        image_name = registry_addr + '/' + image_name

        serv_cmd = form['service_cmd']
        if serv_cmd == '':
            serv_cmd = 'tail -f /dev/null'

        serv_mod = form['service_mod']
        if serv_mod == 'replicated':
            num_replica = form['num_replica']
            service_mode = docker.types.ServiceMode('replicated', int(num_replica))

        elif serv_mod == 'global' or serv_mod == '':
            service_mode = docker.types.ServiceMode('global')

        serv_role = form['service_role']
        service_constraints = []
        if serv_role != '':
            service_constraints.append('node.role==%s' % service_role)

        cpu_req = form['cpu_req']
        if cpu_req == '':
            cpu_req = 1000
        mem_req = form['mem_req']
        if mem_req == '':
            mem_req = 1000000000
        disk_req = form['disk_req']
        if disk_req == '':
            disk_req = 1000000000

        serv_spec = {'cpu': cpu_req, 'mem': mem_req, 'disk': disk_req, 'cnt': num_replica}

        if service_mode != 'global':
            #alloc_lst = self.s_alloc.allocate(service_spec, service_cnt)
            resp = req.post('http://127.0.0.1:42665/alloc', data=json.dumps(serv_spec))
            alloc_lst = json.loads(resp.text)
            print(alloc_lst)

        else:
            alloc_lst = None

        #service_constraints += service_label_lst
        serv_spec = {'cpu': str(cpu_req), 'mem': str(mem_req), 'disk': str(disk_req)}

        try:
            if alloc_lst is None:
                service = host_client.services.create(
                        image=image_name,
                        labels=service_spec,
                        command=serv_cmd,
                        name=serv_name,
                        mode=serv_mode,
                        constraints=service_constraints
                        )
            else:
                #print(alloc_lst)
                #alloc_lst = ['mmlab', 'manager2', 'node2']
                for i, hostname in enumerate(alloc_lst):
                    _node_update_label(hostname, [serv_name + '-%d' % i])
                    service = host_client.services.create(
                            image=image_name,
                            labels=serv_spec,
                            command=serv_cmd,
                            name=(serv_name + '-%d' % i),
                            constraints=['node.labels.%s==1' % (serv_name + '-' + str(i))]
                    )

        except docker.errors.APIError as e:
            print(e)
            nodes = host_client.nodes.list() 
            services = host_client.services.list()
            return render_template('base.html', message='Failed to create the service', nodes=nodes, services=services)

        nodes = host_client.nodes.list() 
        services = host_client.services.list()

        return render_template('base.html', message='Service %s created' % serv_name, nodes=nodes, services=services) 
        

@app.route('/service/remove/<service_id>')
def service_remove(service_id):
    try:
        service = host_client.services.get(service_id)

    except:
        nodes = host_client.nodes.list()
        services = host_client.services.list()
        message = 'Service already removed'
        return render_template('base.html', message=message, nodes=nodes, services=services)


    if service.attrs['Spec']['Name'] == 'perf_monitor':
        nodes = host_client.nodes.list()
        services = host_client.services.list()
   
        return render_template('base.html', message='Performance monitor cannot be removed', 
                nodes=nodes, services=services)

    service.remove()
    message = 'Service %s deleted' % service_id

    nodes = host_client.nodes.list()
    services = host_client.services.list()

    return render_template('base.html', message=message, nodes=nodes, services=services)


@app.route('/service/list')
def service_list():
    return None


if __name__ == '__main__':
    app.config['TEMPLATES_AUTO_RELOAD'] = True
    app.run(host='0.0.0.0', port=6602)
