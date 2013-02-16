mvn -U clean eclipse:clean -f ../pom.xml
mvn release:clean release:prepare -f ../pom.xml
mvn release:perform -f ../pom.xml
