/*
 * ContCommand.cpp
 */

#include "ContCommand.h"

using namespace std;

ContCommand::ContCommand() {
}

ContCommand::ContCommand(const ContCommand& orig) {
}

ContCommand::~ContCommand() {
}

void ContCommand::CommandProcess(std::string cmd) {
    
    string* temp;
    int cnt = 0;
    bool issuccess = false;
    
    for(int i = 0; i < cmd.length(); i++) {
        if(cmd.substr(i, 1) == "|")
            cnt++;
    }
    
    temp = new string[cnt + 1];
    
    for(int i = 0; i < cnt + 1; i++) {
        temp[i] = cmd.substr(0,cmd.find("|"));
        cmd.erase(0, cmd.find("|") + 1);
    }
    
    if(temp[0] == "help" || temp[0] == "h") {
        PrintHelp();
        issuccess = true;
    }
    else if(temp[0] == "show") {
        m_cn.PrintList();
        issuccess = true;
    }
    else { 
        if(is_ip(temp[0])) {
            if(temp[1] == "UE") {
                if(temp[2] == "add") {
                    cout << m_cn.RegisterUE(0, temp[3]) << endl;
                    issuccess = true;
                }
                else if (temp[2] == "del") {
                    cout << m_cn.RegisterUE(1, temp[3]) << endl;
                    issuccess = true;
                }
            }
            else if(temp[1] == "eNB") {
                if(temp[2] == "start") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(START_eNB, "");
                    issuccess = true;
                }
                else if(temp[2] == "stop") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(STOP_eNB, "");
                    issuccess = true;
                }
                else if(temp[2] == "status") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(GET_STATUS, "");
                    issuccess = true;
                }
                else if(temp[2] == "name") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(CHANGE_NAME, temp[3]);
                    issuccess = true;
                }
                else if(temp[2] == "MCC") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(CHANGE_MCC, temp[3]);
                    issuccess = true;
                }
                else if(temp[2] == "MNC") {
                    m_eNB.Connect(temp[0]);
                    m_eNB.RegistereNB(CHANGE_MNC, temp[3]);
                    issuccess = true;
                }
            }
        }  
    }
    
    if(!issuccess) {
        cout << "[error] Invalid Command!!" << endl;
        PrintHelp();
    }
    
}

void ContCommand::PrintHelp() {
    
    cout << "Commands : <IP> <Command> [target] [value]"<< endl;
    
    cout << setw(25) << left << "help or h" << setw(30) << setfill(' ') << "print help message" << endl;
    
    cout << setw(25) << left << "show" << setw(30) << setfill(' ') << "show list of eNB and UE" << endl;
    
    cout << "::UE::" << endl;
    
    cout << setw(25) << left << "IP UE add <value>" << setw(30) << setfill(' ') << "add UE (value : IMSI, 15 digits)" << endl;
    
    cout << setw(25) << left << "IP UE del <value>" << setw(30) << setfill(' ') << "delete UE (value : IMSI, 15 digits)" << endl;
    
    cout << "::eNB::" << endl;
    
    cout << setw(25) << left << "IP eNB start" << setw(30) << setfill(' ') << "start eNB" << endl;
    
    cout << setw(25) << left << "IP eNB stop" << setw(30) << setfill(' ') << "stop eNB" << endl;
    
    cout << setw(25) << left << "IP eNB status" << setw(30) << setfill(' ') << "print eNB's status" << endl;
    
    cout << setw(25) << left << "IP eNB name <value>" << setw(30) << setfill(' ') << "change eNB's name" << endl;
    
    cout << setw(25) << left << "IP eNB MCC <value>" << setw(30) << setfill(' ') << "change eNB's MCC (value : 0~999)" << endl;
    
    cout << setw(25) << left << "IP eNB MNC <value>" << setw(30) << setfill(' ') << "change eNB's MNC (value : 0~99)" << endl;
}