#!/bin/bash

# Test if there are any stale java source files

files=$(mvn -X test-compile | grep "Stale source detected:")
if [[ $? != 0 ]]; then
    echo "mvn command failed."
elif [[ $files ]]; then
    echo "Some output from maven"
    echo $files
else
    echo "No output"
fi
