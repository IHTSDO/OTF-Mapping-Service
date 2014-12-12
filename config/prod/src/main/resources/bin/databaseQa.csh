#!/bin/csh -f

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
echo "MAPPING_DATA = $MAPPING_CODE"
echo "MAPPING_CONFIG = $MAPPING_CODE"

# this will send mail on a failure
echo "    Perform the QA ... '/bin/date'"
cd $MAPPING_CODE/admin/qa
mvn -PDatabase -Drun.config=$MAPPING_CONFIG install
if ($status != 0) then
    echo "ERROR running the QA"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
