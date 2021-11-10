FROM docker-all.repo.ebaotech.com/openjdk:adoptopenjdk-11 as BUILD
WORKDIR /application
ADD target/*-release.zip /app.zip
RUN unzip /app.zip -d .

FROM docker-all.repo.ebaotech.com/openjdk:adoptopenjdk-11

WORKDIR /application
COPY --from=BUILD /application /application

ENV JAVA_OPTS="$JAVA_OPTS "
CMD ["sh", "-c", "bin/app"]