package com.example.tutorial;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloController {
    @FXML
    private Text qule, online, player, uptime, connect;
    @FXML
    private Button GetData, ServerMaps;

    @FXML
    private Button goToSecondScence;


    @FXML
    void switchToSecondScence(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("second.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) goToSecondScence.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    @FXML
    private TextField ServerSearch;

    private String serverID = "";
    private String url = "";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @FXML
    void initialize() {
        GetData.setOnAction(event -> {
            String serverName = ServerSearch.getText().trim();
            if (!serverName.isEmpty()) {
                findServerID(serverName);
            }
        });
        goToSecondScence.setOnAction(this::switchToSecondScence);

//        ServerMaps.setOnAction(event -> {
//            System.out.println("Нажата кнопка");
//            if (!url.isEmpty()) {
//                loadMapInWebView(url);
//                System.out.println("Карта загружена: " + url);
//            } else {
//                System.out.println("Карта недоступна!");
//            }
//        });

        // Автообновление раз в 30 секунд
        scheduler.scheduleAtFixedRate(this::updateServerData, 0, 3, TimeUnit.SECONDS);
    }
    private static final Logger LOGGER = Logger.getLogger(HelloController.class.getName());
    private void findServerID(String serverName) {

        new Thread(() -> {
            try {
                String encodedName = URLEncoder.encode(serverName, StandardCharsets.UTF_8);
                String searchUrl = "https://api.battlemetrics.com/servers?filter[search]=" + encodedName;
                HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray servers = json.getJSONArray("data");

                if (servers.length() > 0) {
                    serverID = servers.getJSONObject(0).getString("id");
                    updateServerData();
                } else {
                    javafx.application.Platform.runLater(() -> {
                        online.setText("Server not found!");
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Ошибка поиска сервера", e);
                javafx.application.Platform.runLater(() -> online.setText("Ошибка 404!"));
            }
        }).start();
    }

    private String fetchServerData() throws IOException {
        if (serverID.isEmpty()) {
            return "{}";
        }
        String apiUrl = "https://api.battlemetrics.com/servers/" + serverID;
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private void updateServerData() {
        new Thread(() -> {
            try {
                String jsonResponseString = fetchServerData();
                JSONObject jsonResponse = new JSONObject(jsonResponseString);

                if (!jsonResponse.has("data")) {
                    //System.out.println("Ошибка: data отсутствует!");
                    return;
                }

                JSONObject data = jsonResponse.getJSONObject("data");
                JSONObject attributes = data.optJSONObject("attributes");

                if (attributes == null) {
                    System.out.println("Ошибка: attributes отсутствует!");
                    return;
                }

                JSONObject details = attributes.optJSONObject("details");
                if (details != null) {
                    JSONObject rustMaps = details.optJSONObject("rust_maps");
                    if (rustMaps != null) {
                        url = rustMaps.optString("url", "");
                    }
                }

                String serverName = attributes.optString("name", "Неизвестный сервер");
                boolean isOnline = attributes.optString("status", "offline").equals("online");
                int players = attributes.optInt("players", 0);
                String connectLink = attributes.optString("ip", "Нет данных") + ":" + attributes.optInt("port", 0);

                int queuedPlayers = 0;
                JSONArray tags = details != null ? details.optJSONArray("tags") : new JSONArray();
                for (int i = 0; i < tags.length(); i++) {
                    String tag = tags.optString(i, "");
                    if (tag.startsWith("qp")) {
                        try {
                            queuedPlayers = Integer.parseInt(tag.substring(2));
                        } catch (NumberFormatException ignored) {
                            queuedPlayers = 0;
                        }
                        break;
                    }
                }

                int lastQueued = queuedPlayers;

                Platform.runLater(() -> {
                    online.setText("Онлайн: " + (isOnline ? "Да" : "Нет"));
                    player.setText("Игроков: " + players);
                    uptime.setText("Сервер: " + serverName);
                    connect.setText("Connect: " + connectLink);
                    qule.setText("Очередь: " + lastQueued);

//                    if (!url.isEmpty()) {
//                        loadMapInWebView(url);
//                    }
                });

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Ошибка при загрузке данных", e);
                Platform.runLater(() -> online.setText("Ошибка загрузки данных!"));
            }
        }).start();
    }

//    private void loadMapInWebView(String mapUrl) {
//        javafx.application.Platform.runLater(() -> {
//            try {
//                if (mapUrl == null || mapUrl.isEmpty()) {
//                    System.out.println("Ошибка: пустой URL карты");
//                    return;
//                }
//                WebEngine webEngine = webView.getEngine();
//                webEngine.load(mapUrl);
//            } catch (Exception e) {
//                System.out.println("Ошибка загрузки карты: " + e.getMessage());
//            }
//        });
//    }
}
