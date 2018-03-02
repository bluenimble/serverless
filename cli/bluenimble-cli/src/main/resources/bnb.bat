@echo off
rem
rem Copyright (c) BlueNimble, Inc. (https://www.bluenimble.com)
rem

rem Guess BN_HOME if not defined
set CURRENT_DIR=%cd%

if exist "%JAVA_HOME:"=%\bin\java.exe" goto setJavaHome
set JAVA=java
goto okJava

:setJavaHome
set JAVA="%JAVA_HOME:"=%\bin\java"

:okJava
if not "%BN_HOME%" == "" goto gotHome
set BN_HOME=%CURRENT_DIR%
if exist "%BN_HOME%\bnb.bat" goto okHome
cd ..
set BN_HOME=%cd%
cd %CURRENT_DIR%

:gotHome
if exist "%BN_HOME%\bnb.bat" goto okHome
echo The BN_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=

:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs

:doneSetArgs

set JAVA_OPTS_SCRIPT=-Xms60m -Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF8 -DBN_HOME=%BN_HOME%

rem TO DEBUG BlueNimble RUN WITH THESE OPTIONS:
rem -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
rem AND ATTACH TO THE CURRENT HOST, PORT 1044

rem BlueNimble MAXIMUM HEAP. USE SYNTAX -Xmx<memory>, WHERE <memory> HAS THE TOTAL MEMORY AND SIZE UNIT. EXAMPLE: -Xmx512m
set MAXHEAP=-Xmx384m

set CPATH=%BN_HOME%/boot/bluenimble-jvm-sdk-[version].jar;%BN_HOME%/boot/bluenimble-cli-boot-[version].jar

call %JAVA% %MAXHEAP% %JAVA_OPTS_SCRIPT% -cp %CPATH% com.bluenimble.platform.icli.mgm.boot.BnMgmICli

:end
