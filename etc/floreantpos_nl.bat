@echo off
if exist "%~dp0jre\bin\java.exe" (
    set JAVA_CMD="%~dp0jre\bin\java.exe"
) else (
    set JAVA_CMD=java
)
%JAVA_CMD% -jar -Duser.language=nl "%~dp0floreantpos.jar"
