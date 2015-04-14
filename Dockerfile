FROM gfx2015/android-with-emulator:latest

MAINTAINER FUJI Goro <g.psy.va@gmail.com>

ENV PROJECT /project

RUN mkdir $PROJECT
WORKDIR $PROJECT

ADD . $PROJECT

RUN echo "sdk.dir=$ANDROID_HOME" > local.properties && \
    ./gradlew --stacktrace dependencies

CMD start-emulator && \
    ./gradlew --stacktrace build connectedAndroidTest
