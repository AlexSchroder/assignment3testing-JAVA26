#!/bin/bash

# Determine path separator based on OS
if [ "$(uname)" = "Linux" ] || [ "$(uname)" = "Darwin" ]; then
    SEP=":"
else
    SEP=";"
fi

echo ""
echo "Running the code with valid inputdata..."
java -cp ./bin${SEP} ltu.Main ./inputfiles/debug.csv
