#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/ihtsdo/config/bin/loadDelta.csh > /home/ihtsdo/logs/loadDelta.log
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

echo "Taking down the server"
service tomcat7 stop
if ($status != 0) then
	echo "ERROR stopping server"
	exit 1
endif

echo "    Delete current wb-release-process-1.20-SNAPSHOT-delta file ...`/bin/date`"
cd ~/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.20-SNAPSHOT
rm -fr wb-release-process-1.20-SNAPSHOT-delta
if ($status != 0) then
    echo "ERROR retrieving latest delta data"
    exit 1
endif


echo "    Obtain latest release ...`/bin/date`"
cd $MAPPING_DATA
mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get \
  -DgroupId=org.ihtsdo.intl.release.process -DartifactId=wb-release-process \
  -Dclassifier=delta -Dversion=1.20-SNAPSHOT -Dpackaging=zip \
  -Dtransitive=false
if ($status != 0) then
    echo "ERROR retrieving latest delta data"
    exit 1
endif

echo "    Unzip delta files into wb-release-process-1.20-SNAPSHOT-delta ... '/bin/date'"
cd ~/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.20-SNAPSHOT
unzip wb-release-process-1.20-SNAPSHOT-delta.zip -d wb-release-process-1.20-SNAPSHOT-delta
if ($status != 0) then
    echo "ERROR unzipping delta data"
    exit 1
endif

echo "    Load the delta ... '/bin/date'"
cd $MAPPING_CODE/admin/loader
mvn install -PRF2-delta -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dinput.dir=/home/ihtsdo/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.20-SNAPSHOT/wb-release-process-1.20-SNAPSHOT-delta | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR processing delta data"
    exit 1
endif

echo "    Remove SNOMEDCT tree positions ... '/bin/date'"
cd $MAPPING_CODE/admin/remover
mvn install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR removing tree positions"
    exit 1
endif

echo "    Generate SNOMEDCT tree positions ... 'bin/date'"
cd $MAPPING_CODE/admin/loader
mvn install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR computing tree positions"
    exit 1
endif

echo "    Compute workflow ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PComputeWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008,447562003,450993002 -Dsend.notification=true | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR computing workflow"
    exit 1
endif

echo "    Restarting tomcat7 server ...`/bin/date`"
service tomcat7 start

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
