#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/ec2-user/mapping/config/bin/dailyReports.csh > /home/ec2-user/mapping/logs/dailyReports.log
#
# Configure
#
set MAPPING_CODE=/home/ec2-user/mapping/code
set MAPPING_CONFIG=/home/ec2-user/mapping/config/config.properties
set MAPPING_DATA=/home/ec2-user/mapping/data

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"

echo "Taking down the server"
service tomcat7 stop
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
    echo "    Restarting tomcat7 server ...`/bin/date`"
    service tomcat7 start
    exit 1
endif

echo "    Restarting tomcat7 server ...`/bin/date`"
service tomcat7 start

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
