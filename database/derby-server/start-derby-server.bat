set CLASSPATH=lib\*
cd database\derby-server
..\..\jre\bin\java -cp %CLASSPATH% org.apache.derby.drda.NetworkServerControl start -h 0.0.0.0 -p 51527
pause