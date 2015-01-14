#!/bin/csh -f
#
# Prerequisite
#  * "mvn" command must be in the PATH
#
# Setup environment
#
set MAPPING_CODE=/home/ihtsdo/code
set MAPPING_CONFIG=/home/ihtsdo/config/config.properties
set MAPPING_DATA=/home/ihtsdo/data

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_CODE"
echo "MAPPING_CONFIG = $MAPPING_CODE"

echo "    Run updatedb with hibernate.hbm2ddl.auto = create ...`/bin/date`"
cd $MAPPING_CODE/admin/updatedb
mvn install -PUpdatedb -Drun.config=$MAPPING_CONFIG -Dhibernate.hbm2ddl.auto=create >&! mvn.log
if ($status != 0) then
    echo "ERROR running updatedb"
    cat mvn.log
    exit 1
endif

echo "    Clear indexes ...`/bin/date`"
cd $MAPPING_CODE/admin/lucene
mvn install -PReindex -Drun.config=$MAPPING_CONFIG >&! mvn.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
