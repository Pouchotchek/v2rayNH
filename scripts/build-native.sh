#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK path}"
export NDK_HOME="$ANDROID_HOME/ndk/30.0.16248370"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/29.0.14206865"
test -d "$NDK_HOME"
test -d "$ANDROID_NDK_HOME"

build_dir="$(mktemp -d)"
trap 'rm -rf -- "$build_dir"' EXIT
xray_revision=1b1647cd9ddf0abc34b981d4611fc03bcac1ca74

git -C "$repo_dir" submodule update --init --recursive
git init -q "$build_dir/xray"
git -C "$build_dir/xray" fetch --depth=1 https://github.com/XTLS/Xray-core.git "$xray_revision"
git -C "$build_dir/xray" checkout --detach FETCH_HEAD
git -C "$build_dir/xray" apply "$repo_dir/patches/xray-tuic.patch"
(
    cd "$build_dir/xray"
    go test ./infra/conf -run '^TestTuic'
    go test ./proxy/tuic ./transport/internet/tuic
)

mkdir "$build_dir/libxray"
git -C "$repo_dir/AndroidLibXrayLite" archive HEAD | tar -xf - -C "$build_dir/libxray"
(
    cd "$build_dir/libxray"
    go mod edit -replace github.com/xtls/xray-core="$build_dir/xray"
    go mod tidy
    mkdir -p assets data
    bash gen_assets.sh download
    cp data/*.dat assets/
    export GOBIN="$build_dir/bin"
    mobile_version="$(go list -m -f '{{.Version}}' golang.org/x/mobile)"
    go install "golang.org/x/mobile/cmd/gomobile@$mobile_version" "golang.org/x/mobile/cmd/gobind@$mobile_version"
    PATH="$GOBIN:$PATH" gomobile bind -target=android -androidapi 24 -trimpath \
        -ldflags='-s -w -buildid= -checklinkname=0' ./
)

bash "$repo_dir/compile-hevtun.sh"
apk_libs="$repo_dir/V2rayNG/app/libs"
mkdir -p "$apk_libs"
cp -r "$repo_dir/libs/." "$apk_libs/"
cp "$build_dir/libxray/libv2ray.aar" "$apk_libs/"
