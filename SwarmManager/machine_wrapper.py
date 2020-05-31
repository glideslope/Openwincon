# -*- coding: utf-8 -*-

import pexpect
import subprocess as sp
import os, os.path as op
from os.path import expanduser
import json

from pprint import pprint


HOME_DIR = expanduser('~')


def get_machine_ssh(hostname):
    cmd = ('docker-machine inspect ' + hostname).split(' ') 
    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()
    
    result = out.decode('utf-8')
    driver = json.loads(result)['Driver']
    ssh_url = '%s@%s -p %s' % (driver['SSHUser'], driver['IPAddress'], driver['SSHPort'])
    return ssh_url


def get_machine_urls():
    """
    Retrieve URLs of docker-machine nodes

    Return:
        List of URL (type 'str')
    """

    cmd = 'docker-machine ls -f {{.Name}},{{.URL}}'.split(' ')
    
    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()
    err_code = proc.returncode

    lst = out.decode('utf-8').split('\n')

    if lst[-1] == '':
        del lst[-1]

    machine_url_dic = {}
    for elt in lst:
        pair = elt.split(',')
        machine_url_dic[pair[0]] = pair[1]

    return machine_url_dic 


def is_machine_registered(name):
    """

    """

    cmd = 'docker-machine ls --filter name=' + name
    cmd = cmd.split(' ')

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()
    err_code = proc.returncode

    lst = out.decode('utf-8').split('\n')

    if len(lst) == 2:
        return False

    else:
        return True


def is_docker_machine_installed(node_name):
    remote_cmd = 'docker-machine version'

    cmd = "docker-machine ssh %s" % node_name
    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']
    cmd.append(remote_cmd)
    #print(cmd)
    
    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()
    
    err = err.decode('utf8')
    #print(err)

    if err != '':
        return -1

    return 0

def _ssh_copy_id(addr, port, user, passwd):
    cmd = 'ssh-copy-id -p %s %s@%s -o StrictHostKeyChecking=no' % (port, user, addr)
    print(cmd)

    child = pexpect.spawn(cmd)

    try:
        index = child.expect(['\'s password: ', 'WARNING', pexpect.EOF], timeout=10)

        if index == 0:
            child.sendline(passwd)
            child.expect(pexpect.EOF)
            child.sendline(passwd)
            #print(child.after,child.before)
            child.close()
            return 0

        if index == 1:
            child.close()
            return 1

        if index == 2:
            child.close()
            return -1

    except pexpect.TIMEOUT:
        print(child.after,child.before)
        child.close()
        return -1


def register_machine(remote_node):
    name = remote_node['name']
    addr = remote_node['addr']
    user = remote_node['user']
    passwd = remote_node['password']
    engine = remote_node['engine']
    engine_port = remote_node['engine_port']
    ssh_port = remote_node['ssh_port']

    if is_machine_registered(name):
        return 2

    ssh_copy_result = _ssh_copy_id(addr, ssh_port, user, passwd)
    print(ssh_copy_result)
    
    # cmd = 'docker-machine ssh sudo usermod -aG docker dockeradmin'

    cmd = 'docker-machine create --engine-storage-driver %s \
            --driver generic \
            --generic-ip-address=%s \
            --generic-engine-port=%s \
            --generic-ssh-user=%s \
            --generic-ssh-port=%s \
            %s' % (engine, addr, engine_port, user, ssh_port, name)

    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']
    print(cmd)

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()

    out = out.decode('utf-8')
    err = err.decode('utf-8')

    #pprint(out.decode('utf-8').split('\n'))
    #pprint(err.decode('utf-8').split('\n'))
 
    if err == '':
        if install_docker_machine_remote(node_name) == -1:
            return -1
        return 0

    return -1


def install_docker_machine_remote(remote_machine_name):
    if is_docker_machine_installed(remote_machine_name) != -1:
        print('Already installed on %s' % remote_machine_name)
        return 1

    remote_cmd = 'base=https://github.com/docker/machine/releases/download/v0.16.0 && \
            curl -L $base/docker-machine-$(uname -s)-$(uname -m) >/tmp/docker-machine && \
            sudo mv /tmp/docker-machine /usr/local/bin/docker-machine && \
            chmod 764 /usr/local/bin/docker-machine'

    cmd = 'docker-machine ssh %s' % remote_machine_name
    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']
    cmd.append(remote_cmd)
    # print(cmd)

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()

    out = out.decode('utf8')
    err = err.decode('utf8')

    print(out, err)

    if err == '':
        return 0

    else:
        return -1
    

def run_service_module(remote_machine_name):
    # 웹 서버에서만 호출 되는 것으로 가정
    cmd = 'docker-machine ssh remote mkdir Openwincon'
    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()

    cmd = 'docker-machine scp -r %s %s:Openwincon/SwarmManager' % (op.join(HOME_DIR, 'Openwincon/SwarmManager'), remote_machine_name)
    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()

    out = out.decode('utf8')
    err = err.decode('utf8')
    
    print(out, err)

    remote_cmd = 'cd Openwincon/SwarmManager; python3 service.py'

    cmd = 'docker-machine ssh %s' % remote_machine_name
    cmd = cmd.split(' ')
    cmd = [x for x in cmd if x != '']
    cmd.append(remote_cmd)

    proc = sp.Popen(cmd, stdin=sp.PIPE, stdout=sp.PIPE, stderr=sp.PIPE)
    out, err = proc.communicate()

    out = out.decode('utf8')
    err = err.decode('utf8')

    return


if __name__ == '__main__':
    install_docker_machine_remote('node1')
