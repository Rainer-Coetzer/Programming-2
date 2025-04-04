import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

// Main Class
public class WeatherApp {
    public static void main(String[] args) {
        new WeatherGUI(); // Starts GUI
    }
}

// Jesaya Barnabas - GUI Class
class WeatherGUI {
    private JFrame frame;
    private JTextField cityInput;
    private JTextArea weatherOutput;

    public WeatherGUI() {
        frame = new JFrame("Weather App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        cityInput = new JTextField();
        JButton searchButton = new JButton("Get Weather");
        weatherOutput = new JTextArea();
        weatherOutput.setEditable(false);

        panel.add(cityInput, BorderLayout.NORTH);
        panel.add(searchButton, BorderLayout.CENTER);
        panel.add(new JScrollPane(weatherOutput), BorderLayout.SOUTH);

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String city = cityInput.getText();
                String data = WeatherFetcher.getWeather(city);
                if (data != null) {
                    WeatherProcessor.processData(data, weatherOutput);
                } else {
                    weatherOutput.setText("Error fetching weather.");
                }
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }
}

// Tadeus Kalola - Fetching Weather Data
class WeatherFetcher {
    private static final String API_KEY = "your_api_key_here"; // Replace with a valid API key

    public static String getWeather(String city) {
        try {
            String urlString = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
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
        } catch (Exception e) {
            ErrorHandler.logError("API Fetch Error: " + e.getMessage());
            return null;
        }
    }
}

// Rainer Coetze - Processing Weather Data
class WeatherProcessor {
    public static void processData(String jsonData, JTextArea outputArea) {
        try {
            JSONObject json = new JSONObject(jsonData);
            String cityName = json.getString("name");
            JSONObject main = json.getJSONObject("main");
            double temp = main.getDouble("temp") - 273.15; // Convert Kelvin to Celsius
            int humidity = main.getInt("humidity");

            String weatherInfo = "City: " + cityName + "\nTemperature: " + String.format("%.2f", temp) + "°C\nHumidity: " + humidity + "%";
            outputArea.setText(weatherInfo);
            WeatherStorage.saveWeatherData(weatherInfo);
        } catch (Exception e) {
            ErrorHandler.logError("Data Processing Error: " + e.getMessage());
        }
    }
}

// Rainer Coetzer - Storing Weather Data
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

// Delicia Damases - Error Handling
class ErrorHandler {
    private static final String ERROR_LOG = "error_log.txt";

    public static void logError(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ERROR_LOG, true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.err.println("Critical Error: Unable to write to error log.");
        }
    }
}

// Tuyeimo Nangombe - Application Logic & User Interaction
class WeatherAppLogic {
    public static void start() {
        System.out.println("Welcome to the Weather App! GUI will start now...");
        new WeatherGUI();
    }
}
