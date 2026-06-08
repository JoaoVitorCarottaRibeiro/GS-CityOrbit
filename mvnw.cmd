@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET ___MVNW_UGLY_INTERRUPT___=
@SETLOCAL
@SET MAVEN_PROJECTBASEDIR=%~dp0
@SET MVNW_REPOURL=https://repo.maven.apache.org/maven2
@SET MVNW_DISTRIBUTION_URL_DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
@SET DISTRIBUTION_FILENAME=apache-maven-3.9.6-bin.zip
@SET DISTRIBUTION_FOLDER=apache-maven-3.9.6
@SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET DOWNLOAD_URL=%MVNW_DISTRIBUTION_URL_DOWNLOAD_URL%
@SET M2_HOME=%USERPROFILE%\.m2\wrapper\dists\%DISTRIBUTION_FOLDER%
@IF NOT EXIST "%M2_HOME%\bin\mvn.cmd" (
  @ECHO Baixando Apache Maven 3.9.6...
  @MKDIR "%M2_HOME%" 2>nul
  @POWERSHELL -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%TEMP%\%DISTRIBUTION_FILENAME%'"
  @POWERSHELL -Command "Expand-Archive -Path '%TEMP%\%DISTRIBUTION_FILENAME%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\' -Force"
)
@SET PATH=%M2_HOME%\bin;%PATH%
@CALL mvn %*
