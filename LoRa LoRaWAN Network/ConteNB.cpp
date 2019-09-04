/*
 * ConteNB.cpp
 */

#include "ConteNB.h"

using namespace std;

ConteNB::ConteNB() {
}

ConteNB::ConteNB(const ConteNB& orig) {
}

ConteNB::~ConteNB() {
}

void ConteNB::Connect(std::string ip) {
    
    if(!m_sock.create())
        cout << "[error] Socket Create Failed!!" << endl;

    if(m_sock.connect(ip, 12032))
        cout << "[Controller] Update Socket Connect Successful" << endl;
    else
        cout << "[error] Socket Connect Failed!!" << endl;
    
}

void ConteNB::RegistereNB(int type, std::string value) {
    
    string msg;
    Packet sendpkt, recvpkt;
    
    sendpkt.DecideMessageType(eNB_STATE_CONFIGURE_REQUEST);
    sendpkt.AddValue(type, value);
    msg = sendpkt.CreateMessage();
    
    m_sock.send(msg);
    cout << PrintTime() << "[Controller] eNB Configure Request" << endl;
    msg.clear();
    
    m_sock.recv(msg);
    
    if(msg != "") {
        recvpkt.Parse(msg);
        if(recvpkt.m_MsgType == eNB_STATE_CONFIGURE_RESPONSE) {
            cout << PrintTime() << "[Controller] eNB Configure Response" << endl;
            cout << recvpkt.m_Value[0] << endl;
        }
    }
}

