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
set SNOMEDCT_VERSION=20160731
set dir=/home/ihtsdo/data/dripFeed

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

echo "    Delete current wb-release-process-1.21-SNAPSHOT-delta file ...`/bin/date`"
cd $dir
rm -fr $dir/*_INT_*txt
if ($status != 0) then
    echo "ERROR deleting old delta data"
    exit 1
endif

# THIS MAY CHANGE - obviously dates are a one-time thing
echo "    Obtain latest release ...`/bin/date`"
touch der2_cRefset_AssociationReferenceDelta_INT_$version.txt
touch der2_cRefset_AttributeValueDelta_INT_$version.txt
touch der2_Refset_SimpleDelta_INT_$version.txt
touch der2_sRefset_SimpleMapDelta_INT_$version.txt
touch sct2_TextDefinition_Delta-en_INT_$version.txt
wget https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/2016-01-14T11:11:36/outputfiles/sct2_Concept_Delta_INT_20160731.txt
wget https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/2016-01-14T11:11:36/outputfiles/sct2_Description_Delta-en_INT_20160731.txt
wget https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/2016-01-14T11:11:36/outputfiles/der2_cRefset_LanguageDelta-en_INT_20160731.txt
wget https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/2016-01-14T11:11:36/outputfiles/sct2_Relationship_Delta_INT_20160731.txt
wget https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/2016-01-14T11:11:36/outputfiles/sct2_StatedRelationship_Delta_INT_20160731.txt

if (`ls $dir/*_INT_*txt | wc -l` != 10) then
    echo "ERROR retrieving latest delta data"
    exit 1
endif

echo "    Load the delta ... `/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PRF2-delta -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT \
  -Dlast.publication.date=$SNOMEDCT_VERSION \
  -Dinput.dir=$dir | sed 's/^/      /'
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
