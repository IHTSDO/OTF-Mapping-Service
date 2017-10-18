#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/ihtsdo/config/bin/dailyReports.csh > /home/ihtsdo/logs/dailyReports.log
#
# Configure
#
set MAPPING_CODE=/home/ihtsdo/code
set MAPPING_CONFIG=/home/ihtsdo/config/config.properties
set MAPPING_DATA=/home/ihtsdo/data

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"


#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /home/ihtsdo/bin 
#

# Check the switch
echo "Check the switch"
if (-e /home/ihtsdo/bin/OFF) then
	echo "The switch is turned off, don't run"
	exit 1
endif

echo "Taking down the server"
service tomcat stop
if ($status != 0) then
	echo "ERROR stopping server"
	exit 1
endif

# this will send mail on a failure
echo "    Generate daily reports ... '/bin/date'"
# start today
set today = `/bin/date +%Y%m%d`
# no end date, all refsets
cd $MAPPING_CODE/admin/loader
mvn install -PGenerateDailyReports -Drun.config=$MAPPING_CONFIG -Dstart.date=$today | sed 's/^/    /'

if ($status != 0) then
    echo "ERROR generating daily reports"
    echo "    Restarting tomcat server ...`/bin/date`"
    service tomcat start
    exit 1
endif

echo "    Restarting tomcat server ...`/bin/date`"
service tomcat start

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
