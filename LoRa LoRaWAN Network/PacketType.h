/*
 * PacketType
 *
 * The Type of message or value is matched with specific number
 *
 * Each packet consist of following sequence.
 * MsgType(int)|ValueType(int)|Value(string)|ValueType(int)|Value(string)|...|END_OF_MESSAGE(int)
 */ 

#ifndef PACKETTYPE_H
#define PACKETTYPE_H
    
#include <stdio.h>
#include <string.h>
#include <time.h>

/********Message Type***********/
#define eNB_REGISTRATION_REQUEST 0
#define eNB_REGISTRATION_RESPONSE 1
#define eNB_STATE_UPDATE_REQUEST 2
#define eNB_STATE_UPDATE_RESPONSE 3
#define eNB_STATE_CONFIGURE_REQUEST 4
#define eNB_STATE_CONFIGURE_RESPONSE 5


/********Vaule Type***********/
#define eNB_ID 0
#define eNB_IP 1
#define eNB_NAME 2
#define eNB_MCC 3
#define eNB_MNC 4
#define eNB_DESCRIPTION 5

#define END_OF_MESSAGE 99 // Flag for end of message

/********Configure Vaule Type***********/
#define mNULL 0
#define START_eNB 1
#define STOP_eNB 2
#define GET_STATUS 3
#define CHANGE_NAME 4
#define CHANGE_MCC 5
#define CHANGE_MNC 6
#define ACK 7

#define END_OF_MESSAGE 99 // Flag for end of message

static time_t m_time;
static struct tm *m_ptm;     

static std::string ValueTypeToString(int type){
	switch(type){
	case 0:
		return "ID";
	case 1:
		return "IP";
	case 2:
		return "Name";
        case 3:
            return "MCC";
        case 4:
            return "MNC";  
        case 5:
            return "Description";    
	}
}

static std::string PrintTime() {
    
    time(&m_time);
    m_ptm = localtime(&m_time);
    std::string strTime = asctime(m_ptm);
    strTime.erase(strTime.length()-5);

    return strTime;
}

static bool isOnlyNumber(char * temp)
{
	int length = strlen(temp);
	for(int i = 0; i < length; i++)
	{
		if(!isdigit(temp[i])) return false;
	}

	return true;
}

static bool is_ip(std::string cand)
{
	int candIP[4];
	int cand_cnt = 0;

	char temp[1024];
	char * result;

	strcpy(temp, cand.c_str());

	result = strtok(temp, ".");

	while(result != NULL)
	{
		if(isOnlyNumber(result))
		{
			if(cand_cnt == 4) return false;
			candIP[cand_cnt] = atoi(result);
			cand_cnt++;
		}
		result = strtok(NULL, ".");
	}
	if(cand_cnt != 4)       //underflow
		return false;
	for(int i = 0; i < 4; i++)      //range check
		if( candIP[i] < 0 || 255 < candIP[i] )
			return false;

	return true;

}
        
#endif /* PACKETTYPE_H */

