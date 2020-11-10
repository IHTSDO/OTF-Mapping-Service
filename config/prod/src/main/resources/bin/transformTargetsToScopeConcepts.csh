#!/bin/csh -f
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh /home/mapping-rest/config/bin/transformTargetsToScopeConcepts.csh > /home/mapping-rest/logs/transformTargetsToScopeConcepts.log
#
# Configure
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
supervisorctl stop mapping-rest
if ($status != 0) then
	echo "ERROR stopping server"
	exit 1
endif

# this will send mail on a failure
echo "    Transform Targets To Scope Concepts ... '/bin/date'"
# source project (to get target concepts)
set source = `MedDRA to SNOMEDCT with REVIEW`
# target project (to load scoped concepts)
set target = `SNOMEDCT to MedDRA with REVIEW`


cd $MAPPING_CODE/loader
mvn install -PTargetMappingToScopedConcepts -Drun.config=$MAPPING_CONFIG -Dsource.map.project.name=$source -Dtarget.map.project.name=$target | sed 's/^/    /'

if ($status != 0) then
    echo "ERROR Transforming Targets To Scope Concepts"
    echo "    Restarting tomcat server ...`/bin/date`"
    supervisorctl start mapping-rest
    exit 1
endif

echo "    Restarting tomcat server ...`/bin/date`"
supervisorctl start mapping-rest

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
