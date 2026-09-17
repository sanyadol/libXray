# AIUnblock Universal native bridge

Phase 1 native bridge for the Floot UI.

Implemented:
- Capacitor `UniversalControl` registration.
- Root `su` execution of `universalctl status/set/reconcile/apply`.
- Current network profile read.
- Wi-Fi direct SSID list read/add/remove.
- `universal.log` read.
- VLESS/AWG config staging.

VPN start/stop is intentionally gated until the exact XTLS/libXray v26.9.9 (commit 50b9597) Android artifact is installed and the real VpnService bridge is added.
