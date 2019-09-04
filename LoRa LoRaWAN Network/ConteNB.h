/*
 * ConteNB.h
 * 
 * Processing command for eNB
 */

#ifndef CONTENB_H
#define CONTENB_H

#include "Socket.h"
#include "Packet.h"

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

class ConteNB {
public:
    ConteNB();
    ConteNB(const ConteNB& orig);
    virtual ~ConteNB();
    
    void Connect(std::string ip);
    void RegistereNB(int type, std::string value);
    
private:
    Socket m_sock;
};

#endif /* CONTENB_H */

