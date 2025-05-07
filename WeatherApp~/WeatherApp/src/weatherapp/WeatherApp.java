package weatherapp; // Defines the package for the weather application classes

// Import necessary Java Swing components for the GUI
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
// Import necessary AWT components for GUI layout and events
import java.awt.*;
import java.awt.event.*;
// Import necessary I/O classes for network communication
import java.io.*;
// Import necessary networking classes
import java.net.*;
// Import necessary SQL classes for database interaction
import java.sql.*;
// Import utility classes
import java.util.*;
import java.util.List; // Explicit import for List
import java.nio.charset.StandardCharsets; // Added for specifying UTF-8

/**
 * Main class to launch the Weather Application.
 */
public class WeatherApp {
    /**
     * The main entry point of the application.
     * It schedules the creation and display of the GUI on the Event Dispatch Thread (EDT).
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Ensure GUI updates are done on the EDT for thread safety
        SwingUtilities.invokeLater(() -> {
            try {
                // Create and show the main GUI window
                new WeatherGUI().createAndShowGUI();
            } catch (Exception e) {
                // Show an error message if the application fails to start
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace(); // Print stack trace for debugging
            }
        });
    }
}

/**
 * Abstract base class for weather-related JFrame windows.
 * Defines a contract for initializing components.
 * NOTE: Per lecturer, GUI inheritance like this (extending JFrame) doesn't count for the exercise.
 */
abstract class WeatherFrame extends JFrame {
    /**
     * Abstract method to be implemented by subclasses to initialize their GUI components.
     */
    public abstract void initializeComponents();
}

/**
 * The main GUI class for the Weather Application.
 * Extends WeatherFrame and handles the user interface elements and interactions.
 */
class WeatherGUI extends WeatherFrame {
    // --- GUI Components ---
    private JFrame frame; // The main application window
    private JTabbedPane tabbedPane; // Tabbed interface for different views
    private JTextField cityInput; // Text field for entering the city name
    private JPopupMenu suggestionMenu; // Popup menu for city suggestions
    private JTextArea currentWeatherDisplay; // Text area to display current weather
    private JTextArea forecastDisplay; // Text area to display the weather forecast
    private JComboBox<String> unitComboBox; // Dropdown for selecting temperature units (Celsius/Fahrenheit)

    // --- Data and Logic ---
    private WeatherDatabase db; // Handles database operations for search history
    private WeatherUnitStrategy unitStrategy; // Strategy for temperature unit conversion

    /**
     * Constructor for WeatherGUI.
     * Initializes the database connection and sets the default unit strategy.
     */
    public WeatherGUI() {
        try {
            // Initialize the database connection
            this.db = new WeatherDatabase();
        } catch (SQLException e) {
            // Show a warning if database initialization fails
            JOptionPane.showMessageDialog(null, "Database initialization failed. History features will be disabled.\n" + e.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
            this.db = null; // Disable database features if connection failed
            e.printStackTrace();
        }
        // Set the default temperature unit strategy to Celsius
        this.unitStrategy = new CelsiusStrategy();
    }

    /**
     * Initializes the main JFrame components (window properties).
     * Overrides the abstract method from WeatherFrame.
     */
    @Override
    public void initializeComponents() {
        frame = new JFrame("Weather Forecast App"); // Set window title
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation
        frame.setSize(700, 600); // Set window size
        frame.setLayout(new BorderLayout()); // Use BorderLayout for the main frame
    }

    /**
     * Creates and displays the main GUI structure, including panels and tabs.
     */
    public void createAndShowGUI() {
        initializeComponents(); // Initialize the frame

        // Create the tabbed pane for different sections
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14)); // Set font for tabs

        // Create the input panel (city search) and add it to the top
        JPanel inputPanel = createInputPanel();
        frame.add(inputPanel, BorderLayout.NORTH);

        // Create the different tabs
        createCurrentWeatherTab();
        createForecastTab();
        createSettingsTab();
        createHistoryTab();

        // Add the tabbed pane to the center of the frame
        frame.add(tabbedPane, BorderLayout.CENTER);

        // Add a window listener to close the database connection when the GUI is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (db != null) {
                    db.close();
                }
            }
        });

        // Make the frame visible
        frame.setVisible(true);
    }

    /**
     * Creates the input panel containing the city label, text field, and fetch button.
     * @return The configured JPanel for input.
     */
    private JPanel createInputPanel() {
        // Use FlowLayout centered for the input panel
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(new Color(240, 240, 240)); // Set background color
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // Label for the city input field
        JLabel cityLabel = new JLabel("Enter City:");
        cityLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Text field for city input
        cityInput = new JTextField(20); // Set preferred width
        cityInput.setFont(new Font("Arial", Font.PLAIN, 14));
        cityInput.setText("Windhoek"); // Set default city

        // Popup menu for city suggestions
        suggestionMenu = new JPopupMenu();
        // Add a listener to the text field to update suggestions as the user types
        cityInput.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) {} // Not needed for plain text fields
        });

        // Button to trigger fetching weather data
        JButton fetchButton = new JButton("Get Weather");
        fetchButton.setFont(new Font("Arial", Font.BOLD, 14));
        fetchButton.setBackground(new Color(70, 130, 180)); // Set button color
        fetchButton.setForeground(Color.WHITE); // Set text color
        // Add action listener to call fetchWeather method on click
        fetchButton.addActionListener(e -> fetchWeather());

        // Add components to the panel
        panel.add(cityLabel);
        panel.add(cityInput);
        panel.add(fetchButton);

        return panel;
    }

    /**
     * Creates the "Current Weather" tab with a text area for display.
     */
    private void createCurrentWeatherTab() {
        // Panel for the current weather tab, using BorderLayout
        JPanel currentWeatherPanel = new JPanel(new BorderLayout());
        currentWeatherPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // Text area to display current weather details
        currentWeatherDisplay = new JTextArea();
        currentWeatherDisplay.setEditable(false); // Make it read-only
        currentWeatherDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16)); // Use monospaced font for alignment
        currentWeatherDisplay.setBackground(new Color(240, 248, 255)); // Set background color
        currentWeatherDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add internal padding

        // Add the text area to a scroll pane in case content is large
        JScrollPane scrollPane = new JScrollPane(currentWeatherDisplay);
        currentWeatherPanel.add(scrollPane, BorderLayout.CENTER); // Add scroll pane to the panel

        // Add the panel as a tab to the tabbed pane
        tabbedPane.addTab("Current Weather", null, currentWeatherPanel, "View current weather conditions");
    }

    /**
     * Creates the "5-Day Forecast" tab with a text area for display.
     */
    private void createForecastTab() {
        // Panel for the forecast tab, using BorderLayout
        JPanel forecastPanel = new JPanel(new BorderLayout());
        forecastPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // Text area to display forecast details
        forecastDisplay = new JTextArea();
        forecastDisplay.setEditable(false); // Make it read-only
        forecastDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16)); // Use monospaced font
        forecastDisplay.setBackground(new Color(240, 248, 255)); // Set background color
        forecastDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add internal padding

        // Add the text area to a scroll pane
        JScrollPane scrollPane = new JScrollPane(forecastDisplay);
        forecastPanel.add(scrollPane, BorderLayout.CENTER); // Add scroll pane to the panel

        // Add the panel as a tab to the tabbed pane
        tabbedPane.addTab("5-Day Forecast", null, forecastPanel, "View 5-day weather forecast");
    }

    /**
     * Creates the "Settings" tab with options like temperature unit selection.
     */
    private void createSettingsTab() {
        // Panel for settings, using GridLayout for vertical arrangement
        JPanel settingsPanel = new JPanel(new GridLayout(0, 1, 10, 10)); // 0 rows = any number, 1 column
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding

        // Panel for the unit selection controls
        JPanel unitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Align components to the left
        JLabel unitLabel = new JLabel("Temperature Unit:");
        unitLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // ComboBox for selecting Celsius or Fahrenheit
        unitComboBox = new JComboBox<>(new String[]{"Celsius (°C)", "Fahrenheit (°F)"});
        unitComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        // Add action listener to change the unit strategy when selection changes
        unitComboBox.addActionListener(e -> {
            if (unitComboBox.getSelectedIndex() == 0) { // Celsius selected
                unitStrategy = new CelsiusStrategy();
            } else { // Fahrenheit selected
                unitStrategy = new FahrenheitStrategy();
            }
            // If weather data is already displayed, re-fetch or re-display with new units
            // This example doesn't automatically refresh, user needs to fetch again to see current/forecast in new unit.
            // History tab will use the new unit upon its next refresh.
        });

        // Add label and combo box to the unit panel
        unitPanel.add(unitLabel);
        unitPanel.add(unitComboBox);
        // Add the unit panel to the main settings panel
        settingsPanel.add(unitPanel);

        // Add an empty label for spacing (GridLayout specific)
        settingsPanel.add(new JLabel());

        // Panel for displaying application info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Center the info text
        JLabel infoLabel = new JLabel("Weather App v1.8 (Improved Parsing & History Fix) - © 2025"); // Version info
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        infoPanel.add(infoLabel);
        // Add the info panel to the settings panel
        settingsPanel.add(infoPanel);

        // Add the settings panel as a tab
        tabbedPane.addTab("Settings", null, settingsPanel, "Configure application settings");
    }

    /**
     * Creates the "History" tab to display past weather searches from the database.
     * Temperatures in history are displayed based on the current unit setting.
     */
    private void createHistoryTab() {
        // Panel for the history tab, using BorderLayout
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // Text area to display search history
        JTextArea historyDisplay = new JTextArea();
        historyDisplay.setEditable(false); // Read-only
        historyDisplay.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Monospaced for table-like format
        historyDisplay.setBackground(new Color(240, 248, 255)); // Background color
        historyDisplay.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Internal padding

        // Button to refresh the history display
        JButton refreshButton = new JButton("Refresh History");
        // Add action listener to load and display history from the database
        refreshButton.addActionListener(e -> {
            try {
                if (db != null) { // Check if database connection is available
                    // Fetch history records from the database
                    List<WeatherHistory> history = db.getWeatherHistory();
                    // Use StringBuilder for efficient string construction
                    StringBuilder sb = new StringBuilder("=== SEARCH HISTORY ===\n\n");
                    
                    // Header uses "Details" for the last column now
                    sb.append(String.format("%-20s %-25s %-15s %-30s\n", "City", "Date", "Temperature", "Details"));
                    sb.append("-----------------------------------------------------------------------------------\n"); // 83 hyphens

                    // Format and append each history item
                    for (WeatherHistory item : history) {
                        double storedCelsiusTemp = item.getTemperature(); // Temperature stored in DB is Celsius
                        double displayTemp = unitStrategy.convertTemperature(storedCelsiusTemp); // Convert to current display unit
                        String unitSymbol = unitStrategy.getUnitSymbol(); // Get current unit symbol

                        // Format the temperature with the current unit for display
                        String tempWithUnit = String.format("%.1f%s", displayTemp, unitSymbol);
                        
                        // item.getSummary() now returns "Original: XX.X°C"
                        sb.append(String.format("%-20s %-25s %-15s %-30s\n",
                                item.getCity(),
                                item.getTimestamp(), // Using getTimestamp from DatedDataEntry
                                tempWithUnit,        // Displayed temperature in current unit
                                item.getSummary())); // Details including original Celsius temp
                    }
                    historyDisplay.setText(sb.toString());
                    historyDisplay.setCaretPosition(0); // Scroll to top
                } else {
                    historyDisplay.setText("Database connection unavailable.\nHistory features disabled.");
                }
            } catch (SQLException ex) {
                historyDisplay.setText("Error loading history: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Panel to hold the refresh button (using FlowLayout by default)
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);

        // Add the button panel to the top and the history display (in a scroll pane) to the center
        historyPanel.add(buttonPanel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyDisplay), BorderLayout.CENTER);

        // Add the history panel as a tab
        tabbedPane.addTab("History", null, historyPanel, "View search history");
    }

    /**
     * Updates the city suggestion popup menu based on the current input in the city text field.
     */
    private void updateSuggestions() {
        String input = cityInput.getText().trim(); // Get trimmed input
        if (input.length() < 2) {
            suggestionMenu.setVisible(false); // Hide menu if input is too short
            return;
        }

        // Fetch city suggestions (this could be run in a background thread for responsiveness)
        List<String> suggestions = WeatherFetcher.getCitySuggestions(input);
        suggestionMenu.removeAll(); // Clear previous suggestions

        if (suggestions.isEmpty()) {
            suggestionMenu.setVisible(false);
            return;
        }

        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(e -> {
                cityInput.setText(suggestion); // Set text field to the selected suggestion
                suggestionMenu.setVisible(false); // Hide the menu
            });
            suggestionMenu.add(item); // Add the menu item
        }

        if (cityInput.isShowing() && cityInput.hasFocus()) { // Show only if input field is visible and has focus
             suggestionMenu.show(cityInput, 0, cityInput.getHeight());
             // cityInput.requestFocusInWindow(); // Avoid stealing focus back if menu is just updating
        } else {
             suggestionMenu.setVisible(false);
        }
    }


    /**
     * Fetches weather data for the city entered in the input field,
     * converts temperatures based on the selected unit, displays the data,
     * and saves the search to the history database (always in Celsius).
     */
    private void fetchWeather() {
        String city = cityInput.getText().trim(); // Get city name from input field
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a city name",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Placeholder for loading indicator (e.g., change cursor, show message)
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        currentWeatherDisplay.setText("Fetching weather data for " + city + "...");
        forecastDisplay.setText("Fetching forecast data...");

        // Perform fetching in a background thread to keep UI responsive
        SwingWorker<WeatherData, Void> worker = new SwingWorker<WeatherData, Void>() {
            private Exception fetchException = null;

            @Override
            protected WeatherData doInBackground() throws Exception {
                try {
                    return WeatherFetcher.getWeatherData(city);
                } catch (Exception e) {
                    fetchException = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                frame.setCursor(Cursor.getDefaultCursor()); // Reset cursor
                try {
                    WeatherData rawData = get(); // Get result from doInBackground
                    if (fetchException != null) {
                        throw fetchException; // Re-throw exception caught in background
                    }
                    if (rawData == null) { // Should be handled by fetchException, but as a safeguard
                        throw new Exception("Unknown error fetching data.");
                    }

                    // --- Temperature Conversion using Strategy Pattern ---
                    double originalCurrentTempCelsius = rawData.getCurrentTemp(); // This is from API, assumed Celsius
                    WeatherData displayData = new WeatherData(); // Create a new instance for display
                    displayData.setTime(rawData.getTime());
                    displayData.setWindSpeed(rawData.getWindSpeed());
                    // Convert current temperature for display using the selected unit strategy
                    displayData.setCurrentTemp(unitStrategy.convertTemperature(originalCurrentTempCelsius));

                    List<DailyForecast> displayForecasts = new ArrayList<>();
                    if (rawData.getForecast() != null) { // Check if forecast data is available
                        for (DailyForecast day : rawData.getForecast()) {
                            DailyForecast displayDay = new DailyForecast();
                            displayDay.setDate(day.getDate());
                            // Convert forecast temperatures for display
                            displayDay.setMaxTemp(unitStrategy.convertTemperature(day.getMaxTemp())); // Max temp from API (Celsius)
                            displayDay.setMinTemp(unitStrategy.convertTemperature(day.getMinTemp())); // Min temp from API (Celsius)
                            displayDay.setSummary(day.getSummary()); // Summary from API
                            displayForecasts.add(displayDay);
                        }
                    }
                    displayData.setForecast(displayForecasts);
                    // --- End Conversion ---

                    displayWeather(displayData, city); // Pass city for display

                    // Save the search to the database (if available) using the original Celsius temp
                    if (db != null) {
                        try {
                            // Always save the original Celsius temperature to the database
                            db.saveWeatherSearch(city, originalCurrentTempCelsius, rawData.getTime());
                        } catch (SQLException e) {
                            System.err.println("Failed to save search to database: " + e.getMessage());
                            // Optionally show a non-blocking warning
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error fetching weather data for '" + city + "':\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    currentWeatherDisplay.setText("Failed to load weather data.");
                    forecastDisplay.setText("Failed to load forecast data.");
                    ex.printStackTrace();
                }
            }
        };
        worker.execute(); // Start the SwingWorker
    }

    /**
     * Displays the fetched weather data in the current weather and forecast text areas.
     * @param data The WeatherData object containing information to display (with converted temperatures).
     * @param cityName The name of the city for which data is displayed.
     */
    private void displayWeather(WeatherData data, String cityName) {
        // --- Display Current Weather ---
        StringBuilder current = new StringBuilder();
        current.append("=== CURRENT WEATHER ===\n\n");
        current.append(String.format("Location:    %s\n", cityName));
        current.append(String.format("Time:        %s\n", data.getTimestamp())); // Using getTimestamp
        current.append(String.format("Temperature: %.1f%s\n",
                data.getCurrentTemp(), unitStrategy.getUnitSymbol())); // Uses converted temp and current unit
        current.append(String.format("Wind Speed:  %.1f km/h\n", data.getWindSpeed()));

        currentWeatherDisplay.setText(current.toString());
        currentWeatherDisplay.setCaretPosition(0);

        // --- Display Forecast ---
        StringBuilder forecastText = new StringBuilder();
        forecastText.append("=== 5-DAY FORECAST ===\n\n");
        if (data.getForecast() != null && !data.getForecast().isEmpty()) {
            forecastText.append(String.format("%-15s %-10s %-10s %-20s\n", "Date", "High", "Low", "Summary"));
            forecastText.append("----------------------------------------------------------\n");

            for (DailyForecast day : data.getForecast()) {
                forecastText.append(String.format("%-15s %-10.1f%s %-10.1f%s %-20s\n",
                        day.getTimestamp(), // Using getTimestamp
                        day.getMaxTemp(), unitStrategy.getUnitSymbol(), // Uses converted temp
                        day.getMinTemp(), unitStrategy.getUnitSymbol(), // Uses converted temp
                        day.getSummary())); // Using getSummary
            }
        } else {
            forecastText.append("Forecast data is currently unavailable for this location.");
        }
        forecastDisplay.setText(forecastText.toString());
        forecastDisplay.setCaretPosition(0);
    }
}

/**
 * Utility class responsible for fetching weather data from external APIs.
 * Uses Open-Meteo APIs for geocoding (city to coordinates) and weather forecasts.
 */
class WeatherFetcher {
    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    public static WeatherData getWeatherData(String city) throws Exception {
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            throw new Exception("Could not find coordinates for city: " + city + ". The geocoding service might be unavailable or the city name is incorrect.");
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
            String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(input, "UTF-8") + "&count=5&format=json";
            String response = makeApiRequest(urlString);

            if (response.contains("\"results\":[")) {
                String resultsArrayString = response.substring(response.indexOf("\"results\":[") + "\"results\":[".length());
                if (resultsArrayString.contains("]")) { // Ensure the array closing bracket exists
                    resultsArrayString = resultsArrayString.substring(0, resultsArrayString.indexOf("]"));
                }


                String[] entries = resultsArrayString.split("\\},\\{");
                for (String entry : entries) {
                    String currentEntry = entry;
                    if (!currentEntry.startsWith("{")) currentEntry = "{" + currentEntry; // Ensure it's a valid object start
                    if (!currentEntry.endsWith("}")) currentEntry = currentEntry + "}"; // Ensure it's a valid object end
                    
                    try {
                        String cityName = parseStringFromJson(currentEntry, "name");
                        String country = "";
                        // Optional: attempt to parse country, admin1 for better suggestions
                        if (currentEntry.contains("\"country\":\"")) {
                             country = ", " + parseStringFromJson(currentEntry, "country");
                        }
                        String admin1 = "";
                         if (currentEntry.contains("\"admin1\":\"")) {
                             String admin1Name = parseStringFromJson(currentEntry, "admin1");
                             if(admin1Name != null && !admin1Name.isEmpty() && !admin1Name.equals(cityName)) { // Avoid redundant info
                                admin1 = ", " + admin1Name;
                             }
                        }
                        String suggestion = cityName + admin1 + country;

                        if (!suggestions.contains(suggestion)) {
                           suggestions.add(suggestion);
                        }
                        if (suggestions.size() >= 5) break;
                    } catch (Exception e) {
                        // Problem parsing this specific suggestion entry, skip it
                        System.err.println("Skipping suggestion entry due to parsing error: " + currentEntry + " | Error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching city suggestions: " + e.getMessage());
        }
        return suggestions;
    }

    private static double[] getCoordinates(String city) throws Exception {
        String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(city, "UTF-8") + "&count=1&format=json";
        String response = makeApiRequest(urlString);

        if (!response.contains("\"results\":[") || response.contains("\"results\":[]")) {
             System.err.println("Geocoding: No results found for city: " + city + ". Response: " + response.substring(0, Math.min(response.length(), 300)));
             return null;
        }
        
        String resultsBlock = response.split("\"results\":\\[")[1].split("\\]")[0]; 
        if (!resultsBlock.trim().startsWith("{")) {
            System.err.println("Geocoding: Malformed results block for city: " + city + ". Block: "+ resultsBlock);
            return null;
        }
         if (!resultsBlock.trim().endsWith("}")) { // Ensure the block is a complete object
            resultsBlock = resultsBlock.substring(0, resultsBlock.lastIndexOf("}") + 1);
        }


        String latStr = extractJsonValue(resultsBlock, "latitude");
        String lonStr = extractJsonValue(resultsBlock, "longitude");


        if (latStr == null || lonStr == null) {
            System.err.println("Failed to parse coordinates from geocoding response for: " + city + ". Block: " + resultsBlock);
            return null;
        }

        try {
            return new double[]{Double.parseDouble(latStr), Double.parseDouble(lonStr)};
        } catch (NumberFormatException e) {
             System.err.println("Failed to parse coordinate numbers: lat='" + latStr + "', lon='" + lonStr + "' for city: " + city);
             return null;
        }
    }

    private static String getWeatherJson(double latitude, double longitude) throws Exception {
        String urlString = WEATHER_API + "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&current_weather=true" + // Request current weather
                "&daily=temperature_2m_max,temperature_2m_min,weathercode" + // Requesting daily data including weathercode
                "&timezone=auto&format=json";
        return makeApiRequest(urlString);
    }

    private static String makeApiRequest(String urlString) throws Exception {
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); 
            conn.setReadTimeout(10000);   
            conn.setRequestProperty("User-Agent", "WeatherApp/1.0"); 

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream(); 
                StringBuilder errorResponse = new StringBuilder();
                if (inputStream != null) {
                    // Use UTF-8 for reading error stream as well
                    reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    if(reader != null) { try {reader.close();} catch (IOException ignored) {} } 
                }
                throw new IOException("HTTP error code: " + responseCode + " from " + urlString +
                                      (errorResponse.length() > 0 ? ". Details: " + errorResponse.toString() : ". No details from error stream."));
            }
            // Specify UTF-8 for reading the successful response
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();

        } catch (MalformedURLException e) {
            throw new Exception("Invalid API URL: " + urlString, e);
        } catch (IOException e) {
            throw new Exception("Network error or API unavailable for " + urlString + ": " + e.getMessage(), e);
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
            if (inputStream != null) try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            if (conn != null) conn.disconnect();
        }
    }

    private static WeatherData parseWeatherData(String jsonData) throws Exception {
        WeatherData data = new WeatherData();
        try {
            // Current Weather Parsing
            if (!jsonData.contains("\"current_weather\":{")) throw new Exception("Missing 'current_weather' block.");
            int cwStartIndex = jsonData.indexOf("\"current_weather\":{") + "\"current_weather\":{".length();
            int cwBraceCount = 0;
            int cwEndIndex = -1;
            for (int i = cwStartIndex; i < jsonData.length(); i++) {
                if (jsonData.charAt(i) == '{') cwBraceCount++;
                else if (jsonData.charAt(i) == '}') {
                    if (cwBraceCount == 0) { cwEndIndex = i; break; }
                    cwBraceCount--;
                }
            }
            if (cwEndIndex == -1) throw new Exception("Malformed 'current_weather' block (no closing brace).");
            String currentBlock = jsonData.substring(cwStartIndex, cwEndIndex);

            data.setCurrentTemp(parseDoubleFromJson(currentBlock, "temperature")); // This is Celsius from API
            data.setWindSpeed(parseDoubleFromJson(currentBlock, "windspeed"));
            data.setTime(parseStringFromJson(currentBlock, "time"));

            // Daily Forecast Parsing
            if (jsonData.contains("\"daily\":{")) {
                int dailyContentStartIndex = jsonData.indexOf("\"daily\":{") + "\"daily\":{".length();
                int braceCount = 0;
                int dailyContentEndIndex = -1;

                for (int i = dailyContentStartIndex; i < jsonData.length(); i++) {
                    if (jsonData.charAt(i) == '{') braceCount++;
                    else if (jsonData.charAt(i) == '}') {
                        if (braceCount == 0) { dailyContentEndIndex = i; break; }
                        braceCount--;
                    }
                }

                if (dailyContentEndIndex == -1) {
                    System.err.println("Warning: Could not find closing brace for 'daily' block.");
                    data.setForecast(new ArrayList<>());
                } else {
                    String dailyBlock = jsonData.substring(dailyContentStartIndex, dailyContentEndIndex);
                    
                    if (dailyBlock.trim().isEmpty()){
                        System.err.println("Warning: 'daily' block is present but empty.");
                        data.setForecast(new ArrayList<>());
                    } else {
                        String[] dates = parseStringArrayFromJson(dailyBlock, "time");
                        double[] maxTemps = parseDoubleArrayFromJson(dailyBlock, "temperature_2m_max"); // Celsius from API
                        double[] minTemps = parseDoubleArrayFromJson(dailyBlock, "temperature_2m_min"); // Celsius from API
                        // Assuming weathercode is an array of integers, parse as string array first then convert or handle as needed.
                        // For simplicity, let's assume parseStringArrayFromJson can get them, and we'll map them.
                        String[] weatherCodesStr = parseStringArrayFromJson(dailyBlock, "weathercode");


                        if (dates.length != maxTemps.length || dates.length != minTemps.length || dates.length != weatherCodesStr.length) {
                             throw new Exception("Forecast array lengths mismatch. Dates: " + dates.length + 
                                                 ", MaxTemps: " + maxTemps.length + ", MinTemps: " + minTemps.length +
                                                 ", WeatherCodes: " + weatherCodesStr.length);
                        }

                        List<DailyForecast> forecastList = new ArrayList<>();
                        int forecastDays = Math.min(dates.length, 7);
                        for (int i = 0; i < forecastDays; i++) {
                            DailyForecast day = new DailyForecast();
                            day.setDate(dates[i]);
                            day.setMaxTemp(maxTemps[i]); // Store as Celsius
                            day.setMinTemp(minTemps[i]); // Store as Celsius
                            try { // Attempt to parse weather code to int
                                day.setSummary(mapWeatherCodeToDescription(Integer.parseInt(weatherCodesStr[i])));
                            } catch (NumberFormatException nfe) {
                                day.setSummary("N/A"); // Fallback if weather code isn't an int
                            }
                            forecastList.add(day);
                        }
                        data.setForecast(forecastList);
                    }
                }
            } else {
                System.err.println("Warning: 'daily' block missing in JSON response.");
                data.setForecast(new ArrayList<>());
            }

        } catch (Exception e) {
            System.err.println("Error parsing weather JSON data: " + e.getMessage());
            System.err.println("JSON Data Snippet (first 1000 chars): " + (jsonData.length() > 1000 ? jsonData.substring(0, 1000) + "..." : jsonData));
            throw new Exception("Failed to parse weather data. Details: " + e.getMessage(), e);
        }
        return data;
    }
    
    private static String extractJsonValue(String jsonBlock, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = jsonBlock.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStartIndex = keyIndex + searchKey.length();
        char firstChar = jsonBlock.charAt(valueStartIndex);

        if (firstChar == '"') { // String value
            int valueEndIndex = jsonBlock.indexOf('"', valueStartIndex + 1);
            if (valueEndIndex == -1) return null; 
            return jsonBlock.substring(valueStartIndex + 1, valueEndIndex);
        } else { // Numeric, boolean, or null value
            int valueEndIndex = valueStartIndex;
            while (valueEndIndex < jsonBlock.length()) {
                char c = jsonBlock.charAt(valueEndIndex);
                if (c == ',' || c == '}' || c == ']') break; 
                valueEndIndex++;
            }
            return jsonBlock.substring(valueStartIndex, valueEndIndex).trim();
        }
    }

    private static double parseDoubleFromJson(String jsonBlock, String key) throws Exception {
        String value = extractJsonValue(jsonBlock, key);
        if (value == null || value.equalsIgnoreCase("null")) {
            throw new Exception("Missing or null numeric value for key: " + key + " in block: " + jsonBlock.substring(0, Math.min(100, jsonBlock.length())));
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number format for key: " + key + ", value: '" + value + "'", e);
        }
    }

    private static String parseStringFromJson(String jsonBlock, String key) throws Exception {
        String value = extractJsonValue(jsonBlock, key);
        if (value == null) {
            throw new Exception("Missing string value for key: " + key + " in block: " + jsonBlock.substring(0, Math.min(100, jsonBlock.length())));
        }
        return value;
    }
    
    private static String[] parseStringArrayFromJson(String jsonBlock, String key) throws Exception {
        String arrayKeySearch = "\"" + key + "\":[";
        int arrayStartIndex = jsonBlock.indexOf(arrayKeySearch);
        if (arrayStartIndex == -1) {
            throw new Exception("Missing array key: " + key + " in block: " + jsonBlock.substring(0, Math.min(jsonBlock.length(),100)) +"...");
        }
        
        int contentStartIndex = arrayStartIndex + arrayKeySearch.length();
        int braceCount = 0; 
        int contentEndIndex = -1;

        for (int i = contentStartIndex; i < jsonBlock.length(); i++) {
            char c = jsonBlock.charAt(i);
            if (c == '[') braceCount++;
            else if (c == ']') {
                if (braceCount == 0) {
                    contentEndIndex = i;
                    break;
                }
                braceCount--;
            }
        }
        if (contentEndIndex == -1) throw new Exception("Malformed array (no closing ']') for key: " + key);
        
        String arrayContent = jsonBlock.substring(contentStartIndex, contentEndIndex);
        if (arrayContent.trim().isEmpty()) return new String[0];
        
        return Arrays.stream(arrayContent.split(","))
                     .map(s -> s.replace("\"", "").trim())
                     .toArray(String[]::new);
    }

    private static double[] parseDoubleArrayFromJson(String jsonBlock, String key) throws Exception {
        String arrayKeySearch = "\"" + key + "\":[";
        int arrayStartIndex = jsonBlock.indexOf(arrayKeySearch);
         if (arrayStartIndex == -1) {
            throw new Exception("Missing array key: " + key + " in block: " + jsonBlock.substring(0, Math.min(jsonBlock.length(),100)) +"...");
        }
        
        int contentStartIndex = arrayStartIndex + arrayKeySearch.length();
        int braceCount = 0;
        int contentEndIndex = -1;

        for (int i = contentStartIndex; i < jsonBlock.length(); i++) {
            char c = jsonBlock.charAt(i);
            if (c == '[') braceCount++;
            else if (c == ']') {
                if (braceCount == 0) {
                    contentEndIndex = i;
                    break;
                }
                braceCount--;
            }
        }
        if (contentEndIndex == -1) throw new Exception("Malformed array (no closing ']') for key: " + key);

        String arrayContent = jsonBlock.substring(contentStartIndex, contentEndIndex);
        if (arrayContent.trim().isEmpty()) return new double[0];
        
        try {
            return Arrays.stream(arrayContent.split(","))
                         .map(s -> s.trim().equalsIgnoreCase("null") ? "NaN" : s.trim()) // Handle potential "null" strings as NaN
                         .mapToDouble(Double::parseDouble)
                         .toArray();
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number format in array for key: " + key + ", content: '" + arrayContent + "'", e);
        }
    }

    /**
     * Maps WMO Weather interpretation codes (weathercode) to human-readable strings.
     * @param code The integer weather code.
     * @return A descriptive string for the weather condition.
     */
    private static String mapWeatherCodeToDescription(int code) {
        // Simplified mapping based on WMO Weather interpretation codes (weathercode)
        // from Open-Meteo documentation.
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: return "Fog";
            case 48: return "Depositing rime fog";
            case 51: return "Light drizzle";
            case 53: return "Moderate drizzle";
            case 55: return "Dense drizzle";
            case 56: return "Light freezing drizzle";
            case 57: return "Dense freezing drizzle";
            case 61: return "Slight rain";
            case 63: return "Moderate rain";
            case 65: return "Heavy rain";
            case 66: return "Light freezing rain";
            case 67: return "Heavy freezing rain";
            case 71: return "Slight snow fall";
            case 73: return "Moderate snow fall";
            case 75: return "Heavy snow fall";
            case 77: return "Snow grains";
            case 80: return "Slight rain showers";
            case 81: return "Moderate rain showers";
            case 82: return "Violent rain showers";
            case 85: return "Slight snow showers";
            case 86: return "Heavy snow showers";
            case 95: return "Thunderstorm"; // Slight or moderate
            case 96: return "Thunderstorm, slight hail";
            case 99: return "Thunderstorm, heavy hail";
            default: return "Unknown (" + code + ")";
        }
    }
}

/**
 * Abstract base class for data entries that have a timestamp and can provide a summary.
 */
abstract class DatedDataEntry {
    public abstract String getTimestamp();
    public abstract String getSummary(); // Summary might include temperature, context dependent
}

/**
 * Data class to hold the overall weather information for a location.
 * Temperatures here are typically from the API (Celsius) before conversion for display.
 */
class WeatherData extends DatedDataEntry {
    private double currentTemp; // Expected to be Celsius from API
    private double windSpeed;
    private String time; 
    private List<DailyForecast> forecast;

    public WeatherData() {
        this.forecast = new ArrayList<>(); 
    }

    public double getCurrentTemp() { return currentTemp; } // Celsius from API
    public void setCurrentTemp(double currentTemp) { this.currentTemp = currentTemp; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public String getTime() { return time; } 
    public void setTime(String time) { this.time = time; }
    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }

    @Override
    public String getTimestamp() {
        return this.time != null ? this.time : "N/A";
    }

    /**
     * Provides a summary. Note: currentTemp is Celsius from API.
     * The display logic in WeatherGUI will use unitStrategy for actual display.
     */
    @Override
    public String getSummary() {
        // This summary is based on raw (likely Celsius) data.
        return String.format("API Current: %.1f°C, Wind: %.1f km/h", currentTemp, windSpeed);
    }
}

/**
 * Data class to hold forecast information for a single day.
 * Temperatures here are typically from the API (Celsius).
 */
class DailyForecast extends DatedDataEntry {
    private String date;
    private double maxTemp; // Expected to be Celsius from API
    private double minTemp; // Expected to be Celsius from API
    private String summary; // Weather condition summary (e.g., "Clear Sky")

    public String getDate() { return date; } 
    public void setDate(String date) { this.date = date; }
    public double getMaxTemp() { return maxTemp; } // Celsius from API
    public void setMaxTemp(double maxTemp) { this.maxTemp = maxTemp; }
    public double getMinTemp() { return minTemp; } // Celsius from API
    public void setMinTemp(double minTemp) { this.minTemp = minTemp; }
    public void setSummary(String summary) { this.summary = summary; }


    @Override
    public String getTimestamp() {
        return this.date != null ? this.date : "N/A";
    }

    /**
     * Provides a summary of the forecast day.
     * Note: maxTemp and minTemp are Celsius from API.
     * The display logic in WeatherGUI will use unitStrategy.
     */
    @Override
    public String getSummary() {
        // Returns the weather condition summary (e.g., "Clear Sky", "Rain")
        return this.summary != null ? this.summary : "N/A";
    }
}

/**
 * Abstract base class for database handling operations.
 */
abstract class BaseDatabaseHandler {
    protected Connection conn;
    protected String dbUrl;

    public BaseDatabaseHandler(String dbUrl) throws SQLException {
        this.dbUrl = dbUrl;
        loadDriver(); 
        this.conn = DriverManager.getConnection(dbUrl);
        initializeDatabaseSchema(); 
    }
    protected abstract void loadDriver();
    protected abstract void initializeDatabaseSchema() throws SQLException;

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection to " + dbUrl + " closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection to " + dbUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/**
 * Handles interaction with the SQLite database for storing and retrieving weather search history.
 */
class WeatherDatabase extends BaseDatabaseHandler {
    private static final String DB_FILENAME = "weather_history_v2.db"; 
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILENAME;

    public WeatherDatabase() throws SQLException {
        super(DB_URL); 
    }

    @Override
    protected void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found! Ensure sqlite-jdbc JAR is in the classpath.");
            throw new RuntimeException("SQLite JDBC driver not found. Application cannot continue.", e);
        }
    }

    @Override
    protected void initializeDatabaseSchema() throws SQLException {
        // Stores the original API timestamp for the search, and temperature is always Celsius.
        String sql = "CREATE TABLE IF NOT EXISTS weather_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "city TEXT NOT NULL," +
                "temperature REAL NOT NULL," +         // This will store temperature in Celsius
                "search_date_time TEXT NOT NULL," +  // API's timestamp for the current weather data
                "search_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"; // DB entry timestamp

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Saves a weather search to the database.
     * @param city The city name.
     * @param temperatureCelsius The temperature in Celsius.
     * @param apiSearchDateTime The timestamp from the weather API for the data.
     * @throws SQLException If a database error occurs.
     */
    public void saveWeatherSearch(String city, double temperatureCelsius, String apiSearchDateTime) throws SQLException {
        String sql = "INSERT INTO weather_history(city, temperature, search_date_time) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, city);
            pstmt.setDouble(2, temperatureCelsius); // Always store Celsius
            pstmt.setString(3, apiSearchDateTime);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves weather search history from the database.
     * @return A list of WeatherHistory objects.
     * @throws SQLException If a database error occurs.
     */
    public List<WeatherHistory> getWeatherHistory() throws SQLException {
        List<WeatherHistory> historyList = new ArrayList<>();
        // `temperature` column holds Celsius values.
        String sql = "SELECT city, temperature, search_date_time, search_timestamp FROM weather_history ORDER BY search_timestamp DESC LIMIT 50";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                WeatherHistory item = new WeatherHistory(
                        rs.getString("city"),
                        rs.getDouble("temperature"),      // This is Celsius from DB
                        rs.getString("search_date_time") 
                );
                historyList.add(item);
            }
        }
        return historyList;
    }
}

/**
 * Data class representing a single entry in the weather search history.
 */
class WeatherHistory extends DatedDataEntry {
    private String city;
    private double temperature; // Stores temperature in Celsius, as retrieved from DB
    private String apiSearchDateTime; // Timestamp from the API for when the data was current

    /**
     * Constructor for WeatherHistory.
     * @param city The city name.
     * @param temperatureCelsius The temperature in Celsius (as stored in/retrieved from DB).
     * @param apiSearchDateTime The API's timestamp for the weather data.
     */
    public WeatherHistory(String city, double temperatureCelsius, String apiSearchDateTime) {
        this.city = city;
        this.temperature = temperatureCelsius; // This is Celsius
        this.apiSearchDateTime = apiSearchDateTime;
    }

    public String getCity() { return city; }
    public double getTemperature() { return temperature; } // Returns Celsius temperature

    @Override
    public String getTimestamp() {
        // This is the timestamp from the weather API (when the data was fetched/current)
        return this.apiSearchDateTime != null ? this.apiSearchDateTime : "N/A";
    }

    /**
     * Provides a summary indicating the original temperature stored (which is Celsius).
     * This is used in the "Details" column of the history view.
     */
    @Override
    public String getSummary() {
        return String.format("Original: %.1f°C", temperature); // Explicitly states original was Celsius
    }
}

/**
 * Interface defining the Strategy pattern for temperature unit conversion.
 */
interface WeatherUnitStrategy {
    double convertTemperature(double celsius);
    String getUnitSymbol();
}

/**
 * Abstract base class for WeatherUnitStrategy implementations.
 */
abstract class AbstractWeatherUnitStrategy implements WeatherUnitStrategy {}

/**
 * Concrete strategy for Celsius units.
 */
class CelsiusStrategy extends AbstractWeatherUnitStrategy {
    @Override
    public double convertTemperature(double celsius) {
        return celsius; // No conversion needed
    }
    @Override
    public String getUnitSymbol() {
        return "°C";
    }
}

/**
 * Concrete strategy for Fahrenheit units.
 */
class FahrenheitStrategy extends AbstractWeatherUnitStrategy {
    @Override
    public double convertTemperature(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0; // Convert Celsius to Fahrenheit
    }
    @Override
    public String getUnitSymbol() {
        return "°F";
    }
}
