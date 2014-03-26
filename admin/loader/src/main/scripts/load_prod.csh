#!/bin/csh -f

#
# Check OTF_CODE_HOME
#
if ($?OTF_MAPPING_HOME == 0) then
	echo "ERROR: OTF_MAPPING_HOME must be set"
	exit 1
endif

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"

echo "    Run updatedb with hibernate.hbm2ddl.auto = update ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/updatedb
mvn -Drun.config=prod -Dhibernate.hbm2ddl.auto=create install >&! mvn.log
if ($status != 0) then
    echo "ERROR running updatedb"
    cat mvn.log
    exit 1
endif

echo "    Clear indexes ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/lucene
mvn -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR running lucene"
    cat mvn.log
    exit 1
endif

echo "    Load SNOMEDCT ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PSNOMEDCT -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading SNOMEDCT"
    cat mvn.log
    exit 1
endif


echo "    Load ICPC ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PICPC -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC"
    cat mvn.log
    exit 1
endif

echo "    Load ICD10 ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PICD10 -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD10"
    cat mvn.log
    exit 1
endif

echo "    Load ICD9CM ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PICD9CM -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICD9CM"
    cat mvn.log
    exit 1
endif

echo "    Import project data ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/import
mvn -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR importing project data"
    cat mvn.log
    exit 1
endif

echo "    Create ICD10 map records ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PCreateMapRecords -Drun.config=prod -Drefset.id=447562003 install >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD10 and ICD9CM map records"
    cat mvn.log
    exit 1
endif

echo "    Create ICD9CM map records ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PCreateMapRecords -Drun.config=prod -Drefset.id=447563008 install >&! mvn.log
if ($status != 0) then
    echo "ERROR creating ICD10 and ICD9CM map records"
    cat mvn.log
    exit 1
endif

echo "    Load ICPC maps from file ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PMapRecords -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading ICPC map records"
    cat mvn.log
    exit 1
endif

echo "    Load map notes from file ...`/bin/date`"
cd $OTF_MAPPING_HOME/admin/loader
mvn -PMapNotes -Drun.config=prod install >&! mvn.log
if ($status != 0) then
    echo "ERROR loading map notes"
    cat mvn.log
    exit 1
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
