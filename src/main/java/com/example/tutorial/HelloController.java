package com.example.tutorial;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class HelloController {

    public Text qule;
    @FXML
    private Button GetData;
    @FXML
    private Button ServerMaps;
    private String url = "";

    @FXML
    private WebView webView;

    @FXML
    private TextField ServerSearch;

    @FXML
    private Text connect, online, player, uptime;

    @FXML
    void initialize() {
        GetData.setOnAction(event -> {
            String serverName = ServerSearch.getText().trim();
            if (!serverName.isEmpty()) {
                findServerID(serverName);
            }
        });

        ServerMaps.setOnAction(event -> {
            System.out.println("Нажата кнопка");
            System.out.println("Карта URL: " + url);
            if (!url.isEmpty()) {
                loadMapInWebView(url);
                System.out.println("Карта URL: " + url);
            } else {
                System.out.println("Карта недоступна!");
            }
        });
    }

    private void loadMapInWebView(String mapUrl) {
        javafx.application.Platform.runLater(() -> {
            try {
                if (mapUrl == null || mapUrl.isEmpty()) {
                    System.out.println("Ошибка: пустой URL карты");
                    return;
                }
                WebEngine webEngine = webView.getEngine();
                webEngine.load(mapUrl);
            } catch (Exception e) {
                System.out.println("Ошибка загрузки карты: " + e.getMessage());
            }
        });
    }

    private void findServerID(String serverName) {
        new Thread(() -> {
            try {
                String encodedName = URLEncoder.encode(serverName, "UTF-8");
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
                    String serverID = servers.getJSONObject(0).getString("id"); // Берем первый сервер из списка
                    loadServerData(serverID);
                } else {
                    javafx.application.Platform.runLater(() -> {
                        online.setText("Server not found!");
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    online.setText("Error finding server");
                });
            }
        }).start();
    }

    private void loadServerData(String serverID) {
        new Thread(() -> {
            try {
                String apiUrl = "https://api.battlemetrics.com/servers/" + serverID;
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (!jsonResponse.has("data")) {
                    System.out.println("Ошибка: `data` отсутствует!");
                    return;
                }

                JSONObject data = jsonResponse.getJSONObject("data");
                JSONObject attributes = data.optJSONObject("attributes");

                if (attributes == null) {
                    System.out.println("Ошибка: `attributes` отсутствует!");
                    return;
                }

                JSONObject details = attributes.optJSONObject("details");

                if (details != null) {
                    JSONObject rustMaps = details.optJSONObject("rust_maps");
                    if(rustMaps != null){
                        url = rustMaps.optString("url", "");
                    }
                }

                String serverName = attributes.optString("name", "Неизвестный сервер");
                boolean isOnline = attributes.optString("status", "offline").equals("online");
                int players = attributes.optInt("players", 0);
                String connectLink = attributes.optString("ip", "Нет данных") + ":" + attributes.optInt("port", 0);

                // Поиск очереди
                int queuedPlayers = 0;


                JSONArray tags = (details != null && details.has("tags")) ? details.optJSONArray("tags") : new JSONArray();

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

                int latsQueued = queuedPlayers;

                javafx.application.Platform.runLater(() -> {
                    online.setText("Онлайн: " + (isOnline ? "Да" : "Нет"));
                    player.setText("Игроков: " + players);
                    uptime.setText("Сервер: " + serverName);
                    connect.setText("Connect: " + connectLink);
                    qule.setText("Очередь: " + latsQueued); //TEST GIT dddd

                    if (!url.isEmpty()) {
                        loadMapInWebView(url);
                    } else {
                        System.out.println("Карта недоступна!");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    online.setText("Ошибка!");
                });
            }
        }).start();
    }
}
