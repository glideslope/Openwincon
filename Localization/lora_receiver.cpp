/* 
 *  LoRa low-level gateway to receive and send command
 *
 *  Copyright (C) 2015-2017 Congduc Pham
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***************************************************************************** 
 *  Version:                1.75
 *  Design:                 C. Pham
 *  Implementation:         C. Pham
 *
 *  waits for command or data on serial port or from the LoRa module
 *    - command starts with /@ and ends with #
 *  
 *  LoRa parameters
 *    - /@M1#: set LoRa mode 1
 *    - /@C12#: use channel 12 (868MHz)
 *    - /@SF8#: set SF to 8
 *    - /@PL/H/M/x/X#: set power to Low, High or Max; extreme (PA_BOOST at +14dBm), eXtreme (PA_BOOST at +20dBm)
 *    - /@W34#: set sync word to 0x34
 *    - /@ON# or /@OFF#: power on/off the LoRa module
 * 
 *  CAD, DIFS/SIFS mechanism, RSSI checking, extended IFS
 *    - /@CAD# performs an SIFS CAD, i.e. 3 or 6 CAD depending on the LoRa mode
 *    - /@CADON3# uses 3 CAD when sending data (normally SIFS is 3 or 6 CAD, DIFS=3SIFS)
 *    - /@CADOFF# disables CAD (IFS) when sending data
 *    - /@RSSI# toggles checking of RSSI before transmission and after CAD
 *    - /@EIFS# toggles for extended IFS wait
 *
 *    - just connect the Arduino board with LoRa module
 *    - use any serial tool to view data that is received
 *    - with a python script to read serial port, all received data could be forwarded to another application through
 *      standart output
 *    - remote configuration needs to be allowed by unlocking the gateway with command '/@U' with an unlock pin
 *      - '/@U1234#'
 *    - allowed commands for a gateway are
 *      - M, C, P, A, ON, OFF
 *      - ACKON, ACKOFF (if using unmodified SX1272 lib)
 *
 *  if compiled with LORA_LAS
 *    - add LAS support
 *      - sending message will use LAS service
 *      - /@LASS# prints LAS statistics
 *      - /@LASR# resets LAS service
 *      - /@LASON# enables LAS service
 *      - /@LASOFF# disables LAS service
 *      - /@LASI# initiate the INITrestart/INIT procedure that ask end-devices to send the REG msg
 *      
 *   IMPORTANT NOTICE   
 *    - the gateway use data prefix to indicate data received from radio. The prefix is 2 bytes: 0xFF0xFE 
 *    - the post-processing stage looks for this sequence to dissect the data, according to the format adopted by the sender 
 *    - if you use our LoRa_Temp example, then the packet format as expected by the post-processing script is as follows:
 *      - without application key and without encryption, payload starts immediately: [payload]
 *      - if application key is used, without encryption: [AppKey(4B)][payload]
 *      - with encryption: the payload is encrypted using LoRaWAN algorithm
 *    - for more details on the underlying packet format used by our modified SX1272 library
 *      - refer to the SX1272.h
 *      - see http://cpham.perso.univ-pau.fr/LORA/RPIgateway.html
 *      
*/

/*  Change logs
 *  July, 4th, 2017. v1.75 
 *        receive window set to MAX_TIMEOUT (10000ms defined in SX1272.h)
 *  June, 19th, 2017. v1.74
 *        interDownlinkCheckTime is set to 5s
 *  May, 8th, 2017. v1.73
 *        Default behavior is to disable the remote configuration features
 *  Mar, 29th, 2017. v1.72
 *        Add periodic status (every 10 minutes) from the low-level gateway
 *        Add reset of radio module when receive error from radio is detected
 *        Change receive windows from 1000ms, or 2500ms in mode 1, to 10000ms for all modes, reducing overheads of receive loop 
 *  Mar, 2nd, 2017. v1.71
 *        Add preamble length verification and correction to the default value of 8 if it is not the case    
 *  Dec, 14st, 2016. v1.7   
 *        Reduce dynamic memory usage for having a simple relay gateway running on an Arduino Pro Mini
 *          - about 600B of free memory should be available
 *        Add support for a simple relay gateway with an Arduino node, uncomment GW_RELAY
 *          - the simple gateway has minimum output as it is not intended to be connected to a computer
 *          - continously waits for packets then will re-sent the packet (destination is 1) by keeping the original packet header
 *  Oct, 21st, 2016. v1.6S
 *        Split the lora_gateway sketch into 2 parts:   
 *          - lora_gateway: for gateway, similar to previous IS_RCV_GATEWAY
 *          - lora_interactivedevice, similar to previous IS_SEND_GATEWAY
 *        Add support for 4 channels in the 433MHz band. Default is CH_00_433 = 0x6C5333 for 433.3MHz. See SX1272.h.         
 *  Oct, 9th, 2016. v1.6
 *        Change the downlink strategy
 *             - the lora_gateway.cpp program will check for a downlink.txt file after each LoRa packet reception
 *             - this behavior can be disable with --ndl option
 *             - after a LoRa packet reception, lora_gateway.cpp will wait for interDownlinkCheckTime before checking for downlink.txt file
 *             - downlink.txt will be normally generated by post_processing_gw.py
 *             - post_processing_gw.py periodically check for downlink-post.txt and will build a queue of downlink requests
 *             - when a lora packet from device i is processed by post_processing_gw.py, it will check if there is a pending request for device i
 *             - if it is the case, then post_processing_gw.py generates the corresponding downlink.txt which will contain in most cases only 1 entry
 *  August, 7th, 2016. v1.5
 *        Add preliminary and simple support for downlink request (only for Linux-based gateway)
 *          - will periodically check for downlink.txt file
 *          - the file contains a series of line in JSON format: { "status" : "send_request", "dst" : 3, "data" : "/@Px#" }
 *          - mandatory key are "status", "dst" and "data". "status" must be "send_request"
 *          - each line must be terminated by \n. Do not leave an empty line at the end, just \n
 *          - you can add other fields for logging/information purposes
 *          - every interDownlinkCheckTime the existence of downlink.txt will be checked
 *          - all requests will be stored in memory and downlink.txt will be renamed, e.g. downlink-backup-2016-08-01T20:25:44.txt
 *          - downlink-queued.txt will be appended with new send_request, marked as "queued"
 *          - when there are pending send request, then every interDownlinkSendTime a transmission will occur
 *          - downlink-send.txt will be appended with new transmissions, marked as "sent" or "sent_fail"
 *          - there is no reliability implemented
 *          - it is expected that new sending request will be indicated in a new downlink.txt file
 *          - this file can be created in various ways: interactive mode, MQTT, ftp, http,...
 *        Change the CarrierSense behavior with an "only once" behavior that that a busy channel will not block the gateway
 *          - CarrierSense now accept an optional parameter that is false by default. true indicates "only once" behavior
 *          - CarrierSense now returns an integer. 1 means that CarrierSense found a busy channel under "only once" behavior      
 *  June, 14th, 2016. v1.4
 *        Fix bug on serial port for the RPI3 and for the Bluetooth interface on RPI3 which uses the serial port
 *  Mar, 25th, 2016. v1.3
 *        Add command to set the spreading factor between 6 and 12:
 *          - /@SF8#: set SF to 8
 *  Fev, 25th, 2016. v1.2
 *        Add 900MHz support when specifying a channel in command line
 *        Use by default channel 10 (865.2MHz) in 868MHz band and channel 5 (913.88) in 900MHz band
 *  Jan, 22th, 2016. v1.1
 *        Add advanced configuration options when running on Linux (Raspberry typically)
 *          - options are: --mode 4 --bw 500 --cr 5 --sf 12 --freq 868.1 --ch 10 --sw 34 --raw 
 *        Add raw output option in the Linux version. The gateway will forward all the payload without any interpretation  
 *          - this feature is implemented in the SX1272 library, see the corresponding CHANGES.log file
 *          - this is useful when the packet interpretation is left to the post-processing stage (e.g. for LoRaWAN)
 *  Dec, 30th, 2015. v1.0
 *        SX1272 library has been modified to allow for sync word setting, a new mode 11 is introduced to test with LoRaWAN
 *        BW=125kHz, CR=4/5 and SF=7. When using mode 11, sync word is set to 0x34. Normally, use the newly defined CH_18_868=868.1MHz
 *        Add possibility to set the sync word
 *          - /@W34# set the sync word to 0x34
 *  Nov, 13th, 2015. v0.9
 *        SX1272 library has been modified to support dynamic ACK request using the retry field of the packet header
 *        Gateway now always use receivePacketTimeout() and sender either use sendPacketTimeout() or sendPacketTimeoutACK()
 *  Nov, 10th, 2015. v0.8a
 *        Add an unlock pin to allow the gateway to accept remote commands
 *        A limited number of attempts is allowed
 *          - /@U1234#: try to unlock with pin 1234. To lock, issue the same command again.
 *  Oct, 8th, 2015. v0.8
 *        Can change packet size for periodic packet transmission
 *          - /@Z200# sets the packet payload size to 200. The real size is 205B with the Libelium header.
 *            Maximum size that can be indicated is then 250.
 *        Add possibility to send periodically at random time interval
 *          - /@TR5000#: send a message at random time interval between [2000, 5000]ms.
 *        Check RSSI value before transmitting a packet. This is done after successful CAD
 *          - CAD must be ON
 *          - /@RSSI# toggles checking of RSSI, must be above -90dBm to transmit, otherwise, repeat 10 times
 *  Sep, 22nd, 2015. v0.7
 *        Add ACK support when sending packets
 *          - /@ACKON# enables ACK
 *          - /@ACKOFF# disables ACK
 *        Can use extended IFS wait to: 
 *          - CAD must be ON
 *          - wait a random number of CAD after a successful IFS
 *          - perform an IFS one more time before packet tranmission 
 *          - /@EIFS# toggles for extended IFS wait
 *  Jul, 1st, 2015. v0.6
 *        Add support of the LoRa Activity Sharing (LAS) mechanism (device side), uncomment #define LORA_LAS
 *          - sending message will use LAS service
 *          - /@LASS# prints LAS statistics
 *          - /@LASR# resets LAS service
 *          - /@LASON# enables LAS service
 *          - /@LASOFF# disables LAS service
 *          - /@REG# sends a REG message if IS_SEND_GATEWAY
 *          - /@INIT# sends an INIT(0,delay) message for restarting if IS_SEND_GATEWAY
 *  June, 29th, 2015. v0.5
 *        Add a CAD_TEST behavior to see continuously channel activity, uncomment #define CAD_TEST
 *        Add LoRa ToA computation when sending data
 *        Add CAD test when sending data
 *          - /@CADON3# uses 3 CAD when sending data (normally SIFS is 3 or 6 CAD, DIFS=3SIFS)
 *          - /@CADOFF# disables CAD when sending data
 *        Add CAD feature for testing
 *          - /@CAD# performs an SIFS CAD, i.e. 3 or 6 CAD depending on the LoRa mode
 *        Add ON and OFF command to power on/off the LoRa module
 *          - /@ON# or /@OFF#
 *        Add the S command to send a string of arbitrary size
 *          - /@S50# sends a 50B user payload packet filled with '#'. The real size is 55B with the Libelium header 
 *        The gateway can accept command from serial or from the LoRa module
 *  May, 11th, 2015. v0.4
 *        Add periodic sending of packet for range test
 *          - /@T5000#: send a message every 5s. Use /@T0# to disable periodic sending
 *  Apr, 17th, 2015. v0.3
 *        Add possibility to configure the LoRa operation mode
 *          - /@M1#: set LoRa mode 1
 *          - /@C12#: use channel 12 (868MHz)
 *          - /@PL/H/M#: set power to Low, High or Max
 *          - /@A9#: set node addr to 9    
 *  Apr, 16th, 2015. v0.2
 *        Integration of receive gateway and send gateway: 
 *          - #define IS_SEND_GATEWAY will produce a sending gateway to send remote commands
 *  Apr, 14th, 2015. v0.1
 *        First version of receive gateway
 */

// MS add
#include <pthread.h>

// Include the SX1272 
#include "SX1272.h"

#ifdef ARDUINO
// IMPORTANT when using an Arduino only. For a Raspberry-based gateway the distribution uses a radio.makefile file
///////////////////////////////////////////////////////////////////////////////////////////////////////////
// please uncomment only 1 choice
//
// uncomment if your radio is an HopeRF RFM92W, HopeRF RFM95W, Modtronix inAir9B, NiceRF1276
// or you known from the circuit diagram that output use the PABOOST line instead of the RFO line
//#define PABOOST
/////////////////////////////////////////////////////////////////////////////////////////////////////////// 
#endif

// IMPORTANT
///////////////////////////////////////////////////////////////////////////////////////////////////////////
// please uncomment only 1 choice
#define BAND868
//#define BAND900
//#define BAND433
///////////////////////////////////////////////////////////////////////////////////////////////////////////

// For a Raspberry-based gateway the distribution uses a radio.makefile file that can define MAX_DBM
//
#ifndef MAX_DBM
#define MAX_DBM 14
#endif

#ifndef ARDUINO
#include <stdio.h>
#include <getopt.h>
#include <stdlib.h>
#include <unistd.h>
#include <termios.h> 
#include  <signal.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#endif

#ifdef ARDUINO
// and SPI library on Arduino platforms
#include <SPI.h>
  
  #define PRINTLN                   Serial.println("")              
  #define PRINT_CSTSTR(fmt,param)   Serial.print(F(param))
  #define PRINT_STR(fmt,param)      Serial.print(param)
  #define PRINT_VALUE(fmt,param)    Serial.print(param)
  #define PRINT_HEX(fmt,param)      Serial.print(param,HEX)
  #define FLUSHOUTPUT               Serial.flush();
#else
  #define PRINTLN                   printf("\n")
  #define PRINT_CSTSTR(fmt,param)   printf(fmt,param)
  #define PRINT_STR(fmt,param)      PRINT_CSTSTR(fmt,param)
  #define PRINT_VALUE(fmt,param)    PRINT_CSTSTR(fmt,param)
  #define PRINT_HEX(fmt,param)      PRINT_VALUE(fmt,param)
  #define FLUSHOUTPUT               fflush(stdout);
#endif

#ifdef DEBUG
  #define DEBUGLN                 PRINTLN
  #define DEBUG_CSTSTR(fmt,param) PRINT_CSTSTR(fmt,param)  
  #define DEBUG_STR(fmt,param)    PRINT_CSTSTR(fmt,param)  
  #define DEBUG_VALUE(fmt,param)  PRINT_VALUE(fmt,param)  
#else
  #define DEBUGLN
  #define DEBUG_CSTSTR(fmt,param)
  #define DEBUG_STR(fmt,param)    
  #define DEBUG_VALUE(fmt,param)  
#endif

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// FOR DOWNLINK FEATURES
//
#if not defined ARDUINO && defined DOWNLINK
#define MAX_DOWNLINK_ENTRY 100

#include "rapidjson/reader.h"
#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"
#include <iostream>
#include <cmath>

using namespace rapidjson;
using namespace std;

char* json_entry[MAX_DOWNLINK_ENTRY];

size_t json_entry_size=100;
FILE *fp;
int dl_line_index;
ssize_t dl_line_size;
int dl_total_line;
bool hasDownlinkEntry=false;
bool enableDownlinkCheck=false;
bool optNDL=false;

unsigned long lastDownlinkCheckTime=0;
// we set to 5s after the gw receives a lora packet
// to give some time for the post-processing stage to generate a downlink.txt file if any
unsigned long interDownlinkCheckTime=5000L;
unsigned long lastDownlinkSendTime=0;
// 20s between 2 downlink transmissions when there are queued requests
unsigned long interDownlinkSendTime=20000L;
#endif
///////////////////////////////////////////////////////////////////////////////////////////////////////////

//#define SHOW_FREEMEMORY
//#define GW_RELAY
//#define RECEIVE_ALL 
//#define CAD_TEST
//#define LORA_LAS
//#define WINPUT
//#define ENABLE_REMOTE

#ifdef BAND868
#define MAX_NB_CHANNEL 15
#define STARTING_CHANNEL 4
#define ENDING_CHANNEL 18
#ifdef SENEGAL_REGULATION
uint8_t loraChannelIndex=0;
#else
uint8_t loraChannelIndex=6;
#endif
uint32_t loraChannelArray[MAX_NB_CHANNEL]={CH_04_868,CH_05_868,CH_06_868,CH_07_868,CH_08_868,CH_09_868,
                                            CH_10_868,CH_11_868,CH_12_868,CH_13_868,CH_14_868,CH_15_868,CH_16_868,CH_17_868,CH_18_868};

#elif defined BAND900 
#define MAX_NB_CHANNEL 13
#define STARTING_CHANNEL 0
#define ENDING_CHANNEL 12
uint8_t loraChannelIndex=5;
uint32_t loraChannelArray[MAX_NB_CHANNEL]={CH_00_900,CH_01_900,CH_02_900,CH_03_900,CH_04_900,CH_05_900,CH_06_900,CH_07_900,CH_08_900,
                                            CH_09_900,CH_10_900,CH_11_900,CH_12_900};
#elif defined BAND433
#define MAX_NB_CHANNEL 4
#define STARTING_CHANNEL 0
#define ENDING_CHANNEL 3
uint8_t loraChannelIndex=0;
uint32_t loraChannelArray[MAX_NB_CHANNEL]={CH_00_433,CH_01_433,CH_02_433,CH_03_433};                                              
#endif

// use the dynamic ACK feature of our modified SX1272 lib
#define GW_AUTO_ACK

///////////////////////////////////////////////////////////////////
// DEFAULT LORA MODE
//#define LORAMODE 1
// the special mode to test BW=125MHz, CR=4/5, SF=12
// on the 868.1MHz channel
#define LORAMODE 11
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
// GATEWAY HAS ADDRESS 1
#define LORA_ADDR 1
///////////////////////////////////////////////////////////////////

// to unlock remote configuration feature
#define UNLOCK_PIN 1234
// will use 0xFF0xFE to prefix data received from LoRa, so that post-processing stage can differenciate
// data received from radio
#define WITH_DATA_PREFIX

#ifdef WITH_DATA_PREFIX
#define DATA_PREFIX_0 0xFF
#define DATA_PREFIX_1 0xFE
#endif

#ifdef LORA_LAS
#include "LoRaActivitySharing.h"
// acting as the LR-BS
LASBase loraLAS = LASBase();
#endif

///////////////////////////////////////////////////////////////////
// CONFIGURATION VARIABLES
//
#ifndef ARDUINO
char keyPressBuff[30];
uint8_t keyIndex=0;
int ch;
#endif"

// be careful, max command length is 60 characters
#define MAX_CMD_LENGTH 60

char cmd[MAX_CMD_LENGTH]="****************";
int msg_sn=0;

// number of retries to unlock remote configuration feature
uint8_t unlocked_try=3;
boolean unlocked=false;
boolean receivedFromSerial=false;
boolean receivedFromLoRa=false;
boolean withAck=false;

bool radioON=false;
bool RSSIonSend=true;

uint8_t loraMode=LORAMODE;

uint32_t loraChannel=loraChannelArray[loraChannelIndex];
#if defined PABOOST || defined RADIO_RFM92_95 || defined RADIO_INAIR9B || defined RADIO_20DBM
// HopeRF 92W/95W and inAir9B need the PA_BOOST
// so 'x' set the PA_BOOST but then limit the power to +14dBm 
char loraPower='x';
#else
// other radio board such as Libelium LoRa or inAir9 do not need the PA_BOOST
// so 'M' set the output power to 15 to get +14dBm
char loraPower='M';
#endif

uint8_t loraAddr=LORA_ADDR;

int status_counter=0;
unsigned long startDoCad, endDoCad;
bool extendedIFS=true;
uint8_t SIFS_cad_number;
// set to 0 to disable carrier sense based on CAD
uint8_t send_cad_number=3;
uint8_t SIFS_value[11]={0, 183, 94, 44, 47, 23, 24, 12, 12, 7, 4};
uint8_t CAD_value[11]={0, 62, 31, 16, 16, 8, 9, 5, 3, 1, 1};

bool optAESgw=false;
uint16_t optBW=0; 
uint8_t optSF=0;
uint8_t optCR=0;
uint8_t optCH=0;
bool  optRAW=false;
double optFQ=-1.0;
uint8_t optSW=0x12;
///////////////////////////////////////////////////////////////////

#if defined ARDUINO && defined SHOW_FREEMEMORY && not defined __MK20DX256__ && not defined __MKL26Z64__ && not defined  __SAMD21G18A__ && not defined _VARIANT_ARDUINO_DUE_X_
int freeMemory () {
  extern int __heap_start, *__brkval; 
  int v; 
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval); 
}
#endif

long getCmdValue(int &i, char* strBuff=NULL) {
        
        char seqStr[7]="******";

        int j=0;
        // character '#' will indicate end of cmd value
        while ((char)cmd[i]!='#' && (i < strlen(cmd)) && j<strlen(seqStr)) {
                seqStr[j]=(char)cmd[i];
                i++;
                j++;
        }
        
        // put the null character at the end
        seqStr[j]='\0';
        
        if (strBuff) {
                strcpy(strBuff, seqStr);        
        }
        else
                return (atol(seqStr));
}   

void startConfig() {

  int e;

  // has customized LoRa settings    
  if (optBW!=0 || optCR!=0 || optSF!=0) {

    e = sx1272.setCR(optCR-4);
    PRINT_CSTSTR("%s","^$LoRa CR ");
    PRINT_VALUE("%d", optCR);    
    PRINT_CSTSTR("%s",": state ");
    PRINT_VALUE("%d", e);
    PRINTLN;

    e = sx1272.setSF(optSF);
    PRINT_CSTSTR("%s","^$LoRa SF ");
    PRINT_VALUE("%d", optSF);    
    PRINT_CSTSTR("%s",": state ");
    PRINT_VALUE("%d", e);
    PRINTLN;
    
    e = sx1272.setBW( (optBW==125)?BW_125:((optBW==250)?BW_250:BW_500) );
    PRINT_CSTSTR("%s","^$LoRa BW ");
    PRINT_VALUE("%d", optBW);    
    PRINT_CSTSTR("%s",": state ");
    PRINT_VALUE("%d", e);
    PRINTLN;

    // indicate that we have a custom setting
    loraMode=0;
  
    if (optSF<10)
      SIFS_cad_number=6;
    else 
      SIFS_cad_number=3;
      
  }
  else {
    
    // Set transmission mode and print the result
    PRINT_CSTSTR("%s","^$LoRa mode ");
    PRINT_VALUE("%d", loraMode);
    PRINTLN;
        
    e = sx1272.setMode(loraMode);
    PRINT_CSTSTR("%s","^$Setting mode: state ");
    PRINT_VALUE("%d", e);
    PRINTLN;
  
  #ifdef LORA_LAS
    loraLAS.setSIFS(loraMode);
  #endif
  
    if (loraMode>7)
      SIFS_cad_number=6;
    else 
      SIFS_cad_number=3;

  }
  
  // Select frequency channel
  if (loraMode==11) {
    // if we start with mode 11, then switch to 868.1MHz for LoRaWAN test
    // Note: if you change to mode 11 later using command /@M11# for instance, you have to use /@C18# to change to the correct channel
    e = sx1272.setChannel(CH_18_868);
    PRINT_CSTSTR("%s","^$Channel CH_18_868: state ");    
  }
  else {
    e = sx1272.setChannel(loraChannel);

    if (optFQ>0.0) {
      PRINT_CSTSTR("%s","^$Frequency ");
      PRINT_VALUE("%f", optFQ);
      PRINT_CSTSTR("%s",": state ");      
    }
    else {
#ifdef BAND868
    if (loraChannelIndex>5) {      
      PRINT_CSTSTR("%s","^$Channel CH_1");
      PRINT_VALUE("%d", loraChannelIndex-6);      
    }
    else {
      PRINT_CSTSTR("%s","^$Channel CH_0");
      PRINT_VALUE("%d", loraChannelIndex+STARTING_CHANNEL);        
    }
    PRINT_CSTSTR("%s","_868: state ");
#elif defined BAND900
    PRINT_CSTSTR("%s","^$Channel CH_");
    PRINT_VALUE("%d", loraChannelIndex);
    PRINT_CSTSTR("%s","_900: state ");
#elif defined BAND433
    //e = sx1272.setChannel(0x6C4000);
    PRINT_CSTSTR("%s","^$Channel CH_");
    PRINT_VALUE("%d", loraChannelIndex);  
    PRINT_CSTSTR("%s","_433: state ");  
#endif
    }
  }  
  PRINT_VALUE("%d", e);
  PRINTLN; 

  // Select amplifier line; PABOOST or RFO
#ifdef PABOOST
  sx1272._needPABOOST=true;
  // previous way for setting output power
  // loraPower='x';
  PRINT_CSTSTR("%s","^$Use PA_BOOST amplifier line");
  PRINTLN;   
#else
  // previous way for setting output power
  // loraPower='M';  
#endif
  
  // Select output power in dBm
  e = sx1272.setPowerDBM((uint8_t)MAX_DBM);
  
  PRINT_CSTSTR("%s","^$Set LoRa power dBm to ");
  PRINT_VALUE("%d",(uint8_t)MAX_DBM);  
  PRINTLN;
                
  PRINT_CSTSTR("%s","^$Power: state ");
  PRINT_VALUE("%d", e);
  PRINTLN;
 
  // get preamble length
  e = sx1272.getPreambleLength();
  PRINT_CSTSTR("%s","^$Get Preamble Length: state ");
  PRINT_VALUE("%d", e);
  PRINTLN; 
  PRINT_CSTSTR("%s","^$Preamble Length: ");
  PRINT_VALUE("%d", sx1272._preamblelength);
  PRINTLN;

  if (sx1272._preamblelength != 8) {
      PRINT_CSTSTR("%s","^$Bad Preamble Length: set back to 8");
      sx1272.setPreambleLength(8);
      e = sx1272.getPreambleLength();
      PRINT_CSTSTR("%s","^$Get Preamble Length: state ");
      PRINT_VALUE("%d", e);
      PRINTLN;
      PRINT_CSTSTR("%s","^$Preamble Length: ");
      PRINT_VALUE("%d", sx1272._preamblelength);
      PRINTLN;
  }  
  
  // Set the node address and print the result
  //e = sx1272.setNodeAddress(loraAddr);
  sx1272._nodeAddress=loraAddr;
  e=0;
  PRINT_CSTSTR("%s","^$LoRa addr ");
  PRINT_VALUE("%d", loraAddr);
  PRINT_CSTSTR("%s",": state ");
  PRINT_VALUE("%d", e);
  PRINTLN;

  if (optAESgw)
      PRINT_CSTSTR("%s","^$Handle AES encrypted data\n");

  if (optRAW) {
      PRINT_CSTSTR("%s","^$Raw format, not assuming any header in reception\n");  
      // when operating n raw format, the SX1272 library do not decode the packet header but will pass all the payload to stdout
      // note that in this case, the gateway may process packet that are not addressed explicitly to it as the dst field is not checked at all
      // this would be similar to a promiscuous sniffer, but most of real LoRa gateway works this way 
      sx1272._rawFormat=true;
  }
  
  // Print a success message
  PRINT_CSTSTR("%s","^$SX1272/76 configured ");
  PRINT_CSTSTR("%s","as LR-BS. Waiting RF input for transparent RF-serial bridge\n");
#if defined ARDUINO && defined GW_RELAY
  PRINT_CSTSTR("%s","^$Act as a simple relay gateway\n");
#endif
}

#define _PI 3.141592
#define Earth_R 6371000
#define DtoR (_PI / 180)

struct Address{
  double distance;
  double latitude;
  double longitude;
};

struct Point {
   double x;
   double y;
};

struct Circle {
   Point center;
   double radius;
};

Address addr[3];


void setup()
{
  int e;
// Power ON the module
  e = sx1272.ON();

  PRINT_CSTSTR("%s","^$**********Power ON: state ");
  PRINT_VALUE("%d", e);
  PRINTLN;

  e = sx1272.getSyncWord();

  if (!e) {
    PRINT_CSTSTR("%s","^$Default sync word: 0x");
    PRINT_HEX("%X", sx1272._syncWord);
    PRINTLN;
  }

  if (optSW!=0x12) {
    e = sx1272.setSyncWord(optSW);

    PRINT_CSTSTR("%s","^$Set sync word to 0x");
    PRINT_HEX("%X", optSW);
    PRINTLN;
    PRINT_CSTSTR("%s","^$LoRa sync word: state ");
    PRINT_VALUE("%d",e);
    PRINTLN;
  }

  if (!e) {
    radioON=true;
    startConfig();
  }


  // addr 초기화 (테스트용)
  addr[0].latitude = 0;
  addr[0].longitude = 0;
  addr[0].distance = 0;

  addr[1].latitude = 0; 
  addr[1].longitude = 0;
  addr[1].distance = 0;

  addr[2].latitude = 0;
  addr[2].longitude = 0;
  addr[2].distance = 0;

  FLUSHOUTPUT;
  delay(1000);

}

Point convert_GPS_to_XY(Circle standard, Circle gps) {
   Point result;

   // 경도
   result.x = (gps.center.y - standard.center.y) * (2 * _PI * Earth_R) * cos(gps.center.x) / 360;

   // 위도
   result.y = (gps.center.x - standard.center.x) * (Earth_R / 180 * _PI);

   return result;
}

bool check_inside(Circle circle, Point point) {

   double eq = pow(point.x - circle.center.x, 2) + pow(point.y - circle.center.y, 2);
   eq = sqrt(eq);

   if (eq <= circle.radius)
      return true;

   return false;
}

Point convert_XY_to_GPS(Circle standard, Point center) {
   Point result;

   result.x = standard.center.x + 360 * center.y / (2 * _PI * Earth_R);
   result.y = standard.center.y + 360 * center.x / (2 * _PI * Earth_R * cos(result.x));

   return result;
}

double calc_distance(Point gps1, Point gps2) {
   double dlong = (gps2.y - gps1.y) * DtoR;
   double dlat = (gps2.x - gps1.x) * DtoR;
   double a = pow(sin(dlat / 2), 2) + cos(gps1.x * DtoR) * cos(gps2.x * DtoR) * pow(sin(dlong / 2), 2);
   double c = 2 * atan2(sqrt(a), sqrt(1 - a));
   double d = Earth_R * c;

   return d;
}

Point find_intersection(Circle circle1, Circle circle2, Circle other) {
   Point p0, p1, p2;
   double r0, r1;
   p0.x = circle1.center.x;
   p0.y = circle1.center.y;
   r0 = circle1.radius;
   p1.x = circle2.center.x;
   p1.y = circle2.center.y;
   r1 = circle2.radius;

   double d = sqrt(pow(p0.x - p1.x, 2) + pow(p0.y - p1.y, 2));

   if (r0 + r1 < d) {
      PRINT_STR("%s", "Circles are not cross");
      PRINTLN;
      return p0;
   }

   double a = (pow(r0, 2) - pow(r1, 2) + pow(d, 2)) / (2 * d);
   double h = sqrt(pow(r0, 2) - pow(a, 2));

   p2.x = p0.x + a * (p1.x - p0.x) / d;
   p2.y = p0.y + a * (p1.y - p0.y) / d;

   if (r0 + r1 == d) {
      return p2;
   }

   Point p3[2];

   p3[0].x = p2.x + h * (p1.y - p0.y) / d;
   p3[0].y = p2.y - h * (p1.x - p0.x) / d;
   p3[1].x = p2.x - h * (p1.y - p0.y) / d;
   p3[1].y = p2.y + h * (p1.x - p0.x) / d;
   
   if (check_inside(other, p3[0]))
      return p3[0];

   return p3[1];
}



void find_location(Address addr[3]) {
   Circle GPS[3];

   for (int i = 0; i < 3; i++) {
      GPS[i].center.x = addr[i].latitude;
      GPS[i].center.y = addr[i].longitude;
      GPS[i].radius = addr[i].distance;
   }


   Circle circle[3];
   Point triangle[3];
   Point center;
   center.x = 0;
   center.y = 0;

   //Conver GPS to (x, y)
   for (int i = 0; i < 3; i++) {
      circle[i].center = convert_GPS_to_XY(GPS[0], GPS[i]);
      circle[i].radius = GPS[i].radius;
   }

   for (int i = 0; i < 3; i++) {
      triangle[i] = find_intersection(circle[i % 3], circle[(i + 1) % 3], circle[(i + 2) % 3]);

      center.x += triangle[i].x;
      center.y += triangle[i].y;
   }

   center.x /= 3;
   center.y /= 3;

   center = convert_XY_to_GPS(GPS[0], center);

   PRINT_STR("%s", "Center of Triangle : (");
   PRINT_VALUE("%d", center.x);
   PRINT_STR("%s", ", ");
   PRINT_VALUE("%d", center.y);
   PRINT_STR("%s", ")");
   PRINTLN;
}

void loop(){
  int i = 0, e;
  int cmdValue;
  int rssi = 0;
  uint8_t tmp_length;
  char message[100];
  double dis = 0;
  int src_addr = 0;
   
  e = sx1272.receiveAll(MAX_TIMEOUT);
  sx1272.getSNR();
  sx1272.getRSSIpacket();
  src_addr = sx1272.packet_received.src;
   
  if(src_addr != 0){
     PRINT_VALUE("%d", src_addr);
     PRINTLN;
     rssi = sx1272._RSSIpacket;
     PRINT_STR("%s", "RSSI : ");
       PRINT_VALUE("%d", rssi);
       PRINTLN;
       tmp_length = sx1272._payloadlength;



       for(int i = 0, j = 0; i < tmp_length; i++, j++){
          PRINT_STR("%c", (char)sx1272.packet_received.data[i]);
     
          message[i] = (char)sx1272.packet_received.data[i];
       }

         dis = (3 * pow(10, 8)) / (4 * 3.141592 * 868 * pow(10,6)) * pow(10, (fabs(rssi) / 20));
         addr[src_addr - 2].distance = dis;

     char temp[20];  

     for(int i = 0, j = 0;  i < tmp_length; i++, j++){
   
      if(message[i] == ','){
         j = -1;
         addr[src_addr - 2].latitude = atof(temp);
         continue;
      }

      temp[j] = message[i];
     }

     addr[src_addr - 2].longitude = atof(temp); 
      
     PRINT_STR("%s", "Packet Received");
     PRINT_STR("%s", "Destination : ");
     PRINT_VALUE("%d", src_addr);
     PRINTLN;
     PRINT_STR("%s", "Latitude : ");
     PRINT_VALUE("%f", addr[src_addr - 2].latitude);
     PRINTLN;
     PRINT_STR("%s", "Longitude : ");
     PRINT_VALUE("%f", addr[src_addr - 2].longitude);
     PRINTLN;
     PRINT_STR("%s", "Distance : ");
     PRINT_VALUE("%f", addr[src_addr - 2].distance);
     PRINT_STR("%s", "m");
     PRINTLN;
   
     find_location(addr);
   }
}


int main (int argc, char *argv[]){

  setup();
  
  while(1){
    loop();
  }
  
  return (0);
}
