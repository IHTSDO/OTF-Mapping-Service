#!/bin/bash

mvn -f ./refset-admin-pom.xml install -PSample2 -Drefset.config=/opt/refset-service/conf/configload.properties -Dmode=create

