FROM maven:3-openjdk-11 AS build
ARG maven_args
ADD . /src
WORKDIR /src
RUN mvn clean verify -s settings.xml -t toolchains.xml -DexcludedGroups=slow $maven_args

#FROM oracle/graalvm-ce:20.3.0-java11 as native-image
#COPY --from=build /src/target/libs/* /libs/
#RUN gu install native-image
#WORKDIR /
#RUN native-image --verbose -J-Xmx4G -cp $(find /libs -name "*.jar" -print | sed "s/.jar/.jar:/g" | tr -d '\n' | sed "s/:$//g") -H:+StaticExecutableWithDynamicLibC -H:Name=designs-aggregate-fetcher

#FROM gcr.io/distroless/base
#COPY --from=native-image designs-aggregate-fetcher /designs-aggregate-fetcher
FROM openjdk:11-jdk
COPY --from=build /src/target/libs/* /libs/
RUN rm /libs/jffi-*-native.jar
COPY --from=build /src/target/com.nextbreakpoint.blueprint.designs-aggregate-fetcher-1.0.0.jar /libs/
COPY entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/etc/config.json"]
