@echo off
cd /d "C:\Users\bergs\Git\InvestmentAccountCalcEngine"
call mvn clean package -q
java -jar "target\my-app-0.0.1-SNAPSHOT.jar"