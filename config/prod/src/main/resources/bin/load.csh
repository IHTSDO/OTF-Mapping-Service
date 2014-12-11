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

echo "    Run updatedb with hibernate.hbm2ddl.auto = create ...`/bin/date`"
cd $MAPPING_CODE/admin/updatedb
mvn -Drun.config=$MAPPING_CONFIG -Dhibernate.hbm2ddl.auto=create install >&! mvn.log
if ($status != 0) then
    echo "ERROR running updatedb"
    cat mvn.log
    exit 1
endif

echo "    Clear indexes ...`/bin/date`"
cd $MAPPING_CODE/admin/lucene
mvn -Drun.config=$MAPPING_CONFIG install >&! mvn.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat mvn.log
    exit 1
endif

echo "    Load SNOMEDCT ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PRF2-snapshot -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dinput.dir=$MAPPING_DATA/snomedct-20140731-snapshot install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading SNOMEDCT"
    cat mvn.log
    exit 1
endif


echo "    Load ICPC ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICPC -Dinput.file=$MAPPING_DATA/icpc-2.xml install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC"
    cat mvn.log
    exit 1
endif

echo "    Load ICD10 ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICD10 -Dinput.file=$MAPPING_DATA/icd10-2010.xml install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD10"
    cat mvn.log
    exit 1
endif

echo "    Load ICD9CM ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICD9CM -Dinput.file=$MAPPING_DATA/icd9cm-2013.xml install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD9CM"
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
