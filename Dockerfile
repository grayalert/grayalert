FROM openjdk:17-jdk as builder
COPY . .
RUN ./gradlew build --refresh-dependencies

FROM openjdk:17.0.1-slim

COPY --from=builder ./build/libs/grayalert-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
VOLUME /tmp

ENV JDK_JAVA_OPTIONS="-XshowSettings:vm -Xms128m -Xmx1024m"

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=GMT","-jar","/app.jar"]
