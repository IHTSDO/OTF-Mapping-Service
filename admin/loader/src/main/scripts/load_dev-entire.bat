@echo off
REM
REM This script is used to load OTF mapping project data for the development
REM environment.  It loads the mini version of SNOMEDCT and assumes you have
REM acquired your data from http://mapping.snomedtools.org/data/dev.zip
REM

REM
REM Set environment variables at system level
REM
REM set MVN_HOME=C:/apache-maven-3.0.5
REM set OTF_MAPPING_HOME="C:/Users/Brian Carlsen/workspace/mapping-parent"
REM


echo ------------------------------------------------
echo Starting ...%date% %time%
echo ------------------------------------------------
if DEFINED MVN_HOME (echo MVN_HOME  = %MVN_HOME%) else (echo MVN_HOME must be defined
goto trailer)
if DEFINED OTF_MAPPING_HOME (echo OTF_MAPPING_HOME  = %OTF_MAPPING_HOME%) else (echo OTF_MAPPING_HOME must be defined
goto trailer)
set MAVEN_OPTS = -XX:MaxPermSize=512m -Xmx3300M
set error=0

echo     Run updatedb with hibernate.hbm2ddl.auto = create ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/updatedb
call %MVN_HOME%/bin/mvn -Drun.config=dev-entire -Dhibernate.hbm2ddl.auto=create install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Clear indexes ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/lucene
call %MVN_HOME%/bin/mvn -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load SNOMEDCT ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PSNOMEDCT -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICPC -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log
echo     Load ICD10 ...%date% %time%

cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICD10-Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICD9CM ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICD9CM -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Import project data ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/import
call %MVN_HOME%/bin/mvn -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Create ICD10 and ICD9CM map records ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PCreateMapRecords -Drun.config=dev-entire -Drefset.id=447562003,447563008 install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC maps from file ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PMapRecords -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load map notes from file ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PMapNotes -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Compute workflow ...%date% %time%
cd %OTF_MAPPING_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PComputeWorkflow -Drun.config=dev-entire -Drefset.id=447563008,447562003 install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

:trailer
echo ------------------------------------------------
IF %error% NEQ 0 (
echo There were one or more errors.  Please reference the mvn.log file for details. 
set retval=-1
) else (
echo Completed without errors.
set retval=0
)
echo Starting ...%date% %time%
echo ------------------------------------------------

pause


