REM
REM Configure MAVEN_HOME
REM
set MVN_HOME=C:\apache-maven-3.0.5
set CODE_HOME="C:/Users/Brian Carlsen/workspace/mapping-parent"
@echo off

echo ------------------------------------------------
echo Starting ...%date%
echo ------------------------------------------------

echo     Run updatedb with hibernate.hbm2ddl.auto = create ...%date%
cd %CODE_HOME%/admin/updatedb
%MVN_HOME%/bin/mvn -Drun.config=dev -Dhibernate.hbm2ddl.auto=create install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)


echo     Load SNOMEDCT ...%date%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PSNOMEDCT -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)


echo     Load ICPC ...%date%
cd %CODE_HOME%/admin/loader (file is ~/data/icpc*xml)
%MVN_HOME%/bin/mvn -PICPC -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)


echo     Load ICD10 ...%date%
cd %CODE_HOME%/admin/loader (file is ~/data/icd10*xml)
%MVN_HOME%/bin/mvn -PICD10-Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Load ICD9CM ...%date%
cd %CODE_HOME%/admin/loader (file is ~/data/icd9cm*xml)
%MVN_HOME%/bin/mvn -PICD9CM -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Import project data ...%date%
cd %CODE_HOME%/admin/import (dir is in ~/data/ihtsdo-project-data)
%MVN_HOME%/bin/mvn -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Create ICD10 and ICD9CM map records ...%date%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PCreateMapRecords -Drun.config=dev -Drefset.id=447562003,447563008 install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Load ICPC maps from file ...%date%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PMapRecords -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo     Load map notes from file ...%date%
cd %CODE_HOME%/admin/loader
%MVN_HOME%/bin/mvn -PMapNotes -Drun.config=dev install > mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)

echo ------------------------------------------------
IF %error% NEQ 0 (
echo There were one or more errors.  Please reference the mvn.log file for details. 
set retval=-1
) else (
echo Completed without errors. >> mysql.log >
set retval=0
)
echo Starting ...%date%
echo ------------------------------------------------

pause


