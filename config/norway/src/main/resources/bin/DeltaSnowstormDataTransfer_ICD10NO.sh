#!/bin/bash

MAPPING_CODE=/opt/mapping-admin
MAPPING_CONFIG=/opt/mapping-rest/config.properties

USERNAME=wci_map
PASSWORD='nrXHX*4r.2ed%x;4'

snowstorm="https://dailybuild.terminologi.ehelse.no/snowstorm/snomed-ct/imports/"

if [[ "$1" != "" ]]; then
    project="$1"
else
    echo "Missing argument(s). Format should be: DeltaSnowstormDataTransfer.sh <project> <version>"
    exit
fi

if [[ "$2" != "" ]]; then
    version="$2"
else
    "Missing argument(s). Format should be: DeltaSnowstormDataTransfer.sh <project> <version>"
    exit
fi

if [[ "$3" != "" ]]; then
    taskBranch="$3"
else
    "Missing argument(s). Format should be: DeltaSnowstormDataTransfer.sh <project> <version> <taskBranch>"
    exit
fi

if [[ $project == "ICD10NO" ]]
then 
	refsetID="447562003"
fi


releaseDir="/opt/mapping-data/doc/release/SNOMEDCT_NO_to_${project}_${refsetID}/${version}/"
topDir="$releaseDir""SnomedCT_InternationalRF2_""$project""DELTA_""$version""T120000Z"

mkdir -p "$topDir/Delta/Refset/Content" "$topDir/Delta/Refset/Language" "$topDir/Delta/Refset/Map" "$topDir/Delta/Refset/Metadata" "$topDir/Delta/Terminology"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	targetComponentId" > "$topDir/Delta/Refset/Content/der2_cRefset_AssociationDelta_INT_""$version"".txt"
echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	valueId" > "$topDir/Delta/Refset/Content/der2_cRefset_AttributeValueDelta_INT_""$version"".txt"
echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId" > "$topDir/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_""$version"".txt"


echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId" > "$topDir/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_""$version"".txt"


echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	mapGroup	mapPriority	mapRule	mapAdvice	mapTarget	correlationId	mapCategoryId" > "$topDir/Delta/Refset/Map/der2_iisssccRefset_ExtendedMapDelta_INT_""$version"".txt"


echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	attributeDescription	attributeType	attributeOrder" > "$topDir/Delta/Refset/Metadata/der2_cciRefset_RefsetDescriptorDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	descriptionFormat	descriptionLength
" > "$topDir/Delta/Refset/Metadata/der2_ciRefset_DescriptionTypeDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	domainId	grouped	attributeCardinality	attributeInGroupCardinality	ruleStrengthId	contentTypeId" > "$topDir/Delta/Refset/Metadata/der2_cissccRefset_MRCMAttributeDomainDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	mrcmRuleRefsetId
" > "$topDir/Delta/Refset/Metadata/der2_cRefset_MRCMModuleScopeDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	rangeConstraint	attributeRule	ruleStrengthId	contentTypeId
" > "$topDir/Delta/Refset/Metadata/der2_ssccRefset_MRCMAttributeRangeDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime
" > "$topDir/Delta/Refset/Metadata/der2_ssRefset_ModuleDependencyDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	domainConstraint	parentDomain	proximalPrimitiveConstraint	proximalPrimitiveRefinement	domainTemplateForPrecoordination	domainTemplateForPostcoordination	guideURL
" > "$topDir/Delta/Refset/Metadata/der2_sssssssRefset_MRCMDomainDelta_INT_""$version"".txt"



echo "id	effectiveTime	active	moduleId	definitionStatusId" > "$topDir/Delta/Terminology/sct2_Concept_Delta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
" > "$topDir/Delta/Terminology/sct2_Description_Delta-en_INT_""$version"".txt"

echo "identifierSchemeId	alternateIdentifier	effectiveTime	active	moduleId	referencedComponentId
" > "$topDir/Delta/Terminology/sct2_Identifier_Delta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId
" > "$topDir/Delta/Terminology/sct2_Relationship_Delta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
" > "$topDir/Delta/Terminology/sct2_sRefset_OWLExpressionDelta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId
" > "$topDir/Delta/Terminology/sct2_StatedRelationship_Delta_INT_""$version"".txt"

echo "id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
" > "$topDir/Delta/Terminology/sct2_TextDefinition_Delta-en_INT_""$version"".txt"

# build edits file
echo "id		active	moduleId	refsetId	referencedComponentId	mapTarget" > "$topDir/Delta/Refset/Map/der2_sRefset_SimpleMapDelta_INT_""$version"".txt"

deltaTransforms="$releaseDir""der2_iisssccRefset_ExtendedMapDelta_INT_${version}.txt"
previousINTRelease="/opt/mapping-data/doc/1/preloadMaps/ExtendedMapSnapshot.txt"
changedMaps="$releaseDir""changedMapsOnly_${version}.txt"

awk 'FNR==NR{a[$1]=$1; next}; !($1 in a) {print $6;}' "$previousINTRelease" "$deltaTransforms" > "$releaseDir""changedConceptIds_${version}.txt"
grep -F -f "$releaseDir""changedConceptIds_${version}.txt" "$deltaTransforms" > "$changedMaps"

while read line
do
    if [[ "$line" == *"$version"* ]]; then
	echo "${line/$version/""}" >> "${topDir}/Delta/Refset/Map/der2_iisssccRefset_ExtendedMapDelta_INT_${version}.txt"
    fi
done < "$changedMaps"

zip -r -q "${releaseDir}SnomedCT_InternationalRF2_${project}DELTA_${version}T120000Z".zip "$topDir"

#wget --keep-session-cookies --save-cookies /tmp/cookies.txt --header="Content-Type:application/json" --post-data '{"login":"'"$ims_username"'","password":"'"$ims_password"'","rememberMe":"false"}' "$ims_url"/api/authenticate

#rm wget-log

#if [[ ! -f ./authenticate ]]
#then
#        echo "Failed to get authentication"
#        FAIL_BODY="Delta Snowstorm ${project}, version ${version} Failed to upload because the authentication failed"
#        cd "${MAPPING_CODE}/loader"
#        mvn -U clean install -PSendEmail -Drun.config="${MAPPING_CONFIG}" -Dsubject="Upload Failed due to failed authentication" -Dbody="${FAIL_BODY}" | sed 's/^/      /'
#        exit 1
#fi

curl -D "header.txt" --user "${USERNAME}:${PASSWORD}" --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
  "branchPath": "'"${taskBranch}"'",
  "type": "DELTA"
}' "${snowstorm}"

location=""
while read line
do
    if echo "$line" | grep -q "Location"; then
	location="$line"
	break
    fi
done < "header.txt"

if [ -z  "$location" ]
then
	echo "Failed to get import ID"
	FAIL_BODY="Delta Snowstorm ${project}, version ${version} Failed to upload because the importID was unavailable"
        rm header.txt
        cd "${MAPPING_CODE}/loader"
        mvn -U clean install -PSendEmail -Drun.config="${MAPPING_CONFIG}" -Dsubject="Upload Failed due to unavailable importID" -Dbody="${FAIL_BODY}" | sed 's/^/      /'
        exit 1
fi

rm header.txt

location=${location##*/}
location=$(echo $location | sed -e 's/\r//g')

importID="$location"
urlSuffix="/archive"
snowstormLink="$snowstorm$importID$urlSuffix"

curl -i -X POST --user "${USERNAME}:${PASSWORD}" --header 'Content-Type: multipart/form-data' --header 'Accept: application/json' --form "file=@${releaseDir}SnomedCT_InternationalRF2_${project}DELTA_${version}T120000Z.zip" "${snowstormLink}" >> upload.txt

# check if upload failed

rm upload.txt

completedStatus="\"status\" : \"COMPLETED\","

curl -i -X GET --header 'Accept: application/json' "${snowstorm}${importID}" >> uploadCheck.txt


for i in {0..10..1} 
do
	curl -i -X GET --header 'Accept: application/json' "${snowstorm}${importID}" >> uploadCheck.txt

	if grep -q "$completedStatus" uploadCheck.txt
	then
		echo "Upload Succeeded"
		SUCCESS_BODY="Delta Snowstorm ${project}, version ${version} Uploaded Successfully to branch ${taskBranch}"
		rm uploadCheck.txt 2>/dev/null
		cd "${MAPPING_CODE}/loader"
		mvn -U clean install -PSendEmail -Drun.config="${MAPPING_CONFIG}" -Dsubject="Upload Successful" -Dbody="${SUCCESS_BODY}" | sed 's/^/      /'
		rm /opt/mapping-admin/scripts/authenticate
		exit 1
	fi
	sleep 6
done

if ! grep -q "$completedStatus" uploadCheck.txt;
then
    echo "Upload Failed"
    ERROR_BODY="Delta Snowstorm ${project}, version ${version} failed to upload"
    rm uploadCheck.txt 2>/dev/null
    cd "${MAPPING_CODE}/loader"
    mvn -U clean install -PSendEmail -Drun.config="${MAPPING_CONFIG}" -Dsubject="Upload Failed" -Dbody="${ERROR_BODY}" | sed 's/^/      /'
    exit 1
fi

