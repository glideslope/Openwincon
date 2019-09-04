
#include <cstdlib>
#include <string>
#include <iomanip>
#include <iostream>

#include "ContCommand.h"

using namespace std;

int main(int argc, char** argv) {
    
    string cmd = "";
    ContCommand cc;
    
    if(argc == 2 || argc == 4 || argc == 5) {
        
        for(int i = 1; i  < argc; i++) {
            cmd.append(argv[i]);
            cmd.append("|");
        }
        cmd.erase(cmd.length() - 1);
        
        cc.CommandProcess(cmd);
    }
    else {
        cout << "[error] Invalid Command!!" << endl;
        cc.PrintHelp();
    }
        
    return 0;
}

