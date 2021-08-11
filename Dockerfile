FROM openjdk:16-alpine
RUN apk --no-cache add curl jq
LABEL "maintainer"="rchigvintsev@gmail.com"
COPY build/libs/*.jar orchestra.jar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "/orchestra.jar"]
HEALTHCHECK --start-period=10s --interval=10s --timeout=3s --retries=3 \
    CMD curl --silent --fail --request GET http://localhost:8080/actuator/health \
        | jq --exit-status '.status == "UP"' || exit 1
