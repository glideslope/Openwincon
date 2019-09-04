/*
 * DB.cpp
 */

#include "DB.h"
using namespace std;

DB::DB() {
    mysql_init(&m_mysql);

    if(!mysql_real_connect(&m_mysql, HOST, USER, PASSWD, Database, 0, (char *)NULL, 0)) {
        cout << "[CN DB] " << mysql_error(&m_mysql)<< endl;
        exit(1);
    }
    else
        cout << "[CN DB] Database is connected" << endl;
}

DB::DB(const DB& orig) {
}

DB::~DB() {
}

void DB::SendQuery(std::string query) {
    mysql_query(&m_mysql, query.c_str());
}

int DB::FindLargestKeyValue(std::string table, std::string type) {
    
    string query;
    MYSQL_RES* result;
    MYSQL_ROW row;
    
    query = "SELECT MAX(" + type + ") FROM " + table;
    
    mysql_query(&m_mysql, query.c_str());

    result = mysql_store_result(&m_mysql);
    
    if((row = mysql_fetch_row(result)) != NULL) {
        return atoi(row[0]);
    }
}

int DB::FindNumberOfTuples(std::string table) {
    
    MYSQL_RES* res;
    MYSQL_ROW row;
    int num = 0;
    std::string query = "select COUNT(*) from " + table + ";";
    
    
    mysql_query(&m_mysql, query.c_str());
    
    res = mysql_store_result(&m_mysql);
    
    if((row = mysql_fetch_row(res)) != NULL) {
        num = atoi(row[0]);    
    }
}

void DB::GeteNBList() {
    
    MYSQL_RES* res;
    MYSQL_ROW row;
    
    mysql_query(&m_mysql, "select ID, IP, Name, MCC, MNC from eNBs;");
    
    res = mysql_store_result(&m_mysql);
    
    cout << endl << "::eNB List::" << endl;
    cout << setfill(' ') << left << setw(20) << "ID" << setw(20) << "IP" << setw(15) << "Name" << setw(10) <<"MCC" << setw(10) << "MNC" << endl;
    
    while((row = mysql_fetch_row(res)) != NULL) {        
        cout << setfill(' ') << left << setw(20) << row[0] << setw(20) << row[1] << setw(15) << row[2] << setw(10) << row[3] << setw(10) << row[4] << endl;    
    }
}

void DB::GetUEList() {
    
    MYSQL_RES* res;
    MYSQL_ROW row;
    
    mysql_query(&m_mysql, "select imsi, imei, mmeidentity_idmmeidentity from users;");
    
    res = mysql_store_result(&m_mysql);
    
    cout << endl << "::UE List::" << endl;
    cout << setfill(' ') << left << setw(25) << "IMSI" << setw(20) << "IMEI" << setw(5) << "MME_ID" << endl;
    
    while((row = mysql_fetch_row(res)) != NULL) {        
        cout << setfill(' ') << left << setw(25) << row[0] << setw(20) << row[1] << setw(5) << row[2] << endl;    
    }
}