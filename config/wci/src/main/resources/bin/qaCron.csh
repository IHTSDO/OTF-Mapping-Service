#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/ec2-user/mapping/config/bin/qaCron.csh > /home/ec2-user/mapping/logs/qaCron.log
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
