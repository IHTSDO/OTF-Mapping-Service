REM
REM Configure MAVEN_HOME
REM
set MVN_HOME=C:\apache-maven-3.0.5
set CODE_HOME="C:/Users/Brian Carlsen/workspace/mapping-parent"
@echo off

echo ------------------------------------------------
echo Starting ...%date% %time%
echo ------------------------------------------------

echo     Run updatedb with hibernate.hbm2ddl.auto = create ...%date% %time%
cd %CODE_HOME%/admin/updatedb
%MVN_HOME%/bin/mvn -Drun.config=dev -Dhibernate.hbm2ddl.auto=create install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load SNOMEDCT ...%date% %time%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PSNOMEDCT -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Load ICPC ...%date% %time%
cd %CODE_HOME%/admin/loader (file is ~/data/icpc*xml)
%MVN_HOME%/bin/mvn -PICPC -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Load ICD10 ...%date% %time%
cd %CODE_HOME%/admin/loader (file is ~/data/icd10*xml)
%MVN_HOME%/bin/mvn -PICD10-Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Load ICD9CM ...%date% %time%
cd %CODE_HOME%/admin/loader (file is ~/data/icd9cm*xml)
%MVN_HOME%/bin/mvn -PICD9CM -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Import project data ...%date% %time%
cd %CODE_HOME%/admin/import (dir is in ~/data/ihtsdo-project-data)
%MVN_HOME%/bin/mvn -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Create ICD10 and ICD9CM map records ...%date% %time%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PCreateMapRecords -Drun.config=dev -Drefset.id=447562003,447563008 install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Load ICPC maps from file ...%date% %time%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PMapRecords -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo     Load map notes from file ...%date% %time%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PMapNotes -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log


echo ------------------------------------------------
IF %error% NEQ 0 (
echo There were one or more errors.  Please reference the mvn.log file for details. 
set retval=-1
) else (
echo Completed without errors. >> mysql.log >
set retval=0
)
echo Starting ...%date% %time%
echo ------------------------------------------------

pause


