FROM openjdk:8-jdk-alpine
ENTRYPOINT ["java","-jar","/opt/couchdbaccessmanager.jar"]
ADD target/couchdbaccessmanager-*.jar /opt/couchdbaccessmanager.jar
