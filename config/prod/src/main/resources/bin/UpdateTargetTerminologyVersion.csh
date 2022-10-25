#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      15      14       2       *       csh /opt/mapping-admin/scripts/UpdateTargetTerminologyVersion.csh GMDN > /opt/mapping-admin/scripts/UpdateTargetTerminologyVersion.log
#
# Configure
#
echo "setting variables"
set MAPPING_CODE=/opt/mapping-admin
set MAPPING_CONFIG=/opt/mapping-rest/config.properties
set MAPPING_DATA=/opt/mapping-data

set mapping_url=https://mapping.ihtsdotools.org/

set ims_url=https://ims.ihtsdotools.org

set ims_username=mappingadmin
set ims_password='XJ62B^4W19Ar'


if ($#argv < 1) then
echo "Missing required argument(s). Format should be: UpdateTargetTerminologyVersion.csh <project> "
exit 1
else
set project = $argv[1]
endif


echo "project=$project"

if ("$project" == "GMDN") then
set terminologyName="gmdn"
else
  echo "project $project not supported - failing."
  exit 1
endif

# log into IMS to get authentication token
# Create temporary cookie file
wget --keep-session-cookies --save-cookies /tmp/cookies.txt --header="Content-Type:application/json" --post-data '{"login":"'"$ims_username"'","password":"'"$ims_password"'","rememberMe":"false"}' ${ims_url}/api/authenticate

# Get and save ims roles
echo "   Getting users ims roles"
curl -b /tmp/cookies.txt --silent "$mapping_url"'ims-api/account' --output imsRoles.txt > /dev/null

# Add line breaks so it works with mapping tool processing
sed -i 's/\([{,\[]\)/\1\n/g' imsRoles.txt

set imsRoles=`cat imsRoles.txt`
#echo "ims roles=$imsRoles"

#log user into the mapping tool
echo "   Logging user into mapping tool"
curl -b /tmp/cookies.txt --silent -d "$imsRoles" -X POST -H "Content-Type: text/plain" "$mapping_url"'security/authenticate/'"$ims_username"''  > /dev/null

set downloadTerminology="$mapping_url"'content/terminology/download/'"$terminologyName"

echo "    Download $terminologyName files  ...`/bin/date`"
set startFileCount = `ls /opt/mapping-data/$project | wc -l`
echo "startFileCount: $startFileCount"
curl -b /tmp/cookies.txt --silent -X POST -H "Authorization: $ims_username" $downloadTerminology> /dev/null &


# give it a bit to start the process
sleep 5

set endFileCount = `ls /opt/mapping-data/$project | wc -l`
echo "endFileCount: $endFileCount"

# poll the begin.log until it finishes successfully or fails
  if("$endFileCount" > "$startFileCount") then
    echo "Download $project completed - `/bin/date`"
  else 
    sleep 30
    echo "Gave $project Download extra time"
    set endFileCount = `ls /opt/mapping-data/$project | wc -l`
    echo "endFileCount: $endFileCount"
    if ("$endFileCount" > "$startFileCount") then
      echo "Download $project completed - `/bin/date`"
    else
      echo "No new versions of $project available.  Aborting."
      exit 1
    endif
  endif

set version = `ls /opt/mapping-data/$project | sort -V |  tail -1`
echo "    Load $terminologyName $version...`/bin/date`"
set loadTerminology="$mapping_url"'content/terminology/load/'"$terminologyName"'/'"$version"' -H Content-Type: text/plain -d /opt/mapping-data/'"$project"'/'"$version"
echo "loadTerminology: $loadTerminology"
curl -b /tmp/cookies.txt --silent -X PUT -H "Authorization: $ims_username" $loadTerminology> /dev/null &

# give it a bit to start the process
sleep 5


echo "    Set $project project to $version...`/bin/date`"
set updateProjectGmdn="$mapping_url"'mapping/project/id/13/version/'"$version"
echo "updateProjectGmdn: $updateProjectGmdn"
curl -b /tmp/cookies.txt --silent -X POST -H "Authorization: $ims_username" $updateProjectGmdn> /dev/null &

sleep 25

# give it a bit to start the process
#cleanup
rm authenticate
rm imsRoles.txt

echo "    Send email to stakeholders...`/bin/date`"
 cd $MAPPING_CODE/loader
    mvn -U clean install -PSendEmail -Drun.config=$MAPPING_CONFIG -Drecipients="dmo@snomed.org;aat@snomed.org;mban@snomed.org;rwood@westcoastinformatics.com;dshapiro@westcoastinformatics.com" -Dsubject="${project} updated to version ${version}" -Dbody="The ${project} project was updated to version ${version}." | sed 's/^/      /'

