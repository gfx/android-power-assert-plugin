FROM gfx2015/android:latest

MAINTAINER FUJI Goro <g.psy.va@gmail.com>

ENV PROJECT /project

RUN mkdir $PROJECT
WORKDIR $PROJECT

ADD . $PROJECT

RUN echo yes | android update sdk -a -u -t sys-img-x86_64-android-22 && \
    echo no | android create avd --force -n test -t android-22 && \
    echo "sdk.dir=$ANDROID_HOME" > local.properties && \
    ./gradlew --stacktrace dependencies

CMD emulator -avd test -no-skin -no-audio -no-window & ; \
    ./android-wait-for-emulator && \
    ./gradlew --stacktrace build connectedAndroidTest
