#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/ihtsdo/config/bin/qaCron.csh > /home/ihtsdo/logs/qaCron.log
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

# this will send mail on a failure
echo "    Perform the database QA ... '/bin/date'"
cd $MAPPING_CODE/admin/qa
mvn install -PDatabase -Drun.config=$MAPPING_CONFIG | sed 's/^/    /'
if ($status != 0) then
    echo "ERROR running the database QA"
    cat mvn.log
    exit 1
endif

# this will send mail on a failure
# refset.id parameter left out - all refsets
echo "    Perform the workflow QA ... '/bin/date'"
cd $MAPPING_CODE/admin/qa
mvn install -PWorkflow -Drun.config=$MAPPING_CONFIG | sed 's/^/    /'
if ($status != 0) then
    echo "ERROR running the workflow QA"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
