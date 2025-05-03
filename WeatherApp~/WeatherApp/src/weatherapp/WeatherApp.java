/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package weatherapp;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class WeatherApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new WeatherGUI().createAndShowGUI();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

abstract class WeatherFrame extends JFrame {
    public abstract void initializeComponents();
}

class WeatherGUI extends WeatherFrame {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JTextField cityInput;
    private JPopupMenu suggestionMenu;
    private JTextArea currentWeatherDisplay;
    private JTextArea forecastDisplay;
    private JComboBox<String> unitComboBox;
    private WeatherDatabase db;
    private WeatherUnitStrategy unitStrategy;

    public WeatherGUI() {
        try {
            this.db = new WeatherDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database initialization failed. History features will be disabled.", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            this.db = null;
        }
        this.unitStrategy = new CelsiusStrategy();
    }

    @Override
    public void initializeComponents() {
        frame = new JFrame("Weather Forecast App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.setLayout(new BorderLayout());
    }

    public void createAndShowGUI() {
        initializeComponents();
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel inputPanel = createInputPanel();
        frame.add(inputPanel, BorderLayout.NORTH);

        createCurrentWeatherTab();
        createForecastTab();
        createSettingsTab();
        createHistoryTab();

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel cityLabel = new JLabel("Enter City:");
        cityLabel.setFont(new Font("Arial", Font.BOLD, 14));

        cityInput = new JTextField(20);
        cityInput.setFont(new Font("Arial", Font.PLAIN, 14));
        cityInput.setText("New York");

        suggestionMenu = new JPopupMenu();
        cityInput.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        JButton fetchButton = new JButton("Get Weather");
        fetchButton.setFont(new Font("Arial", Font.BOLD, 14));
        fetchButton.setBackground(new Color(70, 130, 180));
        fetchButton.setForeground(Color.WHITE);
        fetchButton.addActionListener(e -> fetchWeather());

        panel.add(cityLabel);
        panel.add(cityInput);
        panel.add(fetchButton);

        return panel;
    }

    private void createCurrentWeatherTab() {
        JPanel currentWeatherPanel = new JPanel(new BorderLayout());
        currentWeatherPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        currentWeatherDisplay = new JTextArea();
        currentWeatherDisplay.setEditable(false);
        currentWeatherDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16));
        currentWeatherDisplay.setBackground(new Color(240, 248, 255));
        currentWeatherDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(currentWeatherDisplay);
        currentWeatherPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Current Weather", new ImageIcon("weather.png"), currentWeatherPanel, "View current weather conditions");
    }

    private void createForecastTab() {
        JPanel forecastPanel = new JPanel(new BorderLayout());
        forecastPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        forecastDisplay = new JTextArea();
        forecastDisplay.setEditable(false);
        forecastDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16));
        forecastDisplay.setBackground(new Color(240, 248, 255));
        forecastDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(forecastDisplay);
        forecastPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("5-Day Forecast", new ImageIcon("forecast.png"), forecastPanel, "View 5-day weather forecast");
    }

    private void createSettingsTab() {
        JPanel settingsPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel unitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel unitLabel = new JLabel("Temperature Unit:");
        unitLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        unitComboBox = new JComboBox<>(new String[]{"Celsius (°C)", "Fahrenheit (°F)"});
        unitComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        unitComboBox.addActionListener(e -> {
            if (unitComboBox.getSelectedIndex() == 0) {
                unitStrategy = new CelsiusStrategy();
            } else {
                unitStrategy = new FahrenheitStrategy();
            }
        });
        
        unitPanel.add(unitLabel);
        unitPanel.add(unitComboBox);
        settingsPanel.add(unitPanel);

        settingsPanel.add(new JLabel());
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel infoLabel = new JLabel("Weather App v1.6 - © 2025");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        infoPanel.add(infoLabel);
        settingsPanel.add(infoPanel);

        tabbedPane.addTab("Settings", new ImageIcon("settings.png"), settingsPanel, "Configure application settings");
    }

    private void createHistoryTab() {
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea historyDisplay = new JTextArea();
        historyDisplay.setEditable(false);
        historyDisplay.setFont(new Font("Monospaced", Font.PLAIN, 14));
        historyDisplay.setBackground(new Color(240, 248, 255));
        historyDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton refreshButton = new JButton("Refresh History");
        refreshButton.addActionListener(e -> {
            try {
                if (db != null) {
                    List<WeatherHistory> history = db.getWeatherHistory();
                    StringBuilder sb = new StringBuilder("=== SEARCH HISTORY ===\n\n");
                    sb.append(String.format("%-20s %-25s %-15s\n", "City", "Date", "Temperature"));
                    sb.append("------------------------------------------------------------\n");
                    
                    for (WeatherHistory item : history) {
                        sb.append(String.format("%-20s %-25s %-15.1f°C\n", 
                            item.getCity(), item.getSearchDate(), item.getTemperature()));
                    }
                    
                    historyDisplay.setText(sb.toString());
                } else {
                    historyDisplay.setText("Database connection unavailable\nHistory features disabled");
                }
            } catch (SQLException ex) {
                historyDisplay.setText("Error loading history: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);

        historyPanel.add(buttonPanel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyDisplay), BorderLayout.CENTER);

        tabbedPane.addTab("History", new ImageIcon("history.png"), historyPanel, "View search history");
    }

    private void updateSuggestions() {
        String input = cityInput.getText().trim();
        if (input.length() < 2) {
            suggestionMenu.setVisible(false);
            return;
        }

        List<String> suggestions = WeatherFetcher.getCitySuggestions(input);
        suggestionMenu.removeAll();

        if (suggestions.isEmpty()) {
            suggestionMenu.setVisible(false);
            return;
        }

        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(e -> {
                cityInput.setText(suggestion);
                suggestionMenu.setVisible(false);
            });
            suggestionMenu.add(item);
        }

        suggestionMenu.show(cityInput, 0, cityInput.getHeight());
    }

    private void fetchWeather() {
        String city = cityInput.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a city name", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            WeatherData data = WeatherFetcher.getWeatherData(city);
            
            double convertedTemp = unitStrategy.convertTemperature(data.getCurrentTemp());
            data.setCurrentTemp(convertedTemp);
            
            for (DailyForecast day : data.getForecast()) {
                day.setMaxTemp(unitStrategy.convertTemperature(day.getMaxTemp()));
                day.setMinTemp(unitStrategy.convertTemperature(day.getMinTemp()));
            }
            
            displayWeather(data);
            
            if (db != null) {
                try {
                    db.saveWeatherSearch(city, data.getCurrentTemp());
                } catch (SQLException e) {
                    System.err.println("Failed to save to database: " + e.getMessage());
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error fetching weather: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayWeather(WeatherData data) {
        StringBuilder current = new StringBuilder();
        current.append("=== CURRENT WEATHER ===\n\n");
        current.append(String.format("Location: %s\n", cityInput.getText()));
        current.append(String.format("Time: %s\n", data.getTime()));
        current.append(String.format("Temperature: %.1f%s\n", 
            data.getCurrentTemp(), unitStrategy.getUnitSymbol()));
        current.append(String.format("Wind Speed: %.1f km/h\n", data.getWindSpeed()));
        currentWeatherDisplay.setText(current.toString());

        StringBuilder forecast = new StringBuilder();
        forecast.append("=== 5-DAY FORECAST ===\n\n");
        forecast.append(String.format("%-15s %-10s %-10s\n", "Date", "High", "Low"));
        forecast.append("--------------------------------\n");
        
        for (DailyForecast day : data.getForecast()) {
            forecast.append(String.format("%-15s %-10.1f%s %-10.1f%s\n", 
                day.getDate(), 
                day.getMaxTemp(), unitStrategy.getUnitSymbol(), 
                day.getMinTemp(), unitStrategy.getUnitSymbol()));
        }
        
        forecastDisplay.setText(forecast.toString());
    }
}

class WeatherFetcher {
    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    public static WeatherData getWeatherData(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            throw new Exception("Could not find coordinates for: " + city);
        }

        String weatherJson = getWeatherJson(coordinates[0], coordinates[1]);
        return parseWeatherData(weatherJson);
    }

    public static List<String> getCitySuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        if (input == null || input.length() < 2) {
            return suggestions;
        }

        try {
            String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(input, "UTF-8");
            String response = makeApiRequest(urlString);

            String[] parts = response.split("\"name\":\"");
            for (int i = 1; i < parts.length && i <= 5; i++) {
                String suggestion = parts[i].split("\"")[0];
                suggestions.add(suggestion);
            }
        } catch (Exception e) {
            System.err.println("Autocomplete error: " + e.getMessage());
        }

        return suggestions;
    }

    private static double[] getCoordinates(String city) throws Exception {
        String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(city, "UTF-8");
        String response = makeApiRequest(urlString);

        String[] parts = response.split("\"results\":\\[\\{");
        if (parts.length < 2) return null;

        String block = parts[1].split("\\}")[0];
        String lat = block.split("\"latitude\":")[1].split(",")[0];
        String lon = block.split("\"longitude\":")[1].split(",")[0];

        return new double[]{Double.parseDouble(lat), Double.parseDouble(lon)};
    }

    private static String getWeatherJson(double latitude, double longitude) throws Exception {
        String urlString = WEATHER_API + "?latitude=" + latitude +
                "&longitude=" + longitude + 
                "&current_weather=true" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto";

        return makeApiRequest(urlString);
    }

    private static String makeApiRequest(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (IOException e) {
            throw new Exception("Failed to connect to weather service: " + e.getMessage());
        }
    }

    private static WeatherData parseWeatherData(String jsonData) throws Exception {
        WeatherData data = new WeatherData();

        try {
            String currentBlock = jsonData.split("\"current_weather\":\\{")[1].split("\\}")[0];
            data.setCurrentTemp(Double.parseDouble(currentBlock.split("\"temperature\":")[1].split(",")[0]));
            data.setWindSpeed(Double.parseDouble(currentBlock.split("\"windspeed\":")[1].split(",")[0]));
            data.setTime(currentBlock.split("\"time\":\"")[1].split("\"")[0]);

            String dailyBlock = jsonData.split("\"daily\":\\{")[1].split("\\}\\}")[0];
            String[] dates = dailyBlock.split("\"time\":\\[")[1].split("\\]")[0].replace("\"", "").split(",");
            String[] maxTemps = dailyBlock.split("\"temperature_2m_max\":\\[")[1].split("\\]")[0].split(",");
            String[] minTemps = dailyBlock.split("\"temperature_2m_min\":\\[")[1].split("\\]")[0].split(",");

            List<DailyForecast> forecastList = new ArrayList<>();
            for (int i = 0; i < dates.length && i < 5; i++) {
                DailyForecast day = new DailyForecast();
                day.setDate(dates[i]);
                day.setMaxTemp(Double.parseDouble(maxTemps[i]));
                day.setMinTemp(Double.parseDouble(minTemps[i]));
                forecastList.add(day);
            }
            data.setForecast(forecastList);
        } catch (Exception e) {
            throw new Exception("Failed to parse weather data: " + e.getMessage());
        }

        return data;
    }
}

class WeatherData {
    private double currentTemp;
    private double windSpeed;
    private String time;
    private List<DailyForecast> forecast;

    public double getCurrentTemp() { return currentTemp; }
    public void setCurrentTemp(double currentTemp) { this.currentTemp = currentTemp; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }
}

class DailyForecast {
    private String date;
    private double maxTemp;
    private double minTemp;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getMaxTemp() { return maxTemp; }
    public void setMaxTemp(double maxTemp) { this.maxTemp = maxTemp; }
    public double getMinTemp() { return minTemp; }
    public void setMinTemp(double minTemp) { this.minTemp = minTemp; }
}

class WeatherDatabase {
    private static final String DB_URL = "jdbc:sqlite:weather.db";
    private Connection conn;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            e.printStackTrace();
        }
    }

    public WeatherDatabase() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS weather_history (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "city TEXT NOT NULL," +
                     "temperature REAL NOT NULL," +
                     "search_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void saveWeatherSearch(String city, double temperature) throws SQLException {
        String sql = "INSERT INTO weather_history(city, temperature) VALUES(?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, city);
            pstmt.setDouble(2, temperature);
            pstmt.executeUpdate();
        }
    }

    public List<WeatherHistory> getWeatherHistory() throws SQLException {
        List<WeatherHistory> history = new ArrayList<>();
        String sql = "SELECT city, temperature, search_date FROM weather_history ORDER BY search_date DESC LIMIT 50";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                WeatherHistory item = new WeatherHistory(
                    rs.getString("city"),
                    rs.getDouble("temperature"),
                    rs.getString("search_date")
                );
                history.add(item);
            }
        }
        
        return history;
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}

class WeatherHistory {
    private String city;
    private double temperature;
    private String searchDate;

    public WeatherHistory(String city, double temperature, String searchDate) {
        this.city = city;
        this.temperature = temperature;
        this.searchDate = searchDate;
    }

    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public String getSearchDate() { return searchDate; }
}

interface WeatherUnitStrategy {
    double convertTemperature(double celsius);
    String getUnitSymbol();
}

class CelsiusStrategy implements WeatherUnitStrategy {
    @Override
    public double convertTemperature(double celsius) {
        return celsius;
    }

    @Override
    public String getUnitSymbol() {
        return "°C";
    }
}

class FahrenheitStrategy implements WeatherUnitStrategy {
    @Override
    public double convertTemperature(double celsius) {
        return celsius * 9/5 + 32;
    }

    @Override
    public String getUnitSymbol() {
        return "°F";
    }
}