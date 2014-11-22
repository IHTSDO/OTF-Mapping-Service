@echo off
REM
REM This script is used to load a SNOMEDCT delta terminology file
REM It loads the delta file, re-indexes concept,
REM and tree position objects, and recalculates workflow
REM Assumes delta data file is specified in config file.

REM
REM Set environment variables at system level
REM
set MVN_HOME=C:/apache-maven-3.0.5
set OTF_MAPPING_HOME="C:/OTF-Mapping-Service"
set OTF_MAPPING_CONFIG="C:/data/config.properties.dev"
REM

echo ------------------------------------------------
echo Starting ...%date% %time%
echo ------------------------------------------------
if DEFINED MVN_HOME (echo MVN_HOME  = %MVN_HOME%) else (echo MVN_HOME must be defined
goto trailer)
if DEFINED OTF_MAPPING_HOME (echo OTF_MAPPING_HOME = %OTF_MAPPING_HOME%) else (echo OTF_MAPPING_HOME must be defined
goto trailer)
if DEFINED OTF_MAPPING_CONFIG (echo OTF_MAPPING_CONFIG = %OTF_MAPPING_CONFIG%) else (echo OTF_MAPPING_CONFIG must be defined
goto trailer)
set error=0

echo     Load delta data ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PSNOMEDCTDelta -Drun.config=%OTF_MAPPING_CONFIG% install >> deltaLoader.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Re-index Concepts ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/lucene
call %MVN_HOME%/bin/mvn -Drun.config=%OTF_MAPPING_CONFIG% -DindexedObjects=ConceptJpa install >> deltaLoader.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Recompute TreePositions ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PSNOMEDCT-treepos -Drun.config=%OTF_MAPPING_CONFIG% install >> deltaLoader.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Clear workflow for ICD10 and ICD9CM ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/remover
call %MVN_HOME%/bin/mvn -PClearWorkflow -Drun.config=%OTF_MAPPING_CONFIG% -Drefset.id=447563008,447562003 install >> deltaLoader.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Compute workflow for ICD10 and ICD9CM ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PComputeWorkflow -Drun.config=%OTF_MAPPING_CONFIG% -Drefset.id=447563008,447562003 install >> deltaLoader.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

:trailer
echo ------------------------------------------------
IF %error% NEQ 0 (
echo There were one or more errors.  Please reference the deltaLoader.log file for details. 
set retval=-1
) else (
echo Completed without errors.
set retval=0
)
echo Starting ...%date% %time%
echo ------------------------------------------------

pause


