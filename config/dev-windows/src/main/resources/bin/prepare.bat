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
call mvn install -PReindex -Drun.config=%MAPPING_CONFIG% 1> mvn.log
IF %ERRORLEVEL% NEQ 0 (set error=1
goto trailer)
del /Q mvn.log

echo     Create admin user and empty project ...%date% %time%
cd %MAPPING_CODE%/admin/loader
call mvn install -PCreateMapAdmin -Drun.config=%MAPPING_CONFIG% -Dmap.user=admin 1> mvn.log
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
