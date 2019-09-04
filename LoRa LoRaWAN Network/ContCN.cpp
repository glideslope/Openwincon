/*
 * ContCN.cpp
 */

#include "ContCN.h"

using namespace std;

ContCN::ContCN() {
}

ContCN::ContCN(const ContCN& orig) {
}

ContCN::~ContCN() {
}

std::string ContCN::RegisterUE(int type, std::string imsi) {
    
    bool issuccess = true;
    string msg = "";
    string query = "";
    stringstream ss;
    
    switch(type) {
        case 0:
            if(imsi.length() == 15) {
                query = "INSERT INTO users (`imsi`, `msisdn`, `imei`, `imei_sv`, `ms_ps_status`, `rau_tau_timer`, `ue_ambr_ul`, `ue_ambr_dl`, `access_restriction`, `mme_cap`, `mmeidentity_idmmeidentity`, `key`, `RFSP-Index`, `urrp_mme`, `sqn`, `rand`, `OPc`) VALUES ('" + imsi + "',  '33638060010', NULL, NULL, 'PURGED', '120', '50000000', '100000000', '47', '0000000000', '7', 0x8BAF473F2F8FD09487CCCBD7097C6862, '1', '0', '', 0x00000000000000000000000000000000, '');";
                m_db.SendQuery(query);
                query.clear();
                int id = m_db.FindLargestKeyValue("pdn","id") + 1;
                ss << id;
                query = "INSERT INTO pdn (`id`, `apn`, `pdn_type`, `pdn_ipv4`, `pdn_ipv6`, `aggregate_ambr_ul`, `aggregate_ambr_dl`, `pgw_id`, `users_imsi`, `qci`, `priority_level`,`pre_emp_cap`,`pre_emp_vul`, `LIPA-Permissions`) VALUES ('" + ss.str() + "',  'oai.ipv4','IPV4', '0.0.0.0', '0:0:0:0:0:0:0:0', '50000000', '100000000', '3',  '" + imsi + "', '9', '15', 'DISABLED', 'ENABLED', 'LIPA-ONLY');";
                m_db.SendQuery(query);
                
                cout << "[Controller] Add UE complete" << endl;
            }
            else
                issuccess = false;
            break;
        case 1:
            if(imsi.length() == 15) {
                query = "DELETE FROM users WHERE imsi = " + imsi;
                m_db.SendQuery(query); 
                query.clear();
                query = "DELETE FROM pdn WHERE users_imsi = " + imsi;
                m_db.SendQuery(query); 
                
                cout << "[Controller] Delete UE complete" << endl;
            }
            else
                issuccess = false;
            break;
        default:
            break;
    }
    
    if(!issuccess) {
        msg = "[error] UE Resgistration failed!!";
        return msg;
    }
}

void ContCN::PrintList() {
    m_db.GeteNBList();
    m_db.GetUEList();
}
