#!/bin/bash

yes "this is a file" | head -c $1 > data.txt
du -h data.txt | awk '{print $1}'
