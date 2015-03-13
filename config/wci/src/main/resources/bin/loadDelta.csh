#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/ec2-user/mapping/config/bin/loadDelta.csh > /home/ec2-user/mapping/logs/loadDelta.log
#
# Configure
# 
set MAPPING_CODE=/home/ec2-user/mapping/code
set MAPPING_CONFIG=/home/ec2-user/mapping/config/config.properties
set MAPPING_DATA=/home/ec2-user/mapping/data
set SNOMEDCT_VERSION=20150131

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
    echo "ERROR deleting delta data"
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

echo "    Unzip delta files into wb-release-process-1.20-SNAPSHOT-delta ... `/bin/date`"
cd ~/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.20-SNAPSHOT
unzip wb-release-process-1.20-SNAPSHOT-delta.zip -d wb-release-process-1.20-SNAPSHOT-delta
if ($status != 0) then
    echo "ERROR unzipping delta data"
    exit 1
endif

echo "    Load the delta ... `/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PRF2-delta -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT \
  -Dlast.publication.date=$SNOMEDCT_VERSION \
  -Dinput.dir=/home/ec2-user/mapping/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.20-SNAPSHOT/wb-release-process-1.20-SNAPSHOT-delta/destination/Delta | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR processing delta data"
    exit 1
endif

echo "    Remove SNOMEDCT tree positions ... `/bin/date`"
cd $MAPPING_CODE/admin/remover
mvn install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dversion=latest | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR removing tree positions"
    exit 1
endif

echo "    Generate SNOMEDCT tree positions ... `/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dversion=latest -Droot.ids=138875005 | sed 's/^/      /'
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
