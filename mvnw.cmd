@echo off
setlocal

REM Maven Wrapper: use mvnw.cmd instead of mvn (no Maven install needed)
REM Requires: Java 17, JAVA_HOME set

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if "%JAVA_HOME%"=="" (
  echo Error: JAVA_HOME is not set. Set it to your Java 17 install folder.
  echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-17
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Error: JAVA_HOME is not valid: %JAVA_HOME%
  exit /b 1
)

if not exist "%WRAPPER_JAR%" (
  echo First run: downloading Maven Wrapper...
  if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" mkdir "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper"
  set "PROPS=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"
  set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"
  if exist "%PROPS%" for /f "usebackq tokens=1,* delims==" %%a in ("%PROPS%") do if "%%a"=="wrapperUrl" set "DOWNLOAD_URL=%%b"
  powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"
  if not exist "%WRAPPER_JAR%" (
    echo Failed to download Maven Wrapper. Check internet or install Maven from https://maven.apache.org/download.cgi
    exit /b 1
  )
)

"%JAVA_HOME%\bin\java.exe" -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
exit /b %ERRORLEVEL%
