# v2rayNH

An independent Android fork of [v2rayNG 2.3.14](https://github.com/2dust/v2rayNG/tree/2.3.14) with **TUIC v5** support and background subscription refresh.

## What changed

- **TUIC v5.** Import or export `tuic://` links and create profiles in the app. The APK includes an Xray-core build with the TUIC outbound.
- **Subscription refresh.** New subscriptions refresh every two hours by default. Opening or returning to the app checks for due updates. The interval and enable switch are editable per subscription. Android may defer scheduled work while the device is idle or offline.
- **Default routing.** A fresh configuration uses the built-in Russia whitelist preset. The routing screen still offers the other presets and custom rules.
- **Subscription UI.** The tab uses the provider's title when available. The scrollable header shows traffic and a compact, centered announcement. Profile rows show the name, protocol and ping after a test.

## Download

Get the APK from this repository's **Releases** page. Both the standard and F-Droid-style builds are available for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. Use `universal` if you are unsure. The F-Droid-style build is a separate variant and can be downloaded here.

The standard app ID is `com.v2raynh.android`. The F-Droid-style variant adds `.fdroid`. Each can be installed alongside upstream v2rayNG. App data is separate. Export a backup from the older app and import it in the fork if you want to migrate your settings.

## Build from source

Use Linux with JDK 21, Go 1.27.3, Git, curl, jq, unzip and the Android SDK. Install platform `android-37.0`, Build Tools `37.0.0`, and NDKs `29.0.14206865` and `30.0.16248370`. Set `ANDROID_HOME` to the SDK directory.

```sh
bash scripts/build-native.sh
cd V2rayNG
./gradlew :app:testPlaystoreDebugUnitTest :app:testFdroidDebugUnitTest
./gradlew :app:assemblePlaystoreDebug :app:assembleFdroidDebug
```

The native build checks out the pinned Xray-core revision and applies [the TUIC patch](patches/xray-tuic.patch). It builds the four ABIs from that source and the pinned AndroidLibXrayLite and HEV submodules.

For signed releases, set `FORK_KEYSTORE`, `FORK_KEYSTORE_PASSWORD`, `FORK_KEY_ALIAS`, and `FORK_KEY_PASSWORD` in the build environment. Set the repository explicitly with `-PFORK_GITHUB_REPOSITORY=OWNER/REPO` when running `:app:assembleRelease`. That repository is the only source for in-app APK updates. Local builds without the property skip the update check.

Use the same signing key and increment `versionCode` for each release. Tag releases `vMAJOR.MINOR.PATCH` to match `versionName`. The GitHub Actions build can package and verify signed APKs with the corresponding repository secrets.

## Source and licensing

The Android application is based on [2dust/v2rayNG](https://github.com/2dust/v2rayNG) and retains its [GPL-3.0 license](LICENSE). The native build uses [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite), [Xray-core](https://github.com/XTLS/Xray-core), and [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) under their respective licenses. The TUIC changes to Xray-core are supplied as source in the patch above.
