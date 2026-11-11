#!/usr/bin/env bash
set -euo pipefail

release_dir="${1:?Usage: verify-release.sh APK_DIRECTORY VERSION}"
version="${2:?Missing release version}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK path}"
build_tools="$ANDROID_HOME/build-tools/37.0.0"
abis=(arm64-v8a armeabi-v7a x86 x86_64)
certificate=""

cd "$release_dir"
for distribution in '' '-fdroid'; do
    package="com.v2raynh.android${distribution/-/.}"
    for abi in "${abis[@]}" universal; do
        apk="v2rayNH_${version}${distribution}_${abi}.apk"
        signing_info="$("$build_tools/apksigner" verify --print-certs "$apk")"
        digest="$(awk -F ': ' '/certificate SHA-256 digest/ { print $2 }' <<< "$signing_info")"
        [[ -n "$digest" ]]
        [[ -z "$certificate" || "$certificate" = "$digest" ]]
        certificate="$digest"

        badging="$("$build_tools/aapt" dump badging "$apk")"
        grep -Fq "package: name='$package'" <<< "$badging"
        grep -Fq "versionName='$version'" <<< "$badging"
        contents="$(unzip -Z1 "$apk")"
        expected=("$abi")
        if [[ "$abi" = universal ]]; then expected=("${abis[@]}"); fi
        for arch in "${expected[@]}"; do
            for library in libgojni libhev-socks5-tunnel libhevsockstun; do
                grep -Fxq "lib/$arch/$library.so" <<< "$contents"
            done
        done
        actual="$(awk -F '/' '/^lib\/[^/]+\/[^/]+\.so$/ { print $2 }' <<< "$contents" | sort -u)"
        [[ "$actual" = "$(printf '%s\n' "${expected[@]}" | sort)" ]]
    done
done
sha256sum ./*.apk > SHA256SUMS
printf 'APK signer SHA-256: %s\n' "$certificate"
