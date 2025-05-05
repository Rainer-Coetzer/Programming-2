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
            }
        });
    }
}

/**
 * Abstract base class for weather-related JFrame windows.
 * Defines a contract for initializing components.
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
            JOptionPane.showMessageDialog(null, "Database initialization failed. History features will be disabled.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            this.db = null; // Disable database features if connection failed
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
        // Note: ImageIcons might require the image file to be present relative to the execution path.
        tabbedPane.addTab("Current Weather", new ImageIcon("weather.png"), currentWeatherPanel, "View current weather conditions");
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
        tabbedPane.addTab("5-Day Forecast", new ImageIcon("forecast.png"), forecastPanel, "View 5-day weather forecast");
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
            // Consider re-fetching or re-displaying weather with the new unit if data is already loaded
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
        JLabel infoLabel = new JLabel("Weather App v1.6 - © 2025"); // Version info
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        infoPanel.add(infoLabel);
        // Add the info panel to the settings panel
        settingsPanel.add(infoPanel);

        // Add the settings panel as a tab
        tabbedPane.addTab("Settings", new ImageIcon("settings.png"), settingsPanel, "Configure application settings");
    }

    /**
     * Creates the "History" tab to display past weather searches from the database.
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
                    // Add header row
                    sb.append(String.format("%-20s %-25s %-15s\n", "City", "Date", "Temperature"));
                    sb.append("------------------------------------------------------------\n");

                    // Format and append each history item
                    for (WeatherHistory item : history) {
                        // Assuming temperature in DB is Celsius, display as such.
                        // If units need to be considered here, logic would be needed.
                        sb.append(String.format("%-20s %-25s %-15.1f°C\n",
                                item.getCity(), item.getSearchDate(), item.getTemperature()));
                    }

                    // Set the text area content
                    historyDisplay.setText(sb.toString());
                } else {
                    // Inform user if database is unavailable
                    historyDisplay.setText("Database connection unavailable.\nHistory features disabled.");
                }
            } catch (SQLException ex) {
                // Display error if loading history fails
                historyDisplay.setText("Error loading history: " + ex.getMessage());
            }
        });

        // Panel to hold the refresh button (using FlowLayout by default)
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);

        // Add the button panel to the top and the history display (in a scroll pane) to the center
        historyPanel.add(buttonPanel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyDisplay), BorderLayout.CENTER);

        // Add the history panel as a tab
        tabbedPane.addTab("History", new ImageIcon("history.png"), historyPanel, "View search history");
    }

    /**
     * Updates the city suggestion popup menu based on the current input in the city text field.
     */
    private void updateSuggestions() {
        String input = cityInput.getText().trim(); // Get trimmed input
        // Only show suggestions if input is reasonably long (e.g., 2+ characters)
        if (input.length() < 2) {
            suggestionMenu.setVisible(false); // Hide menu if input is too short
            return;
        }

        // Fetch city suggestions based on the input (calls the API)
        // Consider running this in a background thread if the API call is slow
        List<String> suggestions = WeatherFetcher.getCitySuggestions(input);
        suggestionMenu.removeAll(); // Clear previous suggestions

        // If no suggestions found, hide the menu
        if (suggestions.isEmpty()) {
            suggestionMenu.setVisible(false);
            return;
        }

        // Populate the popup menu with new suggestions
        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            // Add action listener to fill the text field when a suggestion is clicked
            item.addActionListener(e -> {
                cityInput.setText(suggestion); // Set text field to the selected suggestion
                suggestionMenu.setVisible(false); // Hide the menu
            });
            suggestionMenu.add(item); // Add the menu item
        }

        // Show the popup menu below the city input field
        // Ensure the component is visible and has focus for the menu to show correctly
        if (cityInput.isShowing()) {
             suggestionMenu.show(cityInput, 0, cityInput.getHeight());
             // Request focus to ensure interactions work as expected
             cityInput.requestFocusInWindow();
        } else {
             suggestionMenu.setVisible(false); // Don't show if input field isn't visible
        }
    }


    /**
     * Fetches weather data for the city entered in the input field,
     * converts temperatures based on the selected unit, displays the data,
     * and saves the search to the history database.
     */
    private void fetchWeather() {
        String city = cityInput.getText().trim(); // Get city name from input field
        // Validate input
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a city name",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return; // Stop if city is empty
        }

        // Consider showing a loading indicator here

        try {
            // Fetch weather data using the WeatherFetcher utility class
            WeatherData data = WeatherFetcher.getWeatherData(city);

            // --- Temperature Conversion using Strategy Pattern ---
            // Convert the fetched current temperature (assumed Celsius from API)
            double originalCurrentTempCelsius = data.getCurrentTemp(); // Keep original for DB
            double convertedTemp = unitStrategy.convertTemperature(originalCurrentTempCelsius);
            data.setCurrentTemp(convertedTemp); // Update data object with converted temp for display

            // Convert forecast temperatures
            for (DailyForecast day : data.getForecast()) {
                day.setMaxTemp(unitStrategy.convertTemperature(day.getMaxTemp()));
                day.setMinTemp(unitStrategy.convertTemperature(day.getMinTemp()));
            }
            // --- End Conversion ---

            // Display the fetched and converted weather data in the respective tabs
            displayWeather(data);

            // Save the search to the database (if available) using the original Celsius temp
            if (db != null) {
                try {
                    // Save the original Celsius temperature for consistency in history
                    db.saveWeatherSearch(city, originalCurrentTempCelsius);
                } catch (SQLException e) {
                    // Log database save errors, but don't necessarily block the user
                    System.err.println("Failed to save search to database: " + e.getMessage());
                    // Optionally show a non-blocking warning to the user
                }
            }
        } catch (Exception ex) {
            // Show error message if fetching or processing fails
            JOptionPane.showMessageDialog(frame, "Error fetching weather data for '" + city + "':\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            // Clear display areas on error
            currentWeatherDisplay.setText("Failed to load weather data.");
            forecastDisplay.setText("Failed to load forecast data.");
        } finally {
            // Consider hiding the loading indicator here
        }
    }

    /**
     * Displays the fetched weather data in the current weather and forecast text areas.
     * @param data The WeatherData object containing information to display.
     */
    private void displayWeather(WeatherData data) {
        // --- Display Current Weather ---
        StringBuilder current = new StringBuilder(); // Use StringBuilder for efficiency
        current.append("=== CURRENT WEATHER ===\n\n");
        current.append(String.format("Location:    %s\n", cityInput.getText())); // Use entered city name
        current.append(String.format("Time:        %s\n", data.getTime())); // Display fetch time
        // Display temperature with the correct unit symbol from the strategy
        current.append(String.format("Temperature: %.1f%s\n",
                data.getCurrentTemp(), unitStrategy.getUnitSymbol()));
        current.append(String.format("Wind Speed:  %.1f km/h\n", data.getWindSpeed())); // Assuming km/h from API
        // Add more fields as available (humidity, pressure, etc.)

        currentWeatherDisplay.setText(current.toString()); // Update the text area
        currentWeatherDisplay.setCaretPosition(0); // Scroll to top

        // --- Display Forecast ---
        StringBuilder forecast = new StringBuilder();
        forecast.append("=== 5-DAY FORECAST ===\n\n");
        // Header for the forecast table
        forecast.append(String.format("%-15s %-10s %-10s\n", "Date", "High", "Low"));
        forecast.append("------------------------------------\n"); // Separator line

        // Loop through the forecast data (limited to 5 days by API request/parsing)
        for (DailyForecast day : data.getForecast()) {
            // Format each day's forecast with appropriate units
            forecast.append(String.format("%-15s %-10.1f%s %-10.1f%s\n",
                    day.getDate(),
                    day.getMaxTemp(), unitStrategy.getUnitSymbol(), // High temp + unit
                    day.getMinTemp(), unitStrategy.getUnitSymbol())); // Low temp + unit
        }

        forecastDisplay.setText(forecast.toString()); // Update the text area
        forecastDisplay.setCaretPosition(0); // Scroll to top
    }
}

/**
 * Utility class responsible for fetching weather data from external APIs.
 * Uses Open-Meteo APIs for geocoding (city to coordinates) and weather forecasts.
 */
class WeatherFetcher {
    // API endpoints
    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    /**
     * Fetches complete weather data (current and forecast) for a given city name.
     * It first gets coordinates using the geocoding API, then fetches weather using those coordinates.
     * @param city The name of the city.
     * @return A WeatherData object containing the fetched information.
     * @throws Exception If geocoding or weather fetching fails.
     */
    public static WeatherData getWeatherData(String city) throws Exception {
        // Step 1: Get coordinates (latitude, longitude) for the city
        double[] coordinates = getCoordinates(city);
        if (coordinates == null) {
            throw new Exception("Could not find coordinates for city: " + city);
        }

        // Step 2: Fetch weather data using the coordinates
        String weatherJson = getWeatherJson(coordinates[0], coordinates[1]);

        // Step 3: Parse the JSON response into a WeatherData object
        return parseWeatherData(weatherJson);
    }

    /**
     * Fetches city name suggestions based on user input using the geocoding API.
     * @param input The partial city name entered by the user.
     * @return A list of suggested city names (up to 5). Returns empty list on error or short input.
     */
    public static List<String> getCitySuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        // Basic validation: return empty if input is null or too short
        if (input == null || input.length() < 2) { // Require at least 2 chars for suggestions
            return suggestions;
        }

        try {
            // Construct the URL for the geocoding API search endpoint
            // Encode the input to handle spaces and special characters
            String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(input, "UTF-8") + "&count=5"; // Limit results
            // Make the API request
            String response = makeApiRequest(urlString);

            // --- Basic JSON Parsing (String manipulation - fragile) ---
            // This is a simplified and potentially error-prone way to parse JSON.
            // Using a proper JSON library (like Gson or Jackson) is highly recommended for robustness.
            String[] parts = response.split("\"name\":\""); // Split by the "name" key
            // Start from index 1 (skip the part before the first name)
            // Limit to 5 suggestions or the number of parts found
            for (int i = 1; i < parts.length && suggestions.size() < 5; i++) {
                String suggestion = parts[i].split("\"")[0]; // Get the value before the next quote
                // Basic filtering: Avoid adding duplicates or results that don't start with the input
                if (!suggestions.contains(suggestion) && suggestion.toLowerCase().startsWith(input.toLowerCase())) {
                     suggestions.add(suggestion);
                }
            }
            // --- End Basic JSON Parsing ---

        } catch (Exception e) {
            // Log errors during suggestion fetching but don't crash the app
            System.err.println("Error fetching city suggestions: " + e.getMessage());
            // Return the (potentially empty) list of suggestions found so far
        }

        return suggestions;
    }

    /**
     * Gets the latitude and longitude for a given city name using the geocoding API.
     * @param city The city name.
     * @return A double array containing [latitude, longitude], or null if not found.
     * @throws Exception If the API request fails.
     */
    private static double[] getCoordinates(String city) throws Exception {
        // Construct the geocoding API URL, encoding the city name
        String urlString = GEOCODING_API + "?name=" + URLEncoder.encode(city, "UTF-8") + "&count=1"; // Only need the top result
        // Make the API request
        String response = makeApiRequest(urlString);

        // --- Basic JSON Parsing (String manipulation - fragile) ---
        // Again, a JSON library is recommended here.
        String[] parts = response.split("\"results\":\\["); // Find the start of the results array
        // Check if results array exists and is not empty
        if (parts.length < 2 || !parts[1].trim().startsWith("{")) {
             System.err.println("Geocoding response format error or no results for: " + city);
             System.err.println("Response: " + response);
             return null; // No results found or format error
        }


        // Try to extract the first result object
        String block;
        if (parts[1].contains("}")) {
            block = parts[1].split("\\}")[0]; // Get the content of the first result object
        } else {
            System.err.println("Geocoding response format error (missing closing brace) for: " + city);
            return null;
        }


        // Extract latitude and longitude using string splitting
        // This assumes the order and presence of these fields.
        String latStr, lonStr;
        try {
            latStr = block.split("\"latitude\":")[1].split(",")[0].trim();
            lonStr = block.split("\"longitude\":")[1].split(",")[0].trim(); // Often latitude/longitude are the last fields before '}'
             if (lonStr.endsWith("}")) { // Handle case where longitude is the last element
                lonStr = lonStr.substring(0, lonStr.length() - 1);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Failed to parse coordinates from geocoding response for: " + city);
            System.err.println("Block: " + block);
            return null; // Parsing failed
        }


        // Parse the extracted strings to doubles
        try {
            return new double[]{Double.parseDouble(latStr), Double.parseDouble(lonStr)};
        } catch (NumberFormatException e) {
             System.err.println("Failed to parse coordinate numbers: lat='" + latStr + "', lon='" + lonStr + "'");
             return null;
        }
        // --- End Basic JSON Parsing ---
    }


    /**
     * Fetches the weather forecast JSON string from the Open-Meteo API for given coordinates.
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @return The JSON response string.
     * @throws Exception If the API request fails.
     */
    private static String getWeatherJson(double latitude, double longitude) throws Exception {
        // Construct the weather API URL with required parameters
        String urlString = WEATHER_API + "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&current_weather=true" + // Request current weather data
                "&daily=temperature_2m_max,temperature_2m_min" + // Request daily max/min temps
                "&timezone=auto"; // Use auto-detected timezone

        // Make the API request
        return makeApiRequest(urlString);
    }

    /**
     * Makes a generic HTTP GET request to the specified URL string.
     * @param urlString The URL to connect to.
     * @return The response body as a String.
     * @throws Exception If the connection or reading fails.
     */
    private static String makeApiRequest(String urlString) throws Exception {
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); // Set request method to GET
            conn.setConnectTimeout(10000); // 10 seconds connection timeout
            conn.setReadTimeout(10000); // 10 seconds read timeout

            int responseCode = conn.getResponseCode(); // Get the HTTP response code

            // Check for successful response (HTTP 200 OK)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();
            } else {
                // Try to read the error stream for more details if available
                inputStream = conn.getErrorStream();
                String errorDetails = " (No details available)";
                if (inputStream != null) {
                     StringBuilder errorResponse = new StringBuilder();
                     reader = new BufferedReader(new InputStreamReader(inputStream));
                     String line;
                     while ((line = reader.readLine()) != null) {
                         errorResponse.append(line);
                     }
                     errorDetails = ": " + errorResponse.toString();
                     reader.close(); // Close reader for error stream
                }
                 throw new IOException("HTTP error code: " + responseCode + " from " + urlString + errorDetails);
            }


            // Read the response body
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString(); // Return the full response string

        } catch (MalformedURLException e) {
            throw new Exception("Invalid API URL: " + urlString, e);
        } catch (IOException e) {
            // Catch network-related errors (connection refused, timeout, etc.)
             throw new Exception("Failed to connect to API or read response from " + urlString + ": " + e.getMessage(), e);
        } finally {
            // Ensure resources are closed
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            }
            if (conn != null) {
                conn.disconnect(); // Disconnect the HttpURLConnection
            }
        }
    }


    /**
     * Parses the JSON string containing weather data into a WeatherData object.
     * Uses basic string manipulation, which is fragile. A JSON library is recommended.
     * @param jsonData The JSON string received from the weather API.
     * @return A populated WeatherData object.
     * @throws Exception If parsing fails due to format errors or missing data.
     */
    private static WeatherData parseWeatherData(String jsonData) throws Exception {
        WeatherData data = new WeatherData(); // Create object to hold parsed data

        try {
            // --- Parse Current Weather (Fragile String Splitting) ---
            if (!jsonData.contains("\"current_weather\":{")) throw new Exception("Missing 'current_weather' block in JSON");
            String currentBlock = jsonData.split("\"current_weather\":\\{")[1].split("\\}")[0];

            if (!currentBlock.contains("\"temperature\":")) throw new Exception("Missing 'temperature' in current weather");
            data.setCurrentTemp(Double.parseDouble(currentBlock.split("\"temperature\":")[1].split(",")[0].trim()));

            if (!currentBlock.contains("\"windspeed\":")) throw new Exception("Missing 'windspeed' in current weather");
            data.setWindSpeed(Double.parseDouble(currentBlock.split("\"windspeed\":")[1].split(",")[0].trim())); // Assuming windspeed is always present

            if (!currentBlock.contains("\"time\":\"")) throw new Exception("Missing 'time' in current weather");
            data.setTime(currentBlock.split("\"time\":\"")[1].split("\"")[0]);

            // --- Parse Daily Forecast (Fragile String Splitting) ---
             if (!jsonData.contains("\"daily\":{")) throw new Exception("Missing 'daily' block in JSON");
            String dailyBlock = jsonData.split("\"daily\":\\{")[1].split("\\}\\}")[0]; // Get the content within "daily": { ... }}

             if (!dailyBlock.contains("\"time\":[")) throw new Exception("Missing 'time' array in daily forecast");
            String[] dates = dailyBlock.split("\"time\":\\[")[1].split("\\]")[0].replace("\"", "").split(",");

             if (!dailyBlock.contains("\"temperature_2m_max\":[")) throw new Exception("Missing 'temperature_2m_max' array in daily forecast");
            String[] maxTemps = dailyBlock.split("\"temperature_2m_max\":\\[")[1].split("\\]")[0].split(",");

             if (!dailyBlock.contains("\"temperature_2m_min\":[")) throw new Exception("Missing 'temperature_2m_min' array in daily forecast");
            String[] minTemps = dailyBlock.split("\"temperature_2m_min\":\\[")[1].split("\\]")[0].split(",");

            // Check if arrays have consistent lengths
             if (dates.length != maxTemps.length || dates.length != minTemps.length) {
                 throw new Exception("Mismatch in forecast array lengths (dates, maxTemps, minTemps)");
             }


            List<DailyForecast> forecastList = new ArrayList<>();
            // Limit to 5 days or the available data, whichever is smaller
            int forecastDays = Math.min(dates.length, 5);
            for (int i = 0; i < forecastDays; i++) {
                DailyForecast day = new DailyForecast();
                day.setDate(dates[i].trim());
                 // Handle potential "null" values from the API for temperatures
                 day.setMaxTemp(parseTempSafe(maxTemps[i]));
                 day.setMinTemp(parseTempSafe(minTemps[i]));
                forecastList.add(day);
            }
            data.setForecast(forecastList); // Set the parsed forecast list

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException e) {
            // Catch common errors during parsing
            System.err.println("Error parsing weather JSON data: " + e.getMessage());
            System.err.println("JSON Data Snippet: " + (jsonData.length() > 500 ? jsonData.substring(0, 500) + "..." : jsonData)); // Log part of the JSON
            throw new Exception("Failed to parse weather data. Check API response format.", e);
        }

        return data; // Return the populated WeatherData object
    }

     /**
     * Safely parses temperature strings that might be "null".
     * @param tempStr The temperature string from the JSON.
     * @return The parsed double temperature, or Double.NaN if input is "null" or invalid.
     */
     private static double parseTempSafe(String tempStr) {
         if (tempStr == null || tempStr.trim().equalsIgnoreCase("null")) {
             return Double.NaN; // Use NaN (Not a Number) to represent missing data
         }
         try {
             return Double.parseDouble(tempStr.trim());
         } catch (NumberFormatException e) {
             System.err.println("Warning: Could not parse temperature value: '" + tempStr + "'");
             return Double.NaN; // Return NaN on parsing error
         }
     }
}


/**
 * Data class to hold the overall weather information for a location.
 * Contains current weather conditions and a list of daily forecasts.
 */
class WeatherData {
    // Fields for current weather
    private double currentTemp; // Current temperature (in Celsius from API initially)
    private double windSpeed; // Current wind speed (e.g., km/h)
    private String time; // Timestamp of the current weather data

    // Field for the forecast
    private List<DailyForecast> forecast; // List of daily forecast objects

    // --- Getters and Setters ---
    public double getCurrentTemp() { return currentTemp; }
    public void setCurrentTemp(double currentTemp) { this.currentTemp = currentTemp; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }
}

/**
 * Data class to hold forecast information for a single day.
 */
class DailyForecast {
    // Fields for daily forecast
    private String date; // Date of the forecast (e.g., "YYYY-MM-DD")
    private double maxTemp; // Maximum temperature for the day (Celsius from API)
    private double minTemp; // Minimum temperature for the day (Celsius from API)

    // --- Getters and Setters ---
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getMaxTemp() { return maxTemp; }
    public void setMaxTemp(double maxTemp) { this.maxTemp = maxTemp; }

    public double getMinTemp() { return minTemp; }
    public void setMinTemp(double minTemp) { this.minTemp = minTemp; }
}

/**
 * Handles interaction with the SQLite database for storing and retrieving weather search history.
 */
class WeatherDatabase {
    // Database connection URL (using SQLite file-based database)
    private static final String DB_URL = "jdbc:sqlite:weather_history.db"; // Changed filename for clarity
    private Connection conn; // JDBC connection object

    // Static initializer block to load the SQLite JDBC driver
    static {
        try {
            // Load the SQLite driver class
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // Print error if the driver is not found (usually means missing dependency)
            System.err.println("SQLite JDBC driver not found! Ensure sqlite-jdbc JAR is in the classpath.");
            e.printStackTrace();
        }
    }

    /**
     * Constructor: Establishes a connection to the database and initializes the necessary table.
     * @throws SQLException If the database connection or initialization fails.
     */
    public WeatherDatabase() throws SQLException {
        // Establish the connection using the DB URL
        conn = DriverManager.getConnection(DB_URL);
        // Ensure the history table exists
        initializeDatabase();
    }

    /**
     * Creates the 'weather_history' table if it doesn't already exist.
     * @throws SQLException If executing the SQL statement fails.
     */
    private void initializeDatabase() throws SQLException {
        // SQL statement to create the table
        String sql = "CREATE TABLE IF NOT EXISTS weather_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," + // Auto-incrementing primary key
                "city TEXT NOT NULL," +                  // City name (cannot be null)
                "temperature REAL NOT NULL," +           // Temperature (stored as real/double, assumed Celsius)
                "search_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"; // Timestamp of the search

        // Use try-with-resources to ensure the Statement is closed automatically
        try (Statement stmt = conn.createStatement()) {
            // Execute the SQL statement
            stmt.execute(sql);
        }
    }

    /**
     * Saves a weather search record (city and temperature) into the database.
     * @param city The name of the city searched.
     * @param temperature The current temperature recorded (assumed Celsius).
     * @throws SQLException If the database insertion fails.
     */
    public void saveWeatherSearch(String city, double temperature) throws SQLException {
        // SQL statement for insertion using prepared parameters to prevent SQL injection
        String sql = "INSERT INTO weather_history(city, temperature) VALUES(?, ?)";

        // Use try-with-resources for the PreparedStatement
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the parameter values
            pstmt.setString(1, city);       // Set the first parameter (city)
            pstmt.setDouble(2, temperature); // Set the second parameter (temperature)
            // Execute the update (insertion)
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves the weather search history from the database.
     * @return A List of WeatherHistory objects, ordered by search date descending (newest first), limited to 50 entries.
     * @throws SQLException If the database query fails.
     */
    public List<WeatherHistory> getWeatherHistory() throws SQLException {
        List<WeatherHistory> history = new ArrayList<>(); // List to store results
        // SQL query to select history, ordered by date, limited to 50 records
        String sql = "SELECT city, temperature, search_date FROM weather_history ORDER BY search_date DESC LIMIT 50";

        // Use try-with-resources for Statement and ResultSet
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) { // Execute the query

            // Iterate through the results
            while (rs.next()) {
                // Create a WeatherHistory object for each row
                WeatherHistory item = new WeatherHistory(
                        rs.getString("city"),         // Get city from result set
                        rs.getDouble("temperature"),  // Get temperature
                        rs.getString("search_date")   // Get timestamp as string
                );
                history.add(item); // Add the item to the list
            }
        }
        return history; // Return the list of history items
    }

    /**
     * Closes the database connection. Should be called when the application exits.
     */
    public void close() {
        try {
            // Check if the connection is open before closing
            if (conn != null && !conn.isClosed()) {
                conn.close(); // Close the connection
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            // Log error if closing fails
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}

/**
 * Data class representing a single entry in the weather search history.
 */
class WeatherHistory {
    private String city;        // Name of the city searched
    private double temperature; // Temperature recorded at the time of search (assumed Celsius)
    private String searchDate;  // Timestamp of the search

    /**
     * Constructor for WeatherHistory.
     * @param city The city name.
     * @param temperature The recorded temperature.
     * @param searchDate The timestamp of the search.
     */
    public WeatherHistory(String city, double temperature, String searchDate) {
        this.city = city;
        this.temperature = temperature;
        this.searchDate = searchDate;
    }

    // --- Getters ---
    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public String getSearchDate() { return searchDate; }
}

/**
 * Interface defining the Strategy pattern for temperature unit conversion.
 * Allows switching between Celsius and Fahrenheit display logic.
 */
interface WeatherUnitStrategy {
    /**
     * Converts a temperature value (assumed to be in Celsius) to the target unit.
     * @param celsius The temperature in Celsius.
     * @return The temperature converted to the strategy's unit.
     */
    double convertTemperature(double celsius);

    /**
     * Gets the symbol for the temperature unit (e.g., "°C", "°F").
     * @return The unit symbol string.
     */
    String getUnitSymbol();
}

/**
 * Concrete strategy implementation for Celsius units.
 * It doesn't perform any conversion as the input is already Celsius.
 */
class CelsiusStrategy implements WeatherUnitStrategy {
    /**
     * Returns the input Celsius value unchanged.
     * @param celsius The temperature in Celsius.
     * @return The same temperature in Celsius.
     */
    @Override
    public double convertTemperature(double celsius) {
        return celsius; // No conversion needed
    }

    /**
     * Returns the Celsius unit symbol.
     * @return "°C".
     */
    @Override
    public String getUnitSymbol() {
        return "°C";
    }
}

/**
 * Concrete strategy implementation for Fahrenheit units.
 * Converts Celsius temperatures to Fahrenheit.
 */
class FahrenheitStrategy implements WeatherUnitStrategy {
    /**
     * Converts the input Celsius temperature to Fahrenheit.
     * @param celsius The temperature in Celsius.
     * @return The temperature converted to Fahrenheit.
     */
    @Override
    public double convertTemperature(double celsius) {
        // Apply the Celsius to Fahrenheit conversion formula
        return celsius * 9.0 / 5.0 + 32.0; // Use floating-point division
    }

    /**
     * Returns the Fahrenheit unit symbol.
     * @return "°F".
     */
    @Override
    public String getUnitSymbol() {
        return "°F";
    }
}
