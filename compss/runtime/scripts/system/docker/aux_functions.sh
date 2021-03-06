#!/bin/bash

function ERROR {
    echo -e "\\e[91m [  RUNCOMPSS-DOCKER  ]: [  ERROR  ]: $1 \\e[0m" 
}

function ECHO {
    echo -e "\\e[32m [  RUNCOMPSS-DOCKER  ]: $1 \\e[0m"
}

function ASSERT {
    if [ $? -ne 0 ]; then
    	echo
	    ERROR "$1"	
	    echo
	    exit 1
    fi
}
