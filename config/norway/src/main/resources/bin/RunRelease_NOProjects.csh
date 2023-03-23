#!/bin/csh -f
#
# Sample cron entry:
# Minute     Hour     Day of Month     Month     Day of Week
# 0      15      14       2       *       csh /home/ubuntu/data/mapping-admin/scripts/RunReleases.csh ICDO 20220228 MAIN/RMUP/RMUP-6 > /home/ubuntu/data/mapping-admin/scripts/RunReleases_ICDO_20220228.log
# 30      15      14       2       *       csh /home/ubuntu/data/mapping-admin/scripts/RunReleases.csh ICD10 20220228 MAIN/RMUP/RMUP-7 > /home/ubuntu/data/mapping-admin/scripts/RunReleases_ICD10_20220228.log
#
# Configure
#
echo "setting variables"
set MAPPING_CODE=/home/ubuntu/data/mapping-admin
set MAPPING_CONFIG=/home/ubuntu/data/mapping-rest/config.properties
set MAPPING_DATA=/home/ubuntu/data/mapping-data

set mapping_url=https://norway-mapping.terminology.tools/

set ims_url=https://ims.ihtsdotools.org

set ims_username=mappingadmin
set ims_password='XJ62B^4W19Ar'


if ($#argv < 2) then
echo "Missing required argument(s). Format should be: RunReleases.csh <project> <releaseDate> [taskBranch]"
exit 1
else
set project = $argv[1]
set releaseDate = $argv[2]
endif

if($#argv == 3) then
set taskBranch = $argv[3]
endif

echo "project=$project"
echo "releaseDate=$releaseDate"
echo "taskBranch (optional)=$taskBranch"

if("$project" == "ICD10NO") then
set projectNumber=3
set moduleId=51000202101
elif [[ $project == "ICPC2NO" ]]
then
set projectNumber=2
set moduleId=51000202101
else
  echo "project $project not supported - current valid options are: ICD10NO, and ICPC2NO."
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

set beginReleaseUrl="$mapping_url"'mapping/project/id/'"$projectNumber"'/release/'"$releaseDate"'/begin'
set processReleaseUrl="$mapping_url"'mapping/project/id/'"$projectNumber"'/release/'"$releaseDate"'/module/id/'"$moduleId"'/process'

echo "    Run begin release for $project ...`/bin/date`"
curl -b /tmp/cookies.txt --silent -X POST -H "Authorization: $ims_username" $beginReleaseUrl > /dev/null &

# give it a bit to start the process
sleep 2

# poll the begin.log until it finishes successfully or fails
while (1)
  set beginSuccessful=`grep "Done begin release" /home/ubuntu/data/mapping-data/doc/$projectNumber/logs/begin.log`
  set beginFailed=`grep "ERROR" /home/ubuntu/data/mapping-data/doc/$projectNumber/logs/begin.log`
  if("$beginSuccessful" != "") then
    echo "Begin release finished - `/bin/date`"
    break
  else if("$beginFailed" != "") then
    echo "Begin release failed - `/bin/date`"
    exit 1
  else
    echo "Begin release still running - `/bin/date`"
  endif
sleep 60
end


echo "    Run release for $project ...`/bin/date`"
curl -b /tmp/cookies.txt --silent -X POST -H "Authorization: $ims_username" $processReleaseUrl  > /dev/null &

# give it a bit to start the process
sleep 5

# poll the process.log until it finishes successfully or fails
while (1)
  set processSuccessful=`grep "Done processing the release" /home/ubuntu/data/mapping-data/doc/$projectNumber/logs/process.log`
  set processFailed=`grep "ERROR" /home/ubuntu/data/mapping-data/doc/$projectNumber/logs/process.log`
  if("$processSuccessful" != "") then
    echo "Process release finished - `/bin/date`"
    break
  else if("$processFailed" != "") then
    echo "Process release failed - `/bin/date`"
    exit 1
  else
    echo "Process release still running - `/bin/date`"
  endif
sleep 60
end

#cleanup
rm authenticate
rm imsRoles.txt
rm wget-log

# if task branch specified, upload the release file just created to snowstorm
if($?taskBranch) then
echo "Uploading $project $releaseDate DELTA to $taskBranch"
`$MAPPING_CODE/scripts/DeltaSnowstormDataTransfer_NOProjects.sh $project $releaseDate $taskBranch`
exit 1
endif

