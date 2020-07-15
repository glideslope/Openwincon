# SwarmManager

#### Developer: Selin Chun (천세린)
#### E-mail: slchun at mmlab.snu.ac.kr

### Overview

This module is developed to provide functionality of distributing services into Access Points (AP) controlled by _Openwincon controller_.
The module is based on Docker and Docker Swarm which allows deploying service containers.
Our module utilizes this functionality thus, deploy containers on Access Points, but also provides other functionalities which will be described below.

![Docker](https://www.docker.com/sites/default/files/d8/2019-07/Moby-logo.png)
![Docker Swarm](https://raw.githubusercontent.com/docker-library/docs/471fa6e4cb58062ccbf91afc111980f9c7004981/swarm/logo.png)

### Operation flow
Below description is based on the GUI-version of the module. 

As for the start, the web server needs to be run (based on Flask framework) in order to handle swarm operations. After running the web server, user can access to the management screen then start the swarm. Note that, currently, initial nodes for the swarm is provided as json file to provide bootstrapping function.
When the swarm is started, the nodes will join to the current manager server. Then, the user can monitor the status of each node in the swarm (performance is shown as real-time graph, each server repeatedly its last 1-minute performance for everay 1 second). Also, the user can add, remove the node or create, delete or update regarding to the service. 

Further, our module provides automated service distribution among nodes. Using the reported performance metrics from each server and the service's performance requirements (given at the service creation time), our module automatically calculates the avialability score of the service for each given node, then deploys the service to the node with the lowest score. If the service should be deployed on to multiple nodes, then the each instance is regarded as different service, then score is calculated, thus can distribute service automatically among nodes. 
(For further information, please refer to perf/static_allocator.py)

To provide aformentioned function (automatic service deployment considering the node's performance), we have to periodically monitor the node's performance. In this module, we have built the docker container for performance monitoring (refer to containers/perf/*), and when node is added to swarm, we create the service to run this monitoring container. 
Also, to receive and display this monitoring result, we run the server at the background, thus, receives the result and saves it in the manager server. After that, the web server can display the monitoring result (server's resource and service's resource usage) in the page. 



### Files
- Directories
    1. Containers/perf: Used to create docker container that measures given APs performance in terms of CPU, Memory and Disk usage.
    2. perf: Contains files which are used to monitor the APs and aggregate their performance reports.
    3. web: Contains files for Web-GUI.

- cli.py: Supports user interface for CLI.
- client.py: Functions for 'Docker Client'
- common.py: Collection of util functions used in the module.
- image.py: Functions for controlling the service image.
- machine_wrapper.py: Collection of wrapper functions for CLI-based 'Docker-machine' commands.
- node.py: Functions used to control connected Docker nodes. 
- service.py: Functions to control services which are deployed to Docker nodes.
- swarm.py: Function to control the overall swarm


### How to use
Initial version of SwarmManager was based on CLI, and later, developed modules for GUI support.
But, still both versions can be used, thus, we describe both types of interfaces.
In the below, we describe the preliminary requirements to use the module and then describe both types of interfaces.

- Base
We require both docker and docker swarm to be installed.
Also, python3 is require to run this module. (Note that virtual environment can be useful.)


- CLI based
CLI can be run by 'python3 cli.py'. CLI is built based on python's cmd module.
CLI provides following menus.
1. Swarm: Creating swarm or destroying it.
2. Node: List the node in the swarm. (Swarm needs to be created first) Also, it can node's label, role (manager, worker) or availability.
3. Service: List, create, remove or update the service in the swarm. 
4. Image: Push image into the remote image registry.

- GUI based:

