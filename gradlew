#!/usr/bin/env sh
# Lightweight bootstrapper for this project. It downloads the official Gradle
# distribution when Gradle 8.13 is not installed locally. It is not the official
# Gradle Wrapper JAR implementation.
set -eu

GRADLE_VERSION="8.13"
if command -v gradle >/dev/null 2>&1; then
  INSTALLED_VERSION="$(gradle --version 2>/dev/null | awk '/^Gradle / { print $2; exit }' || true)"
  if [ "${INSTALLED_VERSION}" = "${GRADLE_VERSION}" ]; then
    exec gradle "$@"
  fi
fi

GRADLE_HOME_BASE="${GRADLE_USER_HOME:-${HOME}/.gradle}/cinema-tv-bootstrap"
GRADLE_HOME_DIR="${GRADLE_HOME_BASE}/gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${GRADLE_HOME_BASE}/gradle-${GRADLE_VERSION}-bin.zip"
mkdir -p "${GRADLE_HOME_BASE}"

if [ ! -x "${GRADLE_HOME_DIR}/bin/gradle" ]; then
  echo "Gradle ${GRADLE_VERSION} is not installed. Downloading the official distribution..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "${GRADLE_ZIP}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${GRADLE_ZIP}" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Please install Gradle ${GRADLE_VERSION}, or install curl/wget to download it." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "${GRADLE_ZIP}" -d "${GRADLE_HOME_BASE}"
  else
    echo "The unzip command is required to unpack Gradle." >&2
    exit 1
  fi
fi
exec "${GRADLE_HOME_DIR}/bin/gradle" "$@"
