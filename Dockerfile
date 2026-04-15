FROM openjdk:17
COPY target/demo.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]