@echo off
setlocal
set GRADLE_VERSION=8.13
set INSTALLED_VERSION=
where gradle >nul 2>&1
if errorlevel 1 goto bootstrap
for /f "tokens=2" %%V in ('gradle --version ^| findstr /B /C:"Gradle "') do set INSTALLED_VERSION=%%V
if "%INSTALLED_VERSION%"=="%GRADLE_VERSION%" goto use_system
:bootstrap
set GRADLE_HOME_BASE=%USERPROFILE%\.gradle\cinema-tv-bootstrap
set GRADLE_HOME_DIR=%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%
set GRADLE_ZIP=%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%-bin.zip
if exist "%GRADLE_HOME_DIR%\bin\gradle.bat" goto run_gradle
if not exist "%GRADLE_HOME_BASE%" mkdir "%GRADLE_HOME_BASE%"
echo Gradle %GRADLE_VERSION% is not installed. Downloading official distribution...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'; Expand-Archive -Force '%GRADLE_ZIP%' '%GRADLE_HOME_BASE%'"
if errorlevel 1 exit /b 1
:run_gradle
call "%GRADLE_HOME_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
:use_system
gradle %*
exit /b %ERRORLEVEL%
