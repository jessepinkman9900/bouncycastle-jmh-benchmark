FROM eclipse-temurin:17

WORKDIR /app
COPY ./ ./

RUN chmod +x ./mvnw && \
./mvnw -s .mvn/settings.xml -B -f /app/pom.xml dependency:resolve-plugins dependency:resolve dependency:go-offline

ENTRYPOINT ["./mvnw","test"]
