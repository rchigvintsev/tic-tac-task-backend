# TicTacTask (backend)

This is a pet project that was created in order to learn reactive programming using 
[Project Reactor](https://projectreactor.io) and WebFlux. It is a simple web application that allows to create task
lists according to David Allen's [GTD system](https://en.wikipedia.org/wiki/Getting_Things_Done).

This application heavily relies on Spring Boot 2 and Spring Security frameworks.
[Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc) is used to access PostgreSQL database in a reactive 
manner. [Flyway](https://flywaydb.org/) is used for database migration.

The application uses [Gradle](https://gradle.org) as a build automation tool. The following command can be used to
build the application: `gradlew build`

The following application properties are required in order to run the application:

```properties
spring.security.oauth2.client.registration.facebook.client-id=<Facebook OAuth 2 client id>
spring.security.oauth2.client.registration.facebook.client-secret=<Facebook OAuth 2 client secret>
spring.security.oauth2.client.registration.github.client-id=<Github OAuth 2 client id>
spring.security.oauth2.client.registration.github.client-secret=<Github OAuth 2 client secret>
spring.security.oauth2.client.registration.google.client-id=<Google OAuth 2 client id>
spring.security.oauth2.client.registration.google.client-secret=<Google OAuth 2 client secret>
spring.security.oauth2.client.registration.vk.client-id=<VK OAuth 2 client id>
spring.security.oauth2.client.registration.vk.client-secret=<VK OAuth 2 client secret>

spring.mail.host=<Mail host to be able to send emails to users to confirm their registration or to reset password>
spring.mail.username=<Mail username to be able to send emails to users to confirm their registration or to reset password>
spring.mail.password=<Mail password to be able to send emails to users to confirm their registration or to reset password>

spring.flyway.url=<Database URL for Flyway (also can be set using environment variable "JDBC_DATABASE_URL")>
spring.flyway.username=<Database username for Flyway (also can be set using environment variable "JDBC_DATABASE_USERNAME")>
spring.flyway.password=<Database password for Flyway (also can be set using environment variable "JDBC_DATABASE_PASSWORD")>

spring.r2dbc.url=<Database URL that starts with "r2dbc:" (also can be set using environment variable "R2DBC_DATABASE_URL")>
spring.r2dbc.username=<Database username (also can be set using environment variable "R2DBC_DATABASE_USERNAME")>
spring.r2dbc.password=<Database password (also can be set using environment variable "R2DBC_DATABASE_PASSWORD")>

application.security.authentication.access-token.signing-key=<Signing key for access tokens issued by this application>
```
