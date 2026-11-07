# !bash
# !usr/bin/env zsh

javac -d "./bin" -cp "./lib/*" ./src/assignment/*.java test/assignment/*.java
java -jar "./lib/junit-platform-console-standalone-1.10.0.jar" execute -e=junit-jupiter -cp "./bin" --scan-class-path