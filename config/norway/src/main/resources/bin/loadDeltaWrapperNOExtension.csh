#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/ubuntu/data/mapping-admin/scripts/loadDeltaWrapper.csh > /home/ubuntu/data/mapping-admin/scripts/loadDelta.log
#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /home/ubuntu/data/mapping-admin/scripts
#

set MAPPING_CODE=/home/ubuntu/data/mapping-admin
set MAPPING_CONFIG=/home/ubuntu/data/mapping-rest/config.properties

# set error message variables
set ERROR_SUBJECT="[OTF-Mapping-Tool] Drip feed error"
set ERROR_BODY="The drip feed did not complete due to the following issue(s):\n"

# Check the switch
echo "Check the switch"
if (-e /home/ubuntu/data/mapping-admin/scripts/OFF) then
	echo "The switch is turned off, don't run"
        set ERROR_BODY="${ERROR_BODY}\nThe run switch is turned off."
        cd $MAPPING_CODE/loader
        mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
	exit 1
endif

# Runs load delta and ensures server is up
echo "run load delta"
nohup /home/ubuntu/data/mapping-admin/scripts/loadDelta_NO.csh
set x = $status
if ($x != 0) then
    cd /home/ubuntu/project/OTF-Mapping-Service
	set date=`/bin/date +%Y%m%d-%s`
	nohup java -Xmx4500M -Xms500M -Drun.config=/home/ubuntu/data/mapping-rest/config.properties \
  -jar webapp-runner.jar --port 8080 rest/target/mapping-rest.war > /home/ubuntu/logs/log.$date.txt 2>&1 &
    return $x
endif
