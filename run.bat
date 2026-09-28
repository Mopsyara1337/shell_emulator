@echo off
cd /d "%~dp0"
javac -d out src\Main.java
java -cp out Main %*