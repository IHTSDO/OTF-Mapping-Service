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
# no longer set this
# set SNOMEDCT_VERSION=20160731
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

# Redeploy code
#/bin/rm -rf /var/lib/tomcat7/work/Catalina/localhost/mapping-rest
#/bin/rm -rf /var/lib/tomcat7/webapps/mapping-rest
#/bin/rm -rf /var/lib/tomcat7/webapps/ROOT
#/bin/rm -rf /var/lib/tomcat7/webapps/mapping-rest.war
#/bin/rm -rf /var/lib/tomcat7/webapps/ROOT.war

#/bin/cp -f ~/code/rest/target/mapping-rest*war /var/lib/tomcat7/webapps/mapping-rest.war
#/bin/cp -f ~/code/webapp/target/mapping-webapp*war /var/lib/tomcat7/webapps/ROOT.war

echo "    Delete last delta ...`/bin/date`"
cd $dir
rm -fr $dir/*
# OK if that fails

# THIS MAY CHANGE - obviously dates are a one-time thing
echo "    Obtain latest release ...`/bin/date`"
wget "https://release.ihtsdotools.org/api/v1/centers/international/products/snomed_ct_ts_release/builds/" -O /tmp/xx.$$.json
if ($status != 0) then
    echo "ERROR downloading builds file"
    exit 1
endif

set latestRelease = `grep outputfiles_url /tmp/xx.$$.json | sort -n | tail -1 | cut -d\: -f 2- | sed 's/ "//; s/",//'`
if ($status != 0) then
    echo "ERROR determining latest release"
    exit 1
endif

wget $latestRelease -O /tmp/yy.$$.json
if ($status != 0) then
    echo "ERROR downloading latest release directory"
    exit 1
endif

set zipFile = `grep '.zip"' /tmp/yy.$$.json | grep '"url"' | cut -d\: -f 2- | sed 's/ "//; s/"//'`
if ($status != 0) then
    echo "ERROR determining zip file"
    exit 1
endif

wget $zipFile
if ($status != 0) then
    echo "ERROR downloading .zip"
    exit 1
endif

echo "    Unpack the delta ...`/bin/date`"
unzip $zipFile:t "*/Delta/*"
if ($status != 0) then
    echo "ERROR unpacking .zip"
    exit 1
endif

/bin/mv `find */Delta -name "*txt"` .
if ($status != 0) then
    echo "ERROR  moving delta files to current dir"
    exit 1
endif

/bin/rm -rf *zip /tmp/{xx,yy}.$$.json

# continue only if delta concepts file is not empty
if (`grep -v effectiveTime *Concept*txt | wc -l` > 0) then

        echo "    Load the delta ... `/bin/date`"
        cd $MAPPING_CODE/admin/loader
        # don't do this anymore: -Dlast.publication.date=$SNOMEDCT_VERSION \
        # null value leads to correct computation
        mvn install -PRF2-delta -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT \
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

        # optimize tree positions table
        echo "Clear indexes directory"
        /bin/rm -rf /var/lib/tomcat7/indexes/lucene/indexes/org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa/*
        echo "Reindex other tree positions"
         cd $MAPPING_CODE/admin/lucene
        mvn install -PReindex -Drun.config=$MAPPING_CONFIG -Dindexed.objects=TreePositionJpa | sed 's/^/      /'


        echo "    Generate SNOMEDCT tree positions ... `/bin/date`"
        cd $MAPPING_CODE/admin/loader
        mvn install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT -Dversion=latest -Droot.ids=138875005 | sed 's/^/      /'
        if ($status != 0) then
            echo "ERROR computing tree positions"
            exit 1
        endif

else
    echo "Concepts file is empty"
endif

echo "    Compute workflow ...`/bin/date`"
cd $MAPPING_CODE/admin/loader
mvn install -PComputeWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003,467614008,446608001 -Dsend.notification=true | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR computing workflow"
    exit 1
endif

echo "    Restarting tomcat7 server ...`/bin/date`"
service tomcat7 start

# reconnect "doc" directory
#sleep 40

#cd /var/lib/tomcat7/webapps/ROOT
#ln -s ~ihtsdo/data/doc
#chmod -R ga+rwx ~ihtsdo/data/doc

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
