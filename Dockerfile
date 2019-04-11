FROM runmymind/docker-android-sdk

ENV ANDROID_SDK_HOME /opt/android-sdk-linux
ENV ANDROID_SDK_ROOT /opt/android-sdk-linux
ENV ANDROID_HOME /opt/android-sdk-linux
ENV ANDROID_SDK /opt/android-sdk-linux

ENV DEBIAN_FRONTEND noninteractive

# Install required tools
# Dependencies to execute Android builds

RUN apt-get update -yqq && apt-get install -y openjfx && apt-get clean

WORKDIR /opt/android-sdk-linux

RUN /opt/tools/entrypoint.sh built-in

CMD /opt/tools/entrypoint.sh built-in