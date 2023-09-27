FROM maven:3-eclipse-temurin-17 AS build
ARG maven_args
ADD . /src
WORKDIR /src
RUN mvn clean verify -s settings.xml -t toolchains.xml $maven_args -DexcludedGroups=docker

#FROM oracle/graalvm-ce:22.3.1-java17 as native-image
#COPY --from=build /src/target/libs/* /libs/
#RUN gu install native-image
#WORKDIR /
#RUN native-image --verbose -J-Xmx4G -cp $(find /libs -name "*.jar" -print | sed "s/.jar/.jar:/g" | tr -d '\n' | sed "s/:$//g") -H:+StaticExecutableWithDynamicLibC -H:Name=designs-watch

#FROM gcr.io/distroless/base
#COPY --from=native-image designs-watch /designs-watch
FROM eclipse-temurin:17-jdk
COPY --from=build /src/target/libs/* /libs/
COPY --from=build /src/target/service.jar /libs/
COPY entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/etc/config.json"]
