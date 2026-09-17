package com.aiunblock.universal.control;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "UniversalControl")
public class UniversalControlPlugin extends Plugin {
    private static final String MODDIR = "/data/adb/modules/aiunblock-universal";
    private static final String CTL = MODDIR + "/bin/universalctl";
    private static final String PROFILE = MODDIR + "/run/network-profile.env";
    private static final String DIRECT_LIST = MODDIR + "/config/wifi_direct_ssids.list";
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static JSObject result(String stdout, String stderr, int code) {
        JSObject out = new JSObject();
        out.put("ok", code == 0);
        out.put("stdout", stdout == null ? "" : stdout);
        out.put("stderr", stderr == null ? "" : stderr);
        out.put("code", code);
        return out;
    }

    private static String shQuote(String value) {
        if (value == null) return "''";
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private void execRoot(PluginCall call, String command) {
        executor.execute(() -> {
            String stdout = "";
            String stderr = "";
            int code = 127;
            try {
                Process p = new ProcessBuilder("su", "-c", command)
                        .redirectErrorStream(false)
                        .start();
                StringBuilder so = new StringBuilder();
                StringBuilder se = new StringBuilder();
                Thread outThread = new Thread(() -> readStream(p.getInputStream(), so));
                Thread errThread = new Thread(() -> readStream(p.getErrorStream(), se));
                outThread.start();
                errThread.start();
                code = p.waitFor();
                outThread.join();
                errThread.join();
                stdout = so.toString();
                stderr = se.toString();
            } catch (Throwable t) {
                stderr = t.toString();
            }
            JSObject r = result(stdout, stderr, code);
            getActivity().runOnUiThread(() -> call.resolve(r));
        });
    }

    private static void readStream(java.io.InputStream stream, StringBuilder target) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) target.append(line).append('\n');
        } catch (Exception ignored) {}
    }

    private void ctl(PluginCall call, String args) {
        execRoot(call, "if [ -x " + shQuote(CTL) + " ]; then " +
                shQuote(CTL) + " " + args + "; else echo 'universalctl not found' >&2; exit 127; fi");
    }

    @PluginMethod public void status(PluginCall call) { ctl(call, "status"); }

    @PluginMethod public void set(PluginCall call) {
        String key = call.getString("key", "");
        String value = call.getString("value", "");
        if (!key.matches("[A-Za-z0-9_]+") || !value.matches("[A-Za-z0-9_.:-]+")) {
            call.reject("Invalid universalctl arguments"); return;
        }
        ctl(call, "set " + shQuote(key) + " " + shQuote(value));
    }

    @PluginMethod public void reconcile(PluginCall call) { ctl(call, "reconcile"); }
    @PluginMethod public void apply(PluginCall call) { ctl(call, "apply"); }

    @PluginMethod public void wifiStatus(PluginCall call) {
        execRoot(call, "if [ -f " + shQuote(PROFILE) + " ]; then cat " + shQuote(PROFILE) + "; else echo 'SSID='; fi");
    }

    @PluginMethod public void wifiDirectList(PluginCall call) {
        execRoot(call, "F=" + shQuote(DIRECT_LIST) + "; if [ -f \"$F\" ]; then cat \"$F\"; fi");
    }

    @PluginMethod public void wifiDirectAdd(PluginCall call) {
        String ssid = call.getString("ssid", "").trim();
        if (ssid.isEmpty() || ssid.contains("\n") || ssid.contains("\r")) { call.reject("Invalid SSID"); return; }
        execRoot(call, "F=" + shQuote(DIRECT_LIST) + "; mkdir -p \"$(dirname \"$F\")\" && touch \"$F\" && grep -Fqx -- " + shQuote(ssid) + " \"$F\" || printf '%s\\n' " + shQuote(ssid) + " >> \"$F\"");
    }

    @PluginMethod public void wifiDirectRemove(PluginCall call) {
        String ssid = call.getString("ssid", "").trim();
        if (ssid.isEmpty() || ssid.contains("\n") || ssid.contains("\r")) { call.reject("Invalid SSID"); return; }
        execRoot(call, "F=" + shQuote(DIRECT_LIST) + "; if [ -f \"$F\" ]; then TMP=\"$F.tmp.$$\"; grep -Fvx -- " + shQuote(ssid) + " \"$F\" > \"$TMP\" || true; mv \"$TMP\" \"$F\"; fi");
    }

    @PluginMethod public void logs(PluginCall call) {
        execRoot(call, "F=" + shQuote(MODDIR + "/run/universal.log") + "; if [ -f \"$F\" ]; then tail -n 300 \"$F\"; else echo 'universal.log not found'; fi");
    }

    @PluginMethod public void vpnPrepare(PluginCall call) {
        JSObject out = new JSObject();
        out.put("ready", false);
        out.put("error", "libXray Android bridge is not installed yet");
        call.resolve(out);
    }

    @PluginMethod public void vpnSetVlessUri(PluginCall call) {
        String uri = call.getString("uri", "").trim();
        if (!uri.startsWith("vless://")) { call.reject("Invalid VLESS URI"); return; }
        execRoot(call, "mkdir -p " + shQuote(MODDIR + "/run") + " && printf '%s\\n' " + shQuote(uri) + " > " + shQuote(MODDIR + "/run/vless_uri"));
    }

    @PluginMethod public void vpnSetAwgConfig(PluginCall call) {
        String config = call.getString("config", "");
        if (config.trim().isEmpty()) { call.reject("Empty AmneziaWG config"); return; }
        String safe = config.replace("AIU_AWG_EOF", "AIU_AWG_EOF_");
        execRoot(call, "mkdir -p " + shQuote(MODDIR + "/run") + " && cat > " + shQuote(MODDIR + "/run/awg.conf") + " <<'AIU_AWG_EOF'\n" + safe + "\nAIU_AWG_EOF");
    }

    @PluginMethod public void vpnActivate(PluginCall call) { call.reject("VPN bridge is pending libXray integration"); }
    @PluginMethod public void vpnStart(PluginCall call) { call.reject("VPN bridge is pending libXray integration"); }

    @PluginMethod public void vpnDeactivate(PluginCall call) {
        JSObject out = new JSObject(); out.put("ok", true); call.resolve(out);
    }
    @PluginMethod public void vpnStop(PluginCall call) {
        JSObject out = new JSObject(); out.put("ok", true); call.resolve(out);
    }

    @Override public void handleOnDestroy() {
        executor.shutdownNow();
        super.handleOnDestroy();
    }
}
