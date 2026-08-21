package com.pilotcompanion.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class CloudScheduleSync {
    static final String SHARED_URL = "https://raw.githubusercontent.com/Elbo79/pilot-companion/main/shared_schedule.txt";

    static String download() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SHARED_URL).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "PilotCompanion/0.6");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
            return out.toString();
        } finally { connection.disconnect(); }
    }
}
