# Builds and runs the API. Kept at the repository root, and every path below is
# written from here, because a host that resolves them from somewhere else
# cannot find the file and the build fails before it starts.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies first: this layer is reused whenever only source files change.
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline

COPY backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/food-delivery-*.jar app.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
