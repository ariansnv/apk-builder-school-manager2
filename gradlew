#!/bin/sh
# Minimal wrapper — avoids JVM opts parsing bugs on Linux CI
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -Dorg.gradle.appname=gradlew \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
