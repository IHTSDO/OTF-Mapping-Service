#!/bin/csh -f

#
# Check OTF_CODE_HOME
#
if ($?OTF_MAPPING_HOME == 0) then
	echo "ERROR: OTF_MAPPING_HOME must be set"
	exit 1
endif

#
# Check OTF_MAPPING_CONFIG
#
if ($?OTF_MAPPING_CONFIG == 0) then
	echo "ERROR: OTF_MAPPING_CONFIG must be set"
	exit 1
endif

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"

echo "    Obtain latest release ...`/bin/date`"
cd $OTF_MAPPING_HOME/data
mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get \
  -DgroupId=org.ihtsdo.intl.release.process -DartifactId=wb-release-process \
  -Dclassifier=delta -Dversion=1.18-SNAPSHOT -Dpackaging=zip \
  -Dtransitive=false
if ($status != 0) then
    echo "ERROR retrieving latest delta data"
    cat mvn.log
    exit 1
endif

echo "    Unzip delta files into data directory ... '/bin/date'"
cd /home/ihtsdo/.m2/repository/org/ihtsdo/intl/release/process/wb-release-process/1.18-SNAPSHOT
unzip wb-release-process-1.18-SNAPSHOT-delta.zip
if ($status != 0) then
    echo "ERROR unzipping delta data"
    cat mvn.log
    exit 1
endif

echo "    Load the delta ... '/bin/date'"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PSNOMEDCTDelta -Drun.config=$OTF_MAPPING_CONFIG
if ($status != 0) then
    echo "ERROR unzipping delta data"
    cat mvn.log
    exit 1
endif

echo "    Remove SNOMEDCT tree positions ... '/bin/date'"
cd $OTF_MAPPING_HOME/admin/remover
mvn -PSNOMEDCT-treepos -Drun.config=$OTF_MAPPING_CONFIG
if ($status != 0) then
    echo "ERROR removing tree positions"
    cat mvn.log
    exit 1
endif

echo "    Generate SNOMEDCT tree positions ... 'bin/date'"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PSNOMEDCT-treepos -Drun.config=$OTF_MAPPING_CONFIG
if ($status != 0) then
    echo "ERROR computing tree positions"
    cat mvn.log
    exit 1
endif

echo "    Compute workflow ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PComputeWorkflow -Drun.config=$OTF_MAPPING_CONFIG -Drefset.id=447563008,447562003,450993002 install >&! mvn.log
if ($status != 0) then
    echo "ERROR computing workflow"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
