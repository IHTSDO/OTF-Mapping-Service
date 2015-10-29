#!/bin/csh -f
#
# Prerequisite
#  * "mvn" command must be in the PATH
#
# Setup environment
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
mvn install -PUpdatedb -Drun.config=$MAPPING_CONFIG -Dhibernate.hbm2ddl.auto=create >&! mvn.log
if ($status != 0) then
    echo "ERROR running updatedb"
    cat mvn.log
    exit 1
endif

echo "    Clear indexes ...`/bin/date`"
cd $MAPPING_CODE/admin/lucene
mvn install -PReindex -Drun.config=$MAPPING_CONFIG >&! mvn.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat mvn.log
    exit 1
endif

echo "    Load SNOMEDCT ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PRF2-snapshot -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dinput.dir=$MAPPING_DATA/snomedct-20140731-snapshot >&! mvn.log
if ($status != 0) then
    echo "ERROR loading SNOMEDCT"
    cat mvn.log
    exit 1
endif


echo "    Load ICPC ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICPC -Dversion=2 -Dinput.file=$MAPPING_DATA/icpc-2.xml >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC"
    cat mvn.log
    exit 1
endif

echo "    Load ICD10 ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICD10 -Dversion=2010 -Dinput.file=$MAPPING_DATA/icd10-2010.xml >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD10"
    cat mvn.log
    exit 1
endif

echo "    Load ICD9CM ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PClaML -Drun.config=$MAPPING_CONFIG -Dterminology=ICD9CM -Dversion=2013 -Dinput.file=$MAPPING_DATA/icd9cm-2013.xml >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD9CM"
    cat mvn.log
    exit 1
endif

echo "    Import project data ...`/bin/date`"
cd $MAPPING_CODE/admin/import
mvn install -PMapProject -Drun.config=$MAPPING_CONFIG -Dinput.dir=$MAPPING_DATA/ihtsdo-project-data >&! mvn.log
if ($status != 0) then
    echo "ERROR importing project data"
    cat mvn.log
    exit 1
endif

echo "    Create ICD10 map records ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PCreateMapRecords -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003 >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD10 map records"
    cat mvn.log
    exit 1
endif

echo "    Create ICD9CM map records ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PCreateMapRecords -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008 >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD9CM map records"
    cat mvn.log
    exit 1
endif

echo "    Load ICPC maps from file ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PMapRecords -Drun.config=$MAPPING_CONFIG -Dinput.file=$MAPPING_DATA/der2_iisssccRefset_ExtendedMapSnapshot_INT_20140131.txt >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC map records"
    cat mvn.log
    exit 1
endif

echo "    Load map notes from file ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PMapNotes -Drun.config=$MAPPING_CONFIG -Dinput.file=$MAPPING_DATA/der2_sRefset_MapNotesSnapshot_INT_20140131.txt >&! mvn.log
if ($status != 0) then
    echo "ERROR loading map notes"
    cat mvn.log
    exit 1
endif

echo "    Compute workflow ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PComputeWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008,447562003,450993002 >&! mvn.log
if ($status != 0) then
    echo "ERROR computing workflow"
    cat mvn.log
    exit 1
endif

echo "    Begin editing cycle for ICD10 ...`/bin/date`"
cd $MAPPING_CODE/admin/release
mvn install -PBeginEditingCycle -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003 >&! mvn.log
if ($status != 0) then
    echo "ERROR beginning editing cycle for ICD10"
    cat mvn.log
    exit 1
endif

echo "    Begin editing cycle for ICD9CM ...`/bin/date`"
cd $MAPPING_CODE/admin/release
mvn install -PBeginEditingCycle -Drun.config=$MAPPING_CONFIG -Drefset.id=447563008 >&! mvn.log
if ($status != 0) then
    echo "ERROR beginning editing cycle for ICD9CM"
    cat mvn.log
    exit 1
endif

echo "    Begin editing cycle for ICD10 ...`/bin/date`"
cd $MAPPING_CODE/admin/release
mvn install -PBeginEditingCycle -Drun.config=$MAPPING_CONFIG -Drefset.id=450993002 >&! mvn.log
if ($status != 0) then
    echo "ERROR beginning editing cycle for ICD10"
    cat mvn.log
    exit 1
endif


echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
