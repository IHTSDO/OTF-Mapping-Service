#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      19      *       *       0       csh /opt/mapping-admin/scripts/replaceSnomed.csh > /opt/mapping-admin/scripts/replaceSnomed.log
#
# Configure
#
echo "setting variables in remove & load"
set MAPPING_CODE=/opt/mapping-admin
set MAPPING_CONFIG=/opt/mapping-rest/config.properties
set MAPPING_DATA=/opt/mapping-data

# ex. SNOMEDCT
set TERMINOLOGY=$1

# ex. latest
set VERSION=$2

set buildStatus = "Progress Status"
set buildOk = "completed,"
set buildOk2 = "completed"
set numrels = 50

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE   = $MAPPING_CODE"
echo "MAPPING_DATA   = $MAPPING_DATA"
echo "MAPPING_CONFIG = $MAPPING_CONFIG"
echo "TERMINOLOGY    = $TERMINOLOGY"
echo "VERSION        = $VERSION"

# set error message variables
set ERROR_SUBJECT="OTF-Mapping-Tool Remove/Replace Snomed error"
set ERROR_BODY="The Remove/Replace Snomed job did not complete due to the following issue(s):\n"


set relUrl=https://prod-release.ihtsdotools.org/api/centers/international/products/int_daily_build/builds/
set tempdir=/tmp/replaceTerminology

set dir=/opt/mapping-data/replaceTerminology

#set todaysdate="$(date '+%Y%m%d')"
#make sure temp dirs exist
mkdir -p $tempdir
mkdir -p $dir

echo "    Delete last load ...`/bin/date`"
cd $dir
rm -fr $dir/*

# clean up temp dir

#rm -f $tempdir/buildrep.*.json
#rm -f $tempdir/xx.*.json
rm -f $tempdir/*

####################################################################
# Get the files before stopping the server
####################################################################

# log into IMS to get authentication token
set ims_url=https://ims.ihtsdotools.org
set ims_username=EDIT_THIS
set ims_password=EDIT_THIS
set ims_cookie_file=/tmp/cookies.txt
# Create temporary cookie file
wget --keep-session-cookies --save-cookies "$ims_cookie_file" --header="Content-Type:application/json" --post-data '{"login":"'"$ims_username"'","password":"'"$ims_password"'","rememberMe":"false"}' ${ims_url}/api/authenticate

echo "    Obtain latest release ...`/bin/date`"
wget --load-cookies $ims_cookie_file "$relUrl" -O $tempdir/xx.$$.json
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
    wget --load-cookies $ims_cookie_file "$buildreport" -O $tempdir/buildrep.$$.json
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

    echo "downloading latest release directory"
    wget --load-cookies $ims_cookie_file $latestRelease -O $tempdir/$releaseName.json
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

echo "Zip file is" $zipFile
set myLocation = `pwd`
echo "pwd is" "$myLocation"

wget --load-cookies $ims_cookie_file $zipFile
if ($status != 0) then
    echo "ERROR downloading .zip"
    set ERROR_BODY="${ERROR_BODY}\nERROR downloading .zip"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

echo "    Unpack the Snapshot ...`/bin/date`"
unzip $zipFile:t "*/Snapshot/*"
if ($status != 0) then
    echo "ERROR unpacking .zip"
    set ERROR_BODY="${ERROR_BODY}\nERROR unpacking .zip"
    cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
endif

####################################################################
# If file exists, stop the application, remove then add the terminology
####################################################################
if (`grep -v effectiveTime /opt/mapping-data/replaceTerminology/SnomedCT*/Snapshot/Terminology/*Concept*txt | wc -l` > 0) then

    echo "Concepts file was found"

    # sudo /opt/maint/getMaintHtml.sh start
    set MAVEN_OPTS="-Xmx10000M -XX:+UseG1GC"
    echo MAVEN_OPTS=$MAVEN_OPTS

    ####################################################################
    # TODO: Add call Rundeck to start maintenance if run outside of window
    ####################################################################

    echo "Taking down the server"
    timeout --foreground 30 sudo supervisorctl stop mapping-rest
    if ($status != 0) then
            echo "ERROR stopping server"
            set ERROR_BODY="${ERROR_BODY}\nError stopping server."
            cd $MAPPING_CODE/loader
            mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
            exit 1
    endif

    echo "    Remove terminology ... `/bin/date`"
    cd $MAPPING_CODE/remover
    mvn -e -U clean install -PTerminology -Drun.config=$MAPPING_CONFIG -Dterminology=$TERMINOLOGY -Dversion=$VERSION | sed 's/^/      /'
    if ($status != 0) then
        echo "ERROR removing terminology"
        set ERROR_BODY="${ERROR_BODY}\nERROR removing terminology"
        cd $MAPPING_CODE/loader
        mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
        exit 1
    endif


    echo "    Load terminology ... `/bin/date`"
    cd $MAPPING_CODE/loader
    set sourceDir=$dir/SnomedCT*/Snapshot
    mvn -e -U clean install -PRF2-snapshot -Drun.config=$MAPPING_CONFIG -Dterminology=$TERMINOLOGY -Dversion=$VERSION -Dinput.dir=$sourceDir | sed 's/^/      /'
    if ($status != 0) then
        echo "ERROR loading terminology"
        set ERROR_BODY="${ERROR_BODY}\nERROR loading terminology"
        cd $MAPPING_CODE/loader
        mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
        exit 1
    endif


    echo "    Restarting application server ...`/bin/date`"
    timeout --foreground 30 sudo supervisorctl start mapping-rest
    if ($status != 0) then
        echo "ERROR starting server"
        set ERROR_BODY="${ERROR_BODY}\nERROR starting server."
        cd $MAPPING_CODE/loader
        mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Dsubject="${ERROR_SUBJECT}" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
        exit 1
    endif
    sudo /opt/maint/getMaintHtml.sh stop

    ####################################################################
    # TODO: Add call Rundeck to complete maintenance if run outside of window
    ####################################################################

else
    echo "Concepts file is empty"
endif

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"