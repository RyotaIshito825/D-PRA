FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY dpra /app

RUN chmod +x gradlew
RUN ./gradlew build

EXPOSE 8080

CMD ["sh", "-c", "java -jar build/libs/*SNAPSHOT.jar"]