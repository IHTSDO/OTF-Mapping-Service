#!/bin/csh -f
#
# Prerequisite
#  * "mvn" command must be in the PATH
#
# Sample cron configuration - run daily
# Minute Hour Day-of-Month Month Day of Week Command
# 0 0 * * * csh $MAPPING_CODE/admin/lucene/dailyReports.csh > reindex.log
#
# Rebuild Lucene indexes for specific objects listed in INDEX_OBJECTS  
#

# comma separated list of Jpa objects
set INDEX_OBJECTS=FeedbackConversationJpa

set MAPPING_CODE=/opt/mapping-admin
set MAPPING_CONFIG=/opt/mapping-rest/config.properties
set MAPPING_DATA=/opt/mapping-data

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"

cd $MAPPING_CODE/admin/lucene
mvn install -PReindex -Drun.config=$MAPPING_CONFIG -Dindexed.objects=$INDEX_OBJECTS >&! reindex.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat reindex.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
