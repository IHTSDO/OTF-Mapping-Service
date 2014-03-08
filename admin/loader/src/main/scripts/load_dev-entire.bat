@echo off
REM
REM This script is used to load OTF mapping project data for the development
REM environment.  It loads the entire snapshot of SNOMEDCT and assumes you have
REM acquired your data from http://mapping.snomedtools.org/data/dev.zip
REM and from http://mapping.snomedtools.org/data/snomedct-20140131-snapshot.zip
REM

REM
REM Configure MAVEN_HOME
REM
set MVN_HOME=C:/apache-maven-3.0.5
set CODE_HOME="C:/Users/Brian Carlsen/workspace/mapping-parent"

echo ------------------------------------------------
echo Starting ...%date% %time%
echo ------------------------------------------------
echo MVN_HOME  = %MVN_HOME%
echo CODE_HOME = %CODE_HOME%

echo     Run updatedb with hibernate.hbm2ddl.auto = create ...%date%%time%
cd %CODE_HOME%/admin/updatedb
call %MVN_HOME%/bin/mvn -Drun.config=dev-entire -Dhibernate.hbm2ddl.auto=create install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load SNOMEDCT ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PSNOMEDCT -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICPC -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log
echo     Load ICD10 ...%date%%time%

cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICD10-Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICD9CM ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PICD9CM -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Import project data ...%date%%time%
cd %CODE_HOME%/admin/import
call %MVN_HOME%/bin/mvn -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Create ICD10 and ICD9CM map records ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PCreateMapRecords -Drun.config=dev-entire -Drefset.id=447562003,447563008 install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC maps from file ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PMapRecords -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load map notes from file ...%date%%time%
cd %CODE_HOME%/admin/loader
call %MVN_HOME%/bin/mvn -PMapNotes -Drun.config=dev-entire install 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

trailer:
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


