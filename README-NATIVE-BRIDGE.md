# AIUnblock Universal Control - native Android bridge

This archive is the Floot-generated native Android project with the first custom Capacitor bridge added.

## Added

- `UniversalControlPlugin.java` registered as `UniversalControl`.
- Root execution of `universalctl status`, `set`, `reconcile`, and `apply`.
- Current network profile read.
- Wi-Fi direct SSID list read/add/remove.
- `universal.log` reader.
- VLESS/AWG configuration staging.
- MainActivity registration of the plugin.

## Important

The downloaded Floot ZIP contains the web bundle from the last native build. The current Floot source has newer `UniversalControl` calls than that bundle. Do not spend another Floot native build on this. The intended local workflow is to update the web bundle/source and then build Android locally.

## libXray

The exact upstream target is XTLS/libXray `v26.9.9`, commit `50b9597`. The supplied `build-libxray-termux.sh` checks out that exact commit and builds `libXray.aar` using the official `build/main.py android` path.

The VPN methods in the bridge remain gated until the AAR is installed and a real Android `VpnService` is wired to libXray. This is deliberate: starting a fake VPN layer before the exact core is integrated would make the APK look functional while doing the wrong network work.

Upstream libXray documents `Invoke(apiVersion=3)`, `runXray`/`stopXray`, `xray.tun.fd`, Android socket protection and `SetDNS`/`ResetDNS`. It also requires only one independently built Go runtime in a process.
