#!/usr/bin/env bash
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_TOOLS_VERSION="latest"
JAVA_MIN_VERSION=17

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[✓]${NC} $*"; }
warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
error() { echo -e "${RED}[✗]${NC} $*"; exit 1; }

check_prerequisites() {
    log "Checking prerequisites..."

    if ! command -v java &>/dev/null; then
        error "Java not found. Install JDK $JAVA_MIN_VERSION+."
    fi

    local java_version
    java_version=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
    if (( java_version < JAVA_MIN_VERSION )); then
        error "Java $JAVA_MIN_VERSION+ required. Found: $java_version"
    fi
    log "Java $java_version found."

    for cmd in curl unzip; do
        command -v "$cmd" &>/dev/null || error "'$cmd' not found. Please install it."
    done
    log "All prerequisites satisfied."
}

install_sdk() {
    if [[ -d "$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_VERSION/bin" ]]; then
        log "Android SDK already installed at $ANDROID_HOME. Skipping download."
    else
        log "Downloading Android SDK command-line tools..."
        mkdir -p "$ANDROID_HOME/cmdline-tools"
        local tmp_zip="/tmp/cmdline-tools.zip"
        curl -fsSL "$CMDLINE_TOOLS_URL" -o "$tmp_zip"
        unzip -qo "$tmp_zip" -d /tmp/cmdline-extract
        mv /tmp/cmdline-extract/cmdline-tools "$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_VERSION"
        rm -rf "$tmp_zip" /tmp/cmdline-extract
        log "Android SDK command-line tools installed."
    fi

    export ANDROID_HOME
    local sdkmanager="$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_VERSION/bin/sdkmanager"

    log "Accepting SDK licenses..."
    yes | "$sdkmanager" --licenses >/dev/null 2>&1 || true
    log "Licenses accepted."

    log "Installing SDK components (platform-tools, platforms;android-36, build-tools;36.0.0)..."
    "$sdkmanager" "platform-tools" "platforms;android-36" "build-tools;36.0.0" 2>&1 | tail -1
    log "SDK components installed."
}

setup_environment() {
    log "Setting up environment..."

    local profile_file="$HOME/.bashrc"
    local marker="# >>> Android SDK >>>"
    if ! grep -q "$marker" "$profile_file" 2>/dev/null; then
        cat >> "$profile_file" <<EOF
$marker
export ANDROID_HOME="$ANDROID_HOME"
export PATH="\$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_VERSION/bin:\$ANDROID_HOME/platform-tools:\$PATH"
# <<< Android SDK <<<
EOF
        log "Added ANDROID_HOME to $profile_file"
    else
        log "ANDROID_HOME already configured in $profile_file"
    fi

    export ANDROID_HOME
    export PATH="$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_VERSION/bin:$ANDROID_HOME/platform-tools:$PATH"
}

setup_keystore() {
    local keystore="$HOME/.android/debug.keystore"
    if [[ -f "$keystore" ]]; then
        log "Debug keystore already exists."
    else
        log "Generating debug keystore..."
        mkdir -p "$HOME/.android"
        keytool -genkeypair -v \
            -keystore "$keystore" \
            -alias androiddebugkey \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass android -keypass android \
            -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
        log "Debug keystore created."
    fi
}

setup_local_properties() {
    local project_dir="${1:-.}"
    local props_file="$project_dir/local.properties"
    if [[ ! -f "$props_file" ]]; then
        echo "sdk.dir=$ANDROID_HOME" > "$props_file"
        log "Created local.properties"
    else
        log "local.properties already exists."
    fi
}

build_project() {
    local project_dir="${1:-.}"
    local build_type="${2:-debug}"

    log "Building project ($build_type)..."
    chmod +x "$project_dir/gradlew"
    "$project_dir/gradlew" -p "$project_dir" "assemble${build_type^}" --no-daemon

    local apk="$project_dir/app/build/outputs/apk/${build_type}/app-${build_type}.apk"
    if [[ -f "$apk" ]]; then
        log "Build successful! APK: $apk"
    else
        error "Build completed but APK not found at expected path."
    fi
}

main() {
    local project_dir="${1:-.}"
    local build_type="${2:-debug}"

    echo "========================================="
    echo "  Android SDK Setup & Build Script"
    echo "========================================="

    check_prerequisites
    install_sdk
    setup_environment
    setup_keystore
    setup_local_properties "$project_dir"
    build_project "$project_dir" "$build_type"

    echo ""
    log "All done!"
}

main "$@"
