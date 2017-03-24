#!/bin/bash

cd ./zips

rm -rf ../config*
mkdir -p ../config/src/main/resources/data
 

find . -maxdepth 1 -type f  -name "refset-config-[0-9]*.zip" | xargs unzip -d ../config/src/main/resources/data
find . -maxdepth 1 -type f  -name "refset-config-uat*.zip" | xargs unzip -d ../config-uat
find . -maxdepth 1 -type f  -name "refset-config-prod*.zip" | xargs unzip -d ../config-prod
