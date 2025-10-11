
package com.minkang.ultimate.usershop.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    public static void send(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            String json = "{\"content\":" + toJsonString(content) + "}";
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(out.length);
            con.connect();
            try (OutputStream os = con.getOutputStream()) {
                os.write(out);
            }
            int code = con.getResponseCode();
            // ignore response; best-effort
        } catch (Exception ignored) {}
    }

    private static String toJsonString(String s) {
        if (s == null) return "null";
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"")
                          .replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }
}
