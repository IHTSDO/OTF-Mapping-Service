#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/mapping-rest/config/bin/meddraToSqlReport.csh > /home/mapping-rest/logs/meddraToSqlReport.log
#
# Configure
#
set MAPPING_CODE=/opt/mapping-admin
set MAPPING_CONFIG=/opt/mapping-rest/config.properties
set MAPPING_DATA=/opt/mapping-data
set MAPPING_PROJECT_IDS=1,2

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"
echo "MAPPING_PROJECT_IDS = $MAPPING_PROJECT_IDS"


#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /home/mapping-rest/bin 
#

# Check the switch
echo "Check the switch"
if (-e /home/mapping-rest/bin/OFF) then
	echo "The switch is turned off, don't run"
	exit 1
endif

echo "Taking down the server"
sudo supervisorctl stop mapping-rest
if ($status != 0) then
	echo "ERROR stopping server"
	exit 1
endif

# this will send mail on a failure
echo "    MedDra SQL report ... '/bin/date'"
# start today

# no end date, all refsets
cd $MAPPING_CODE/loader
mvn install -PMeddraSqlReport -Drun.config=$MAPPING_CONFIG -Dreport.map.project.ids=$MAPPING_PROJECT_IDS | sed 's/^/    /'

if ($status != 0) then
    echo "ERROR generating daily reports"
    echo "    Restarting tomcat server ...`/bin/date`"
    sudo supervisorctl start mapping-rest
    exit 1
endif

echo "    Restarting tomcat server ...`/bin/date`"
sudo supervisorctl start mapping-rest

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
