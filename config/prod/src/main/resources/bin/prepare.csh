#!/bin/csh -f
#
# Prerequisite
#  * "mvn" command must be in the PATH
#
# Setup environment
#
set MAPPING_CODE=/opt/mapping-admin
set MAPPING_CONFIG=/opt/mapping-rest/config.properties
set MAPPING_DATA=/opt/mapping-data

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"

echo "    Run updatedb with hibernate.hbm2ddl.auto = create ...`/bin/date`"
cd $MAPPING_CODE/updatedb
mvn install -PUpdatedb -Drun.config=$MAPPING_CONFIG -Dhibernate.hbm2ddl.auto=create >&! mvn.log
if ($status != 0) then
    echo "ERROR running updatedb"
    cat mvn.log
    exit 1
endif

echo "    Clear indexes ...`/bin/date`"
cd $MAPPING_CODE/lucene
mvn install -PReindex -Drun.config=$MAPPING_CONFIG >&! mvn.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat mvn.log
    exit 1
endif

echo "    Create admin user and empty project ...`/bin/date`"
cd $MAPPING_CODE/loader
mvn install -PCreateMapAdmin -Drun.config=$MAPPING_CONFIG -Dmap.user=admin >&! mvn.log
if ($status != 0) then
    echo "ERROR creating admin user and empty project"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
