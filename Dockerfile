# Use an official Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-17 as build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml first
COPY pom.xml .

# Copy and run the download script to get the LookseeCore JAR
COPY scripts/download-core.sh ./scripts/download-core.sh
RUN chmod +x ./scripts/download-core.sh
RUN bash ./scripts/download-core.sh
RUN mvn install:install-file -Dfile=libs/core-0.3.7.jar -DgroupId=com.looksee -DartifactId=core -Dversion=0.3.7 -Dpackaging=jar

# Copy the rest of the project source code
COPY src ./src

# Build the application (Maven will download and install core JAR automatically)
RUN mvn clean install -DskipTests

# Use a smaller JDK image to run the app
FROM eclipse-temurin:17-jre

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
EXPOSE 80
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms3G", "-ea","-jar", "app.jar"]