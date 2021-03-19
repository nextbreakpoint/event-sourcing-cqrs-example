FROM maven:3-openjdk-11 AS build
ADD . /src
WORKDIR /src
RUN mvn clean verify -s settings.xml -t toolchains.xml -DskipTests=true

#FROM oracle/graalvm-ce:20.3.0-java11 as native-image
#COPY --from=build /src/target/libs/* /libs/
#RUN gu install native-image
#WORKDIR /
#RUN native-image --verbose -J-Xmx4G -cp $(find /libs -name "*.jar" -print | sed "s/.jar/.jar:/g" | tr -d '\n' | sed "s/:$//g") -H:+StaticExecutableWithDynamicLibC -H:Name=accounts

#FROM gcr.io/distroless/base
#COPY --from=native-image accounts /accounts
FROM openjdk:11-jdk
COPY --from=build /src/target/libs/* /libs/
COPY --from=build /src/target/com.nextbreakpoint.blueprint.accounts-1.0.0.jar /libs/
COPY entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/etc/config.json"]
