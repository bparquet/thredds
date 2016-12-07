pushd "%~dp0%"
call gradlew.bat buildnetcdfandroid
@REM ==========================================================================
@REM   Is there some way in Gradle to build a sources.jar from ALL of the 
@REM   Sources?
@REM ==========================================================================
for %%I in ("%CD%") DO (
  SET SOURCES_JAR=%%~fsI\build\libs\netcdfAll-4.6.6-SOURCES.jar
)
del %SOURCES_JAR%
for /f "tokens=*" %%A in ('dir /s /b /a:d java') do (
  pushd %%~fsA
  7za -tzip a %SOURCES_JAR% *
  popd
)
popd

