# !bash
# !usr/bin/env zsh

javac -d "bin" -cp "lib/*" src/assignment/*.java test/assignment/*.java
java -cp "bin:lib/*" assignment.GUI