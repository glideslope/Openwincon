/*
 * DB.h
 * 
 * database
 */

#ifndef DB_H
#define DB_H

#include <mysql/mysql.h>
#include <iostream>
#include <string.h>
#include <iomanip>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>

#define HOST     "localhost"
#define USER     "root"
#define PASSWD   "linux"
#define Database "oai_db"

class DB {
public:
    DB();
    DB(const DB& orig);
    virtual ~DB();
    
    void SendQuery(std::string query);
    int FindLargestKeyValue(std::string table, std::string type);
    int FindNumberOfTuples(std::string table);
    
    void GeteNBList();
    void GetUEList();
    
private:
    MYSQL m_mysql;
};

#endif /* DB_H */

