FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY dpra /app

RUN chmod +x gradlew
RUN ./gradlew build

EXPOSE 8080

CMD ["java", "-jar", "build/libs/dpra-0.0.1-SNAPSHOT.jar"]