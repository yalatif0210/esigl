# Use the OpenJDK 8 image with full JDK as base
FROM openjdk:8-jdk

# Set the working directory in the container
WORKDIR /app


# Install Node.js and npm
RUN apt-get update && \
    apt-get install -y nodejs npm

RUN npm install -g grunt-cli

# Copy the current directory contents into the container at /app
COPY . /app

# Change permissions and build the project
RUN chmod +x ./gradlew && \
    ./gradlew build -x test -x migrateExtensions

# Start a new stage for the final Tomcat image
FROM tomcat:8.5.29-jre8

# Copy the built WAR file and database config
COPY --from=0 /app/modules/openlmis-web/build/libs/openlmis-web.war ./
COPY database-config.properties /usr/local/tomcat/lib/

# Remove default Tomcat ROOT application and deploy the custom WAR as ROOT
RUN rm -R /usr/local/tomcat/webapps/ROOT && \
    rm -f /usr/local/tomcat/webapps/ROOT.war && \
    export defaultLocale=fr && \
    mv openlmis-web.war /usr/local/tomcat/webapps/ROOT.war