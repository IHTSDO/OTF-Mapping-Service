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

echo "    Clear workflow ...`/bin/date`"
cd $MAPPING_CODE/admin/remover
mvn -PClearWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008,447562003,450993002 install >&! mvn.log
if ($status != 0) then
    echo "ERROR clearing workflow"
    cat mvn.log
    exit 1
endif

echo "    Remove map notes ...`/bin/date`"
cd $MAPPING_CODE/admin/remover
mvn -PMapNotes -Drefset.id=447562003,447563008,450993002 -Drun.config=$MAPPING_CONFIG install >&! mvn.log
if ($status != 0) then
    echo "ERROR removing map notes"
    cat mvn.log
    exit 1
endif

echo "    Remove mapping records ...`/bin/date`"
cd $MAPPING_CODE/admin/remover
mvn -PMapRecords -Drefset.id=447562003,447563008,450993002 -Drun.config=$MAPPING_CONFIG install >&! mvn.log
if ($status != 0) then
    echo "ERROR removing map records"
    cat mvn.log
    exit 1
endif

echo "    Remove map project data ...`/bin/date`"
cd $MAPPING_CODE/admin/remover
mvn -PMapProject -Drun.config=$MAPPING_CONFIG install >&! mvn.log
if ($status != 0) then
    echo "ERROR removing map record data"
    cat mvn.log
    exit 1
endif

echo "    Import project data ...`/bin/date`"
cd $MAPPING_CODE/admin/import
mvn -PMapRecords -Drun.config=$MAPPING_CONFIG -DinputDir=$MAPPING_DATA/ihtsdo-project-data install >&! mvn.log
if ($status != 0) then
    echo "ERROR importing project data"
    cat mvn.log
    exit 1
endif

echo "    Create ICD10 map records ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PCreateMapRecords -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003 install >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD10 map records"
    cat mvn.log
    exit 1
endif

echo "    Create ICD9CM map records ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PCreateMapRecords -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008 install >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD9CM map records"
    cat mvn.log
    exit 1
endif

echo "    Load ICPC maps from file ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PMapRecords -Drun.config=$MAPPING_CONFIG -Dinput.file=$MAPPING_DATA/der2_iisssccRefset_ExtendedMapSnapshot_INT_20140131.txt install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC map records"
    cat mvn.log
    exit 1
endif

echo "    Load map notes from file ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PMapNotes -Drun.config=$MAPPING_CONFIG -Dinput.file=$MAPPING_DATA/der2_sRefset_MapNotesSnapshot_INT_20140131.txt install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading map notes"
    cat mvn.log
    exit 1
endif

echo "    Compute workflow ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PComputeWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008,447562003,450993002 install >&! mvn.log
if ($status != 0) then
    echo "ERROR computing workflow"
    cat mvn.log
    exit 1
endif


echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
