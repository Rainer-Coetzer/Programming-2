import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class WeatherApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherGUI().createAndShowGUI());
    }
}

class WeatherGUI {
    private JFrame frame;
    private JTextArea outputArea;
    private JTextField cityField;
    private JPopupMenu suggestionMenu;

    public void createAndShowGUI() {
        frame = new JFrame("Java Weather App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel();

        JLabel cityLabel = new JLabel("Enter City:");
        cityField = new JTextField("Windhoek", 15); // Default city
        suggestionMenu = new JPopupMenu();
        JButton fetchButton = new JButton("Get Weather");

        cityField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                showSuggestions();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                showSuggestions();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                showSuggestions();
            }
        });

        inputPanel.add(cityLabel);
        inputPanel.add(cityField);
        inputPanel.add(fetchButton);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel);

        fetchButton.addActionListener(e -> {
            String city = cityField.getText().trim();
            double[] coords = WeatherFetcher.getCoordinates(city);
            if (coords != null) {
                String json = WeatherFetcher.getWeather(coords[0], coords[1]);
                if (json != null) {
                    WeatherProcessor.processData(json, outputArea);
                } else {
                    outputArea.setText("Failed to fetch weather data.");
                }
            } else {
                outputArea.setText("Invalid location or API limit reached.");
            }
        });

        frame.setVisible(true);
    }

    private void showSuggestions() {
        String input = cityField.getText().trim();
        if (input.length() < 2) return;

        ArrayList<String> suggestions = WeatherFetcher.getSuggestions(input);
        suggestionMenu.removeAll();

        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(e -> cityField.setText(suggestion));
            suggestionMenu.add(item);
        }

        if (!suggestions.isEmpty()) {
            suggestionMenu.show(cityField, 0, cityField.getHeight());
        } else {
            suggestionMenu.setVisible(false);
        }
    }
}


class WeatherFetcher {
    public static ArrayList<String> getSuggestions(String input) {
        ArrayList<String> list = new ArrayList<>();
        try {
            String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(input, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String[] parts = response.toString().split("\"name\":\"");
            for (int i = 1; i < parts.length && i <= 5; i++) {
                String suggestion = parts[i].split("\"")[0];
                list.add(suggestion);
            }
        } catch (Exception e) {
            ErrorHandler.logError("Autocomplete error: " + e.getMessage());
        }
        return list;
    }

    public static double[] getCoordinates(String city) {
        try {
            String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(city, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String block = response.toString().split("\"name\":\"")[1];
            String lat = block.split("\"latitude\":")[1].split(",")[0];
            String lon = block.split("\"longitude\":")[1].split(",")[0];

            return new double[]{Double.parseDouble(lat), Double.parseDouble(lon)};

        } catch (Exception e) {
            ErrorHandler.logError("Geocoding Error: " + e.getMessage());
            return null;
        }
    }

    public static String getWeather(double latitude, double longitude) {
        try {
            String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                    "&longitude=" + longitude + "&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            return response.toString();

        } catch (Exception e) {
            ErrorHandler.logError("API Fetch Error: " + e.getMessage());
            return null;
        }
    }
}


class WeatherProcessor {
    public static void processData(String jsonData, JTextArea outputArea) {
        try {
            String current = jsonData.split("\"current_weather\":\\{")[1].split("}")[0];
            double temp = Double.parseDouble(current.split("\"temperature\":")[1].split(",")[0]);
            double wind = Double.parseDouble(current.split("\"windspeed\":")[1].split(",")[0]);
            String time = current.split("\"time\":\"")[1].split("\"")[0];

            StringBuilder output = new StringBuilder("Current Weather:\n");
            output.append("Temperature: ").append(temp).append("°C\n");
            output.append("Wind Speed: ").append(wind).append(" km/h\n");
            output.append("Time: ").append(time).append("\n\n5-Day Forecast:\n");

            String dailyBlock = jsonData.split("\"daily\":\\{\"time\":\\[")[1];
            String[] dates = dailyBlock.split("\\]")[0].replace("\"", "").split(",");
            String[] tempsMax = jsonData.split("\"temperature_2m_max\":\\[")[1].split("\\]")[0].split(",");
            String[] tempsMin = jsonData.split("\"temperature_2m_min\":\\[")[1].split("\\]")[0].split(",");

            for (int i = 0; i < dates.length; i++) {
                output.append(dates[i]).append(": Max ").append(tempsMax[i]).append("°C, Min ").append(tempsMin[i]).append("°C\n");
            }

            outputArea.setText(output.toString());
            WeatherStorage.saveWeatherData(output.toString());

        } catch (Exception e) {
            outputArea.setText("Error processing weather data.");
            ErrorHandler.logError("Manual JSON parse failed: " + e.getMessage());
        }
    }
}


class WeatherStorage {
    private static final String FILE_NAME = "weather_log.txt";

    public static void saveWeatherData(String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            writer.write(data + "\n----------------\n");
        } catch (IOException e) {
            ErrorHandler.logError("File Write Error: " + e.getMessage());
        }
    }
}


class ErrorHandler {
    private static final String LOG_FILE = "error_log.txt";

    public static void logError(String errorMsg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write("ERROR: " + errorMsg + "\n");
        } catch (IOException ignored) {
        }
    }
}


class AppUtils {
    public static boolean isValidCity(String city) {
        return city != null && !city.isBlank();
    }
}
