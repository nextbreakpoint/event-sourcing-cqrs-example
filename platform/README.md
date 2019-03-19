# event-sourcing-cqrs-example

Generate secrets:

    ./scripts/secrets.sh

Create Docker networks:

    docker network create shop-test

Build and execute tests:

    mvn clean verify

Build without testing:

    mvn clean package -DskipTests=true
