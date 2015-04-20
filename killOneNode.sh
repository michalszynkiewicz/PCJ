#!/bin/bash

NODES_STRING=$(pgrep -f pcjNodeDiscriminator)
echo "nodes string: ${NODES_STRING}"
IFS=$'\n'
sorted=($(printf '%s\n' "$NODES_STRING"|sort))
echo "[KILLER] grepped: $sorted"

CHOSEN=${sorted[3]}

echo "CHOSEN NODE: $CHOSEN"
kill $CHOSEN