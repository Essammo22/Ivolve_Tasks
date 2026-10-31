FROM maven:3.10-eclipse-temurin-17-alpine

WORKDIR /app
RUN adduser -D appuser && chown appuser:appuser /app
COPY --chown=appuser:appuser . .
USER appuser

RUN mvn package

EXPOSE 8080

CMD ["java", "-jar", "target/calculator.jar"]
