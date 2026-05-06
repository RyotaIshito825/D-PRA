FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY dpra/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]