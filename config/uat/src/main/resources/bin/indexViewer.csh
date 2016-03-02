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
set version = 2010

echo "------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------"
echo "MAPPING_CODE = $MAPPING_CODE"
echo "MAPPING_DATA = $MAPPING_CODE"
echo "MAPPING_CONFIG = $MAPPING_CODE"
echo "version      = $version"
echo ""

echo "  Update artifact ...`/bin/date`"
cd $MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data
git pull
if ($status != 0) then
    echo "ERROR updating artifact"
    cat mvn.log
    exit 1
endif

echo "  Run ConvertAscToXml ...`/bin/date`"
cd $MAPPING_CODE/admin/lucene
mvn install -PConvertAscToXml -Drun.config=$MAPPING_CONFIG -Dinput.dir=$MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data/icd10/src/main/resources/indexViewerData >&! mvn.log
if ($status != 0) then
    echo "ERROR running ConvertAscToXml"
    cat mvn.log
    exit 1
endif

echo "  Run ConvertXmlToHtmlAndLucene ...`/bin/date`"
cd $MAPPING_CODE/admin/lucene
mvn install -PConvertXmlToHtmlAndLucene -Drun.config=$MAPPING_CONFIG -Dinput.dir=$MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data/icd10/src/main/resources/indexViewerData >&! mvn.log
if ($status != 0) then
    echo "ERROR running ConvertXmlToHtmlAndLucene"
    cat mvn.log
    exit 1
endif

# set lucene dir - this is where indexes get written to
echo "  Move lucene to directory ...`/bin/date`"
set luceneDir = `grep hibernate.search.default.indexBase $MAPPING_CONFIG | perl -pe 's/.*=//; s/\r//;'`
/bin/rm -rf $MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data/icd10/src/main/resources/indexViewerData/ICD10/$version/lucene/*
/bin/mv -f $luceneDir/ICD10/$version/* $MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data/icd10/src/main/resources/indexViewerData/ICD10/$version/lucene

echo "  Package up artifact ...`/bin/date`"
cd $MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data
mvn clean install >&! mvn.log
if ($status != 0) then
    echo "ERROR failed to package index dir"
    cat mvn.log
    exit 1
endif

echo "  Add and commit when ready ...`/bin/date`"
cd $MAPPING_DATA/ihtsdo-mapping-tool-data/index-viewer-data
git status
echo "COMMIT AND PUSH CHANGES WHEN READY"

echo "------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------"
