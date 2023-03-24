#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /home/ubuntu/data/mapping-admin/scripts/loadDelta.csh > /home/ubuntu/data/mapping-admin/scripts/loadDelta.log
#
# Configure
#
echo "setting variables in delta loader"
set MAPPING_CODE=/home/ubuntu/data/mapping-admin
set MAPPING_CONFIG=/home/ubuntu/data/mapping-rest/config.properties
set MAPPING_DATA=/home/ubuntu/data/mapping-data

# set error message variables
set ERROR_SUBJECT="[OTF-Mapping-Tool] Drip feed error"
set ERROR_BODY="The drip feed did not complete due to the following issue(s):\n"

set relUrl=https://release.ihtsdotools.org/api/centers/no/products/snomed_ct_no_daily_build/builds/
set outFile=/outputfiles
set tempdir=/tmp/dripFeed

set dir=/home/ubuntu/data/mapping-data/dripFeed

set buildStatus = "Progress Status"
set buildOk = "completed,"
set buildOk2 = "completed"
set numrels = 50

set MAVEN_OPTS="-Xmx14000M"

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"

echo "Taking down the server"
pkill -9 -f webapp
if ($status != 0) then
        echo "ERROR stopping server"
        set ERROR_BODY="${ERROR_BODY}\nError stopping server."
        cd $MAPPING_CODE/loader
        mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
        exit 1
endif

#make sure temp dirs exist
mkdir -p $tempdir
mkdir -p $dir

echo "    Delete last delta ...`/bin/date`"
cd $dir
rm -fr $dir/*
 OK if that fails

# clean up temp dir

rm -f $tempdir/buildrep.*.json
rm -f $tempdir/xx.*.json

# log into IMS to get authentication token
set ims_url=https://ims.ihtsdotools.org
set ims_username=mapping-prod
set ims_password='Rsd&9&Nx8u#HK26'
# Create temporary cookie file
wget --keep-session-cookies --save-cookies /tmp/cookies.txt --header="Content-Type:application/json" --post-data '{"login":"'"$ims_username"'","password":"'"$ims_password"'","rememberMe":"false"}' ${ims_url}/api/authenticate

echo "    Obtain latest release ...`/bin/date`"
wget --load-cookies /tmp/cookies.txt "$relUrl" -O $tempdir/xx.$$.json
if ($status != 0) then
    echo "ERROR downloading builds file"
    set ERROR_BODY="${ERROR_BODY}\nERROR downloading builds file"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

set latestRelease = ""
set buildOKbool = false
set j = 1
while ($buildOKbool == false && $j <= $numrels)
echo "j = "$j

set latestRelease = `cat $tempdir/xx.$$.json | jq -r '.content[] | .outputfiles_url' | head -n$j | tail -1`
set buildreport = `cat $tempdir/xx.$$.json | jq -r '.content[] | .buildReport_url' | head -n$j | tail -1`
set releaseName = `cat $tempdir/xx.$$.json | jq -r '.content[] | .id' | head -n$j | tail -1`

echo "latest release = "$latestRelease
echo "buildreport = "$buildreport
echo "release name = "$releaseName

#get the buildreport and see if the Progess Status is completed

wget --load-cookies /tmp/cookies.txt "$buildreport" -O $tempdir/buildrep.$$.json
set buildstat = `cat $tempdir/buildrep.$$.json | jq -r '."Progress Status"'`

echo "buildstat = "$buildstat
if($buildstat == $buildOk || $buildstat == $buildOk2) then
echo "buildOK"
set buildOKbool = true
echo "OK release = "$latestRelease
else
echo "Build not ok val = "$buildstat
endif
@ j++
end


echo "buildOKbool = " $buildOKbool
if($buildOKbool == true) then
set fullfile=$tempdir/$releaseName.json
if ( -f $fullfile ) then
    echo "full file already exists should bug out now as have seen it before. fullfile = "$fullfile
    set ERROR_BODY="${ERROR_BODY}\nThe latest available build has already been processed previously."
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
else
  echo "fullfile does not exist will check it. fullfile = "$fullfile
endif

wget --load-cookies /tmp/cookies.txt $latestRelease -O $tempdir/$releaseName.json
if ($status != 0) then
    echo "ERROR downloading latest release directory"
    set ERROR_BODY="${ERROR_BODY}\nERROR downloading latest release directory"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

endif

# identify zip file - should be one of the top ten objects
set k = 1
set zipFile = notFound
while ($k<10 && $zipFile == notFound)
echo "k = "$k

set fileName = `cat $tempdir/$releaseName.json | jq -r '.[] | .id' | head -n$k | tail -1`

if ( "$fileName" =~ *.zip ) then
  set zipFile = `cat $tempdir/$releaseName.json | jq -r '.[] | .url' | head -n$k | tail -1`
  echo "zip file="$zipFile
endif

@ k++
end

if ($zipFile == notFound) then
    echo "ERROR determining zip file"
    set ERROR_BODY="${ERROR_BODY}\nERROR determining zip file"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

wget --load-cookies /tmp/cookies.txt $zipFile
if ($status != 0) then
    echo "ERROR downloading .zip"
    set ERROR_BODY="${ERROR_BODY}\nERROR downloading .zip"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

echo "    Unpack the delta ...`/bin/date`"
unzip $zipFile:t "*/Delta/*"
if ($status != 0) then
    echo "ERROR unpacking .zip"
    set ERROR_BODY="${ERROR_BODY}\nERROR unpacking .zip"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

/bin/mv `find */Delta -name "*txt"` .
if ($status != 0) then
    echo "ERROR  moving delta files to current dir"
    set ERROR_BODY="${ERROR_BODY}\nERROR moving delta files to current dir"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

/bin/rm -rf *zip /tmp/{xx,yy}.$$.json

# continue only if delta concepts file is not empty
if (`grep -v effectiveTime *Concept*txt | wc -l` > 0) then

       echo "    combine language-specific files into single file ... `/bin/date`"
       cd $dir
       set f1=`find -type f -name 'der2_cRefset_LanguageDelta-en*.txt'`
       echo $f1
       set f2=`find -type f -name 'der2_cRefset_LanguageDelta-nn*.txt'`
       echo $f2
       cat $f2 >> $f1
       rm $f2

       set f3=`find -type f -name 'der2_cRefset_LanguageDelta-nb_NO*.txt'`
       echo $f3
       cat $f3 >> $f1
       rm $f3

       set f4=`find -type f -name 'der2_cRefset_LanguageDelta-nb-gp_NO*.txt'`
       echo $f4
       cat $f4 >> $f1
       rm $f4

       cd $dir
       set f1=`find -type f -name 'sct2_TextDefinition_Delta-en*.txt'`
       echo $f1
       set f2=`find -type f -name 'sct2_TextDefinition_Delta-no*.txt'`
       echo $f2  
       cat $f2 >> $f1
       rm $f2

       cd $dir
       set f1=`find -type f -name 'sct2_Description_Delta-en*.txt'`
       echo $f1
       set f2=`find -type f -name 'sct2_Description_Delta-no*.txt'`
       echo $f2
       cat $f2 >> $f1
       rm $f2



        echo "    Load the delta ... `/bin/date`"
        cd $MAPPING_CODE/loader
        # don't do this anymore: -Dlast.publication.date=$SNOMEDCT_VERSION \
        # null value leads to correct computation
        nohup mvn -U clean install -PRF2-delta -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT_NO \
            -Dinput.dir=$dir | sed 's/^/      /'
        if ($status != 0) then
            echo "ERROR processing delta data"
            set ERROR_BODY="${ERROR_BODY}\nERROR processing delta data"
            cd $MAPPING_CODE/loader
            mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
            exit 1
        endif

        echo "    Remove SNOMEDCT tree positions ... `/bin/date`"
        cd $MAPPING_CODE/remover
        mvn -U clean install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT_NO -Dversion=latest | sed 's/^/      /'
        if ($status != 0) then
            echo "ERROR removing tree positions"
            set ERROR_BODY="${ERROR_BODY}\nERROR removing tree positions"
            cd $MAPPING_CODE/loader
            mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
            exit 1
        endif

        # optimize tree positions table
        echo "Clear indexes directory"
        /bin/rm -rf /home/ubuntu/data/mapping-indexes/mapping/org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa/*
        echo "Reindex other tree positions"
         cd $MAPPING_CODE/lucene
        nohup mvn -U clean install -PReindex -Drun.config=$MAPPING_CONFIG -Dindexed.objects=TreePositionJpa | sed 's/^/      /'


        echo "    Generate SNOMEDCT tree positions ... `/bin/date`"
        cd $MAPPING_CODE/loader
        nohup mvn -U clean install -PTreepos -Drun.config=$MAPPING_CONFIG -Dterminology=SNOMEDCT_NO -Dversion=latest -Droot.ids=138875005 | sed 's/^/      /'
        if ($status != 0) then
            echo "ERROR computing tree positions"
            set ERROR_BODY="${ERROR_BODY}\nERROR computing tree positions"
            cd $MAPPING_CODE/loader
            mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
            exit 1
       endif

else
    echo "Concepts file is empty"
endif

echo "    Update the Map Records' concept names ... `/bin/date`"
cd $MAPPING_CODE/loader
nohup mvn -U clean install -PUpdateMapRecords -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003
if ($status != 0) then
    echo "ERROR updating map records concept names"
    set ERROR_BODY="${ERROR_BODY}\nERROR updating map records concept names"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

echo "    Compute workflow ...`/bin/date`"
cd $MAPPING_CODE/loader
nohup mvn -U clean install -PComputeWorkflow -Drun.config=$MAPPING_CONFIG -Drefset.id=447562003 -Dsend.notification=true | sed 's/^/      /'
if ($status != 0) then
    echo "ERROR computing workflow"
    set ERROR_BODY="${ERROR_BODY}\nERROR computing workflow"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

echo "    Restarting application server ...`/bin/date`"
cd /home/ubuntu/project/OTF-Mapping-Service
set date=`/bin/date +%Y%m%d-%s`
nohup java -Xmx4500M -Xms500M -Drun.config=/home/ubuntu/data/mapping-rest/config.properties \
  -jar webapp-runner.jar --port 8080 rest/target/mapping-rest.war > /home/ubuntu/logs/log.$date.txt 2>&1 &


echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
