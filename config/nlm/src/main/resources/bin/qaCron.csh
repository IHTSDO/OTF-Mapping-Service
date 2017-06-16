#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/ihtsdo/config/bin/qaCron.csh > /home/ihtsdo/logs/qaCron.log
#

#
# Configure
#
set MAPPING_CODE=/opt/mapping-service-admin
set MAPPING_CONFIG=/opt/mapping-service/conf/config.properties

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"


#
# This script is controlled by a switch
# The switch is on by default.  To turn it off
# Create a file called "OFF" in /opt/mapping-service-admin/config/bin
#

# Check the switch
echo "Check the switch"
if (-e /opt/mapping-service-admin/config/bin/OFF) then
        echo "The switch is turned off, don't run"
        exit 1
endif

# this will send mail on a failure
echo "    Perform the database QA ... '/bin/date'"
cd $MAPPING_CODE/admin/qa
mvn install -PDatabase -Drun.config=$MAPPING_CONFIG | sed 's/^/    /'
if ($status != 0) then
    echo "ERROR running the database QA"
    exit 1
endif

# this will send mail on a failure
# refset.id parameter left out - all refsets
echo "    Perform the workflow QA ... '/bin/date'"
cd $MAPPING_CODE/admin/qa
mvn install -PWorkflow -Drun.config=$MAPPING_CONFIG | sed 's/^/    /'
if ($status != 0) then
    echo "ERROR running the workflow QA"
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"