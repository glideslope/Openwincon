/*
 * ContCommand.h
 * 
 * Processing command 
 */

#ifndef CONTCOMMAND_H
#define CONTCOMMAND_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <iostream>
#include <iomanip>
#include <netdb.h>

#include "ContCN.h"
#include "ConteNB.h"

class ContCommand {
public:
    ContCommand();
    ContCommand(const ContCommand& orig);
    virtual ~ContCommand();
    
    void CommandProcess(std::string cmd);
    void PrintHelp();
    
private:
    ContCN m_cn;
    ConteNB m_eNB;
};

#endif /* CONTCOMMAND_H */

