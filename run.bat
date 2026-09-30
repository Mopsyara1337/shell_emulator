@echo off
cd /d "%~dp0"
javac -d out src\*.java
java -cp out Main %*