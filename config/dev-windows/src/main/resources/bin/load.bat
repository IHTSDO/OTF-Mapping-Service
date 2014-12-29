@echo off
REM
REM This script is used to load OTF mapping project data for the development
REM environment.  It loads the mini version of SNOMEDCT and assumes you have
REM acquired your data from http://mapping.snomedtools.org/data/dev.zip
REM
REM Prerequisite:
REM   - "mvn" executable is in your path

REM
REM Set variables 
REM
set MAPPING_CODE="C:\mapping\code"
set MAPPING_CONFIG="C:\mapping\config\config.properties"
set MAPPING_DATA="C:\mapping\data"

echo ------------------------------------------------
echo Starting ...%date% %time%
echo ------------------------------------------------
if DEFINED MAPPING_CODE (echo MAPPING_CODE = %MAPPING_CODE%) else (echo MAPPING_CODE must be defined
goto trailer)
if DEFINED MAPPING_CONFIG (echo MAPPING_CONFIG = %MAPPING_CONFIG%) else (echo MAPPING_CONFIG must be defined
goto trailer)
if DEFINED MAPPING_DATA (echo MAPPING_DATA = %MAPPING_DATA%) else (echo MAPPING_DATA must be defined
goto trailer)
set error=0
pause

echo     Run updatedb with hibernate.hbm2ddl.auto = create ...%date% %time%
cd %MAPPING_CODE%/admin/updatedb
call mvn install -Drun.config=%MAPPING_CONFIG% -PUpdatedb -Dhibernate.hbm2ddl.auto=create 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Clear indexes ...%date% %time%
cd %MAPPING_CODE%/admin/lucene
call mvn install -Drun.config=%MAPPING_CONFIG% 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load SNOMEDCT ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PRF2-snapshot -Drun.config=%MAPPING_CONFIG% -Dterminology=SNOMEDCT -Dinput.dir=%MAPPING_DATA%/snomedct-20140731-mini 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PClaML -Drun.config=%MAPPING_CONFIG% -Dterminology=ICPC -Dversion=2 -Dinput.file=%MAPPING_DATA%/icpc-2.xml 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICD10 ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PClaML -Drun.config=%MAPPING_CONFIG% -Dterminology=ICD10 -Dversion=2010 -Dinput.file=%MAPPING_DATA%/icd10-2010.xml 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICD9CM ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PClaML -Drun.config=%MAPPING_CONFIG% -Dterminology=ICD9CM -Dversion=2013 -Dinput.file=%MAPPING_DATA%/icd9cm-2013.xml 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Import project data ...%date% %time%
cd %MAPPING_CODE%/admin/import
call mvn install -PMapProject -Drun.config=%MAPPING_CONFIG% -Dinput.dir=%MAPPING_DATA%/ihtsdo-project-data -Dmini=true  1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Create ICD10 and ICD9CM map records ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PCreateMapRecords -Drun.config=%MAPPING_CONFIG% -Drefset.id=447562003,447563008 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load ICPC maps from file ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PMapRecords -Drun.config=%MAPPING_CONFIG% -Dinput.file=%MAPPING_DATA%/der2_iisssccRefset_ExtendedMapSnapshotMini_INT_20140131.txt 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Load map notes from file ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PMapNotes -Drun.config=%MAPPING_CONFIG% -Dinput.file=%MAPPING_DATA%/der2_sRefset_MapNotesSnapshotMini_INT_20140131.txt 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Compute workflow ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PComputeWorkflow -Drun.config=%MAPPING_CONFIG% -Drefset.id=447563008,447562003 1> mvn.log
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


