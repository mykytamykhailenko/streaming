ARG PROJECT=kafka

# See https://github.com/mozilla/docker-sbt#building
FROM mozilla/sbt:local AS build

# In multi-stage builds, you should renew ARGs per build phase.
# Otherwise, they disappear.
ARG PROJECT

WORKDIR /home/kafka

COPY . .

RUN sbt ${PROJECT}/assembly

FROM openjdk:11 as deploy
ARG PROJECT

# Variable substitution for ARGs does not work correctly in ENTRYPOINT.
# So you need to assign them to ENVs.
ENV PROJECT_JAR=${PROJECT}.jar

RUN useradd -m kafka

USER kafka

WORKDIR /home/kafka/

# Changing user ownership in RUN command takes a lot of time.
# COPY with --chown speeds up the process significantly.
COPY --from=build --chown=kafka:kafka /home/kafka/jars/ .

ENTRYPOINT java -jar ${PROJECT_JAR}

