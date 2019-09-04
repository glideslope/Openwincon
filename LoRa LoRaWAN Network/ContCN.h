/*
 * ContCN.h
 * 
 * Processing command for CN
 */

#ifndef CONTCN_H
#define CONTCN_H

#include "Socket.h"
#include "Packet.h"
#include "DB.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <iostream>
#include <iomanip>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

class ContCN {
public:
    ContCN();
    ContCN(const ContCN& orig);
    virtual ~ContCN();
    
    void Connect(std::string ip);
    std::string RegisterUE(int type, std::string imsi);
    void PrintList();
    
private:
    Socket m_sock;
    DB m_db;
};

#endif /* CONTCN_H */

