import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;

public class TaxiMeter {
    private static double baseRate = 10.0;
    private static double taxPercent = 0.0;
    private static JFrame frame;
    private static JTextArea resultArea;
    private static JLabel todayDistanceLabel;
    private static JLabel overallDistanceLabel;
    private static JLabel currentTimeLabel;
    private static JComboBox<String> fontDropdown;
    private static JComboBox<String> fontSizeDropdown;

    public static void main(String[] args) {
        frame = new JFrame("DIGITAL TAXI METER");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        String[] fontOptions = {"Arial", "Verdana", "Times New Roman"};
        String[] fontSizeOptions = {"12", "14", "16", "18", "20"};
        
        fontDropdown = new JComboBox<>(fontOptions);
        fontSizeDropdown = new JComboBox<>(fontSizeOptions);
        
        fontSizeDropdown.setSelectedItem("16");

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        frame.add(new JLabel("Enter Source Address (e.g., locality/village, city/district , state/country):"), gbc);
        JTextField sourceField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 0;
        frame.add(sourceField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        frame.add(new JLabel("Enter Destination Address (e.g., locality/village, city/district , state/country):"), gbc);
        JTextField destinationField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 1;
        frame.add(destinationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        frame.add(new JLabel("First Kilometre Fare (Rs.):"), gbc);
        JTextField baseRateField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 2;
        frame.add(baseRateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        frame.add(new JLabel("Subsequent Kilometre Fare (Rs.):"), gbc);
        JTextField rateField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 3;
        frame.add(rateField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        frame.add(new JLabel("Tax Percent (e.g., 10.0):"), gbc);
        JTextField taxField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 4;
        frame.add(taxField, gbc);

        JButton calculateButton = new JButton("Calculate Total Fare");
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        frame.add(calculateButton, gbc);

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        frame.add(scrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        frame.add(new JLabel("Search Record by Source:"), gbc);
        JTextField searchSourceField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 7;
        frame.add(searchSourceField, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        frame.add(new JLabel("Destination:"), gbc);
        JTextField searchDestinationField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 8;
        frame.add(searchDestinationField, gbc);

        JButton searchButton = new JButton("Search");
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        frame.add(searchButton, gbc);

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 1;
        frame.add(new JLabel("Edit/Delete Record by ID:"), gbc);
        JTextField editDeleteIdField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 10;
        frame.add(editDeleteIdField, gbc);

        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 1;
        frame.add(editButton, gbc);
        gbc.gridx = 1; gbc.gridy = 11;
        frame.add(deleteButton, gbc);

        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 1;
        frame.add(new JLabel("Choose Font:"), gbc);
        gbc.gridx = 1; gbc.gridy = 12;
        frame.add(fontDropdown, gbc);

        gbc.gridx = 0; gbc.gridy = 13; gbc.gridwidth = 1;
        frame.add(new JLabel("Choose Font Size:"), gbc);
        gbc.gridx = 1; gbc.gridy = 13;
        frame.add(fontSizeDropdown, gbc);

        gbc.gridx = 0; gbc.gridy = 14; gbc.gridwidth = 2;
        todayDistanceLabel = new JLabel("Distance Traveled Today: 0 km");
        frame.add(todayDistanceLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 15; gbc.gridwidth = 2;
        overallDistanceLabel = new JLabel("Overall Distance Traveled: 0 km");
        frame.add(overallDistanceLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 16; gbc.gridwidth = 2;
        currentTimeLabel = new JLabel("Current Time: ");
        frame.add(currentTimeLabel, gbc);

        Font defaultFont = new Font("Arial", Font.PLAIN, 16);
        updateFont(frame, defaultFont);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String sourceAddress = sourceField.getText();
                    String destinationAddress = destinationField.getText();
                    double customRate = Double.parseDouble(rateField.getText());
                    baseRate = Double.parseDouble(baseRateField.getText());
                    taxPercent = Double.parseDouble(taxField.getText());

                    String matrixResponse = GeocodingService.getDistanceMatrix(sourceAddress, destinationAddress);
                    JSONObject matrixJson = new JSONObject(matrixResponse);

                    if (!matrixJson.getString("status").equals("OK")) {
                        throw new Exception("Error in API response: " + matrixJson.getString("status"));
                    }

                    double distance = matrixJson.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0)
                                                .getJSONObject("distance")
                                                .getDouble("value") / 1000;

                    double fare = baseRate + (distance * customRate);
                    double taxAmount = (fare * taxPercent) / 100;

                    resultArea.setText(String.format("Distance: %.2f km\n" +
                                                     "Fare: Rs. %.2f\n" +
                                                     "Tax: Rs. %.2f\n" +
                                                     "Total Fare: Rs. %.2f\n",
                                                     distance, fare, taxAmount, fare + taxAmount));

                    try (PrintWriter writer = new PrintWriter(new FileWriter("taxi_meter_log.csv", true))) {
                        writer.println(sourceAddress + "," + destinationAddress + "," +
                                       String.format("%.2f", distance) + "," +
                                       String.format("%.2f", fare) + "," +
                                       String.format("%.2f", taxAmount) + "," +
                                       String.format("%.2f", fare + taxAmount) + "," +
                                       String.format("%.2f", taxPercent) + "," +
                                       new java.util.Date());
                    }

                    storeResultsInDatabase(sourceAddress, destinationAddress, distance, fare, taxAmount, taxPercent);
                    updateDistanceLabels();

                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchSource = searchSourceField.getText();
                String searchDestination = searchDestinationField.getText();
                searchDatabase(searchSource, searchDestination);
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = editDeleteIdField.getText();
                editRecord(id);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = editDeleteIdField.getText();
                deleteRecord(id);
            }
        });

        fontDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFont(frame, getSelectedFont());
            }
        });

        fontSizeDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFont(frame, getSelectedFont());
            }
        });

        frame.setVisible(true);

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime();
            }
        });
        timer.start();
    }

    private static void storeResultsInDatabase(String sourceAddress, String destinationAddress, double distance,
                                               double fare, double taxAmount, double taxPercent) throws Exception {
        String url = "jdbc:mysql://localhost:3306/taxi_db";
        String user = "root";
        String password = "shaanrout@123";
        Connection conn = DriverManager.getConnection(url, user, password);

        String query = "INSERT INTO taxi_meter_records (source, destination, distance, fare, tax, total_fare, tax_percent, journey_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, sourceAddress);
        stmt.setString(2, destinationAddress);
        stmt.setDouble(3, distance);
        stmt.setDouble(4, fare);
        stmt.setDouble(5, taxAmount);
        stmt.setDouble(6, fare + taxAmount);
        stmt.setDouble(7, taxPercent);
        stmt.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
        stmt.executeUpdate();
        conn.close();
    }

    private static void searchDatabase(String sourceAddress, String destinationAddress) {
        try {
            String url = "jdbc:mysql://localhost:3306/taxi_db";
            String user = "root";
            String password = "shaanrout@123";
            Connection conn = DriverManager.getConnection(url, user, password);

            String query = "SELECT * FROM taxi_meter_records WHERE source LIKE ? AND destination LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + sourceAddress + "%");
            stmt.setString(2, "%" + destinationAddress + "%");
            ResultSet rs = stmt.executeQuery();

            StringBuilder results = new StringBuilder();
            while (rs.next()) {
                results.append("ID: ").append(rs.getInt("id"))
                       .append(", Source: ").append(rs.getString("source"))
                       .append(", Destination: ").append(rs.getString("destination"))
                       .append(", Distance: ").append(rs.getDouble("distance"))
                       .append(", Fare: ").append(rs.getDouble("fare"))
                       .append(", Tax: ").append(rs.getDouble("tax"))
                       .append(", Total Fare: ").append(rs.getDouble("total_fare"))
                       .append(", Tax Percent: ").append(rs.getDouble("tax_percent"))
                       .append(", Date/Time: ").append(rs.getTimestamp("journey_timestamp"))
                       .append("\n");
            }
            resultArea.setText(results.toString());
            conn.close();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }

    private static void editRecord(String id) {
        // Implement record editing logic here
    }

    private static void deleteRecord(String id) {
        try {
            String url = "jdbc:mysql://localhost:3306/taxi_db";
            String user = "root";
            String password = "shaanrout@123";
            Connection conn = DriverManager.getConnection(url, user, password);

            String query = "DELETE FROM taxi_meter_records WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();
            conn.close();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }

    private static void updateDistanceLabels() {
        try {
            String url = "jdbc:mysql://localhost:3306/taxi_db";
            String user = "root";
            String password = "shaanrout@123";
            Connection conn = DriverManager.getConnection(url, user, password);

            String todayQuery = "SELECT SUM(distance) FROM taxi_meter_records WHERE DATE(journey_timestamp) = CURDATE()";
            PreparedStatement todayStmt = conn.prepareStatement(todayQuery);
            ResultSet todayRs = todayStmt.executeQuery();
            if (todayRs.next()) {
                double todayDistance = todayRs.getDouble(1);
                todayDistanceLabel.setText("Distance Traveled Today: " + String.format("%.2f", todayDistance) + " km");
            }

            String overallQuery = "SELECT SUM(distance) FROM taxi_meter_records";
            PreparedStatement overallStmt = conn.prepareStatement(overallQuery);
            ResultSet overallRs = overallStmt.executeQuery();
            if (overallRs.next()) {
                double overallDistance = overallRs.getDouble(1);
                overallDistanceLabel.setText("Overall Distance Traveled: " + String.format("%.2f", overallDistance) + " km");
            }
            conn.close();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }

    private static void updateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
        String currentTime = sdf.format(new Date());
        currentTimeLabel.setText("Current Time: " + currentTime);
    }

    private static Font getSelectedFont() {
        String selectedFont = (String) fontDropdown.getSelectedItem();
        int fontSize = Integer.parseInt((String) fontSizeDropdown.getSelectedItem());
        return new Font(selectedFont, Font.PLAIN, fontSize);
    }

    private static void updateFont(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                updateFont((Container) comp, font);
            }
        }
    }
}
















/*import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONObject;

public class TaxiMeter {
    private static double baseRate = 10.0; // Default base fare in currency units
    private static double taxPercent = 0.0; // Default tax percent
    private static JFrame frame;
    private static JTextArea resultArea;

    public static void main(String[] args) {
        frame = new JFrame("DIGITAL TAXI METER");
        frame.setSize(600, 600); // Increased size for additional components
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding for components

        // Font settings
        String[] fontOptions = {"Arial", "Verdana", "Times New Roman"};
        String[] fontSizeOptions = {"12", "14", "16", "18", "20"};
        
        JComboBox<String> fontDropdown = new JComboBox<>(fontOptions);
        JComboBox<String> fontSizeDropdown = new JComboBox<>(fontSizeOptions);
        
        // Set default font size
        fontSizeDropdown.setSelectedItem("16");

        // Inputs
        JLabel sourceLabel = new JLabel("Enter Source Address:");
        JTextField sourceField = new JTextField(20);

        JLabel destinationLabel = new JLabel("Enter Destination Address:");
        JTextField destinationField = new JTextField(20);

        JLabel baseRateLabel = new JLabel("First Kilometre Fare (Rs.):");
        JTextField baseRateField = new JTextField(10); // Default value for base rate

        JLabel rateLabel = new JLabel("Subsequent Kilometre Fare (Rs.):");
        JTextField rateField = new JTextField(10); // Default value for rate
        
        JLabel taxLabel = new JLabel("Tax Percent:");
        JTextField taxField = new JTextField(10); // Default value for tax percent

        JButton calculateButton = new JButton("Calculate Total Fare");

        // Search feature
        JLabel searchLabel = new JLabel("Search by Source and Destination:");
        JTextField searchSourceField = new JTextField(15);
        JTextField searchDestinationField = new JTextField(15);
        JButton searchButton = new JButton("Search");

        // Edit and Delete feature
        JLabel editDeleteLabel = new JLabel("Edit or Delete Record by ID:");
        JTextField editDeleteIdField = new JTextField(10);
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);

        // GridBagLayout positioning
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Adding components to the frame with GridBagConstraints
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        frame.add(sourceLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2;
        frame.add(sourceField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        frame.add(destinationLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        frame.add(destinationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        frame.add(baseRateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        frame.add(baseRateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        frame.add(rateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        frame.add(rateField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        frame.add(taxLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        frame.add(taxField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        frame.add(calculateButton, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 3;
        frame.add(resultScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        frame.add(searchLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 7;
        frame.add(searchSourceField, gbc);
        gbc.gridx = 2; gbc.gridy = 7;
        frame.add(searchDestinationField, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3;
        frame.add(searchButton, gbc);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1;
        frame.add(editDeleteLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 9;
        frame.add(editDeleteIdField, gbc);
        gbc.gridx = 2; gbc.gridy = 9;
        frame.add(editButton, gbc);

        gbc.gridx = 0; gbc.gridy = 10;
        frame.add(deleteButton, gbc);

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 1;
        frame.add(new JLabel("Choose Font:"), gbc);
        gbc.gridx = 1; gbc.gridy = 11;
        frame.add(fontDropdown, gbc);

        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 1;
        frame.add(new JLabel("Choose Font Size:"), gbc);
        gbc.gridx = 1; gbc.gridy = 12;
        frame.add(fontSizeDropdown, gbc);

        // Set default font size to 16
        Font defaultFont = new Font("Arial", Font.PLAIN, 16);
        updateFont(frame, defaultFont);

        // Action Listener for the Calculate button
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get addresses and rates from input fields
                    String sourceAddress = sourceField.getText();
                    String destinationAddress = destinationField.getText();
                    double customRate = Double.parseDouble(rateField.getText());
                    baseRate = Double.parseDouble(baseRateField.getText());
                    taxPercent = Double.parseDouble(taxField.getText());

                    // Get distance from GeocodingService
                    String matrixResponse = GeocodingService.getDistanceMatrix(sourceAddress, destinationAddress);
                    JSONObject matrixJson = new JSONObject(matrixResponse);

                    // Check if the response contains valid data
                    if (!matrixJson.getString("status").equals("OK")) {
                        throw new Exception("Error in API response: " + matrixJson.getString("status"));
                    }

                    // Extracting distance information
                    double distance = matrixJson.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0)
                                                .getJSONObject("distance")
                                                .getDouble("value") / 1000; // Convert meters to km

                    // Calculate fare
                    double fare = baseRate + (distance * customRate);
                    double taxAmount = (fare * taxPercent) / 100;

                    // Display results with two decimal places
                    resultArea.setText(String.format("Distance: %.2f km\n" +
                                                     "Fare: Rs. %.2f\n" +
                                                     "Tax: Rs. %.2f\n" +
                                                     "Total Fare: Rs. %.2f\n",
                                                     distance, fare, taxAmount, fare + taxAmount));

                    // Store results in CSV file
                    try (PrintWriter writer = new PrintWriter(new FileWriter("taxi_meter_log.csv", true))) {
                        writer.println(sourceAddress + "," + destinationAddress + "," +
                                       String.format("%.2f", distance) + "," +
                                       String.format("%.2f", fare) + "," +
                                       String.format("%.2f", taxAmount) + "," +
                                       String.format("%.2f", fare + taxAmount) + "," +
                                       String.format("%.2f", taxPercent) + "," +
                                       new java.util.Date());
                    }

                    // Store results in MySQL database
                    storeResultsInDatabase(sourceAddress, destinationAddress, distance, fare, taxAmount, taxPercent);

                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for the Search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchSource = searchSourceField.getText();
                String searchDestination = searchDestinationField.getText();
                searchDatabase(searchSource, searchDestination);
            }
        });

        // Action Listener for the Edit button
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int id = Integer.parseInt(editDeleteIdField.getText());
                    String newSource = searchSourceField.getText();
                    String newDestination = searchDestinationField.getText();
                    double newBaseRate = Double.parseDouble(baseRateField.getText());
                    double newRate = Double.parseDouble(rateField.getText());
                    double newTaxPercent = Double.parseDouble(taxField.getText());

                    editRecord(id, newSource, newDestination, newBaseRate, newRate, newTaxPercent);
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for the Delete button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int id = Integer.parseInt(editDeleteIdField.getText());
                    deleteRecord(id);
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for font changes
        fontDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFont(frame, new Font((String) fontDropdown.getSelectedItem(), Font.PLAIN, Integer.parseInt((String) fontSizeDropdown.getSelectedItem())));
            }
        });

        fontSizeDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFont(frame, new Font((String) fontDropdown.getSelectedItem(), Font.PLAIN, Integer.parseInt((String) fontSizeDropdown.getSelectedItem())));
            }
        });

        frame.setVisible(true);
    }

    private static void updateFont(Container container, Font font) {
        for (Component component : container.getComponents()) {
            component.setFont(font);
            if (component instanceof Container) {
                updateFont((Container) component, font);
            }
        }
    }

    private static void storeResultsInDatabase(String source, String destination, double distance, double fare, double tax, double taxPercent) {
        // Database connection details
        String url = "jdbc:mysql://localhost:3306/taxi_meter";
        String user = "root";
        String password = "password";

        // SQL insert query
        String query = "INSERT INTO records (source, destination, distance, fare, tax, total_fare, tax_percent, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, source);
            pstmt.setString(2, destination);
            pstmt.setDouble(3, distance);
            pstmt.setDouble(4, fare);
            pstmt.setDouble(5, tax);
            pstmt.setDouble(6, fare + tax);
            pstmt.setDouble(7, taxPercent);
            pstmt.setString(8, new java.util.Date().toString());

            pstmt.executeUpdate();
        } catch (Exception e) {
            resultArea.setText("Database Error: " + e.getMessage());
        }
    }

    private static void searchDatabase(String source, String destination) {
        String url = "jdbc:mysql://localhost:3306/taxi_meter";
        String user = "root";
        String password = "password";

        String query = "SELECT * FROM records WHERE source LIKE ? AND destination LIKE ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, "%" + source + "%");
            pstmt.setString(2, "%" + destination + "%");

            ResultSet rs = pstmt.executeQuery();
            StringBuilder result = new StringBuilder("Search Results:\n");
            while (rs.next()) {
                result.append("ID: ").append(rs.getInt("id")).append("\n")
                      .append("Source: ").append(rs.getString("source")).append("\n")
                      .append("Destination: ").append(rs.getString("destination")).append("\n")
                      .append("Distance: ").append(rs.getDouble("distance")).append(" km\n")
                      .append("Fare: ").append(rs.getDouble("fare")).append("\n")
                      .append("Tax: ").append(rs.getDouble("tax")).append("\n")
                      .append("Total Fare: ").append(rs.getDouble("total_fare")).append("\n")
                      .append("Tax Percent: ").append(rs.getDouble("tax_percent")).append("\n")
                      .append("Date: ").append(rs.getString("date")).append("\n\n");
            }
            resultArea.setText(result.toString());
        } catch (Exception e) {
            resultArea.setText("Database Error: " + e.getMessage());
        }
    }

    private static void editRecord(int id, String newSource, String newDestination, double newBaseRate, double newRate, double newTaxPercent) {
        String url = "jdbc:mysql://localhost:3306/taxi_meter";
        String user = "root";
        String password = "password";

        String query = "UPDATE records SET source = ?, destination = ?, fare = ?, tax_percent = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newSource);
            pstmt.setString(2, newDestination);
            pstmt.setDouble(3, newBaseRate);
            pstmt.setDouble(4, newRate);
            pstmt.setDouble(5, newTaxPercent);
            pstmt.setInt(6, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                resultArea.setText("Record updated successfully.");
            } else {
                resultArea.setText("No record found with ID: " + id);
            }
        } catch (Exception e) {
            resultArea.setText("Database Error: " + e.getMessage());
        }
    }

    private static void deleteRecord(int id) {
        String url = "jdbc:mysql://localhost:3306/taxi_meter";
        String user = "root";
        String password = "password";

        String query = "DELETE FROM records WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                resultArea.setText("Record deleted successfully.");
            } else {
                resultArea.setText("No record found with ID: " + id);
            }
        } catch (Exception e) {
            resultArea.setText("Database Error: " + e.getMessage());
        }
    }
}
    
*/






/* 
//code without edit delete functionality
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONObject;

public class TaxiMeter {
    private static double baseRate = 10.0; // Default base fare in currency units
    private static double taxPercent = 0.0; // Default tax percent
    private static JFrame frame;
    private static JTextArea resultArea;

    public static void main(String[] args) {

        frame = new JFrame("DIGITAL TAXI METER");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout()); // Use GridBagLayout for more control over component placement
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Font settings
        String[] fontOptions = {"Arial", "Verdana", "Times New Roman"};
        String[] fontSizeOptions = {"12", "14", "16", "18", "20"};
        
        JComboBox<String> fontDropdown = new JComboBox<>(fontOptions);
        JComboBox<String> fontSizeDropdown = new JComboBox<>(fontSizeOptions);
        
        // Inputs
        JLabel sourceLabel = new JLabel("Enter Source Address (e.g., street name, city, and country...):");
        JTextField sourceField = new JTextField("");
        
        JLabel destinationLabel = new JLabel("Enter Destination Address (e.g., street name, city, and country...):");
        JTextField destinationField = new JTextField("");
        
        JLabel baseRateLabel = new JLabel("First Kilometre Fare (Rs.):");
        JTextField baseRateField = new JTextField(""); // Default value for base rate

        JLabel rateLabel = new JLabel("Subsequent Kilometre Fare (Rs.) :");
        JTextField rateField = new JTextField(""); // Default value for rate
        
        JLabel taxLabel = new JLabel("Tax Percent (e.g., 10.0 ):");
        JTextField taxField = new JTextField(""); // Default value for tax percent
        
        JButton calculateButton = new JButton("Calculate Total Fare");
        
        // Search feature
        JLabel searchLabel = new JLabel("Search record by Source and Destination Address:");
        JTextField searchSourceField = new JTextField("");
        JTextField searchDestinationField = new JTextField("");
        JButton searchButton = new JButton("Search");
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        // Placing components in the frame
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(sourceLabel, gbc);
        
        gbc.gridx = 1;
        frame.add(sourceField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(destinationLabel, gbc);
        
        gbc.gridx = 1;
        frame.add(destinationField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(baseRateLabel, gbc);
        
        gbc.gridx = 1;
        frame.add(baseRateField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        frame.add(rateLabel, gbc);
        
        gbc.gridx = 1;
        frame.add(rateField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        frame.add(taxLabel, gbc);
        
        gbc.gridx = 1;
        frame.add(taxField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        frame.add(calculateButton, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        frame.add(resultArea, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        frame.add(searchLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 8;
        frame.add(new JLabel("Source Address:"), gbc);
        
        gbc.gridx = 1;
        frame.add(searchSourceField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 9;
        frame.add(new JLabel("Destination Address:"), gbc);
        
        gbc.gridx = 1;
        frame.add(searchDestinationField, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 10;
        frame.add(searchButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 11;
        frame.add(new JLabel("Choose Font:"), gbc);
        
        gbc.gridx = 1;
        frame.add(fontDropdown, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 12;
        frame.add(new JLabel("Choose Font Size:"), gbc);
        
        gbc.gridx = 1;
        frame.add(fontSizeDropdown, gbc);

        // Action Listener for the Calculate button
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get addresses and rates from input fields
                    String sourceAddress = sourceField.getText();
                    String destinationAddress = destinationField.getText();
                    double customRate = Double.parseDouble(rateField.getText());
                    baseRate = Double.parseDouble(baseRateField.getText());
                    taxPercent = Double.parseDouble(taxField.getText());

                    // Get distance from GeocodingService
                    String matrixResponse = GeocodingService.getDistanceMatrix(sourceAddress, destinationAddress);
                    JSONObject matrixJson = new JSONObject(matrixResponse);

                    // Check if the response contains valid data
                    if (!matrixJson.getString("status").equals("OK")) {
                        throw new Exception("Error in API response: " + matrixJson.getString("status"));
                    }

                    // Extracting distance information
                    double distance = matrixJson.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0)
                                                .getJSONObject("distance")
                                                .getDouble("value") / 1000; // Convert meters to km

                    // Calculate fare
                    double fare = baseRate + (distance * customRate);
                    double taxAmount = (fare * taxPercent) / 100;

                    // Display results with two decimal places
                    resultArea.setText(String.format("Distance: %.2f km\n" +
                                                     "Fare: Rs. %.2f\n" +
                                                     "Tax: Rs. %.2f\n" +
                                                     "Total Fare: Rs. %.2f\n",
                                                     distance, fare, taxAmount, fare + taxAmount));

                    // Store results in CSV file
                    try (PrintWriter writer = new PrintWriter(new FileWriter("taxi_meter_log.csv", true))) {
                        writer.println(sourceAddress + "," + destinationAddress + "," +
                                       String.format("%.2f", distance) + "," +
                                       String.format("%.2f", fare) + "," +
                                       String.format("%.2f", taxAmount) + "," +
                                       String.format("%.2f", fare + taxAmount) + "," +
                                       String.format("%.2f", taxPercent));
                    }

                    // Store results in MySQL database
                    storeResultsInDatabase(sourceAddress, destinationAddress, distance, fare, taxAmount, taxPercent);

                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for the Search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchSource = searchSourceField.getText();
                String searchDestination = searchDestinationField.getText();
                searchDatabase(searchSource, searchDestination);
            }
        });

        // Action Listener for font and size dropdowns
        ActionListener fontChangeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFont = (String) fontDropdown.getSelectedItem();
                int selectedSize = Integer.parseInt((String) fontSizeDropdown.getSelectedItem());
                Font newFont = new Font(selectedFont, Font.PLAIN, selectedSize);
                updateFont(frame, newFont);
            }
        };

        fontDropdown.addActionListener(fontChangeListener);
        fontSizeDropdown.addActionListener(fontChangeListener);

        frame.setVisible(true);
    }

    private static void updateFont(Container container, Font newFont) {
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                updateFont((Container) component, newFont);
            }
            component.setFont(newFont);
        }
    }

    private static void storeResultsInDatabase(String sourceAddress, String destinationAddress, double distance,
                                               double fare, double taxAmount, double taxPercent) throws Exception {
        String url = "jdbc:mysql://localhost:3306/taxi_db";
        String user = "root";
        String password = "shaanrout@123";
        Connection conn = DriverManager.getConnection(url, user, password);
        String query = "INSERT INTO taxi_fares (source_address, destination_address, distance_km, fare, tax_amount, total_fare, tax_percent) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, sourceAddress);
        stmt.setString(2, destinationAddress);
        stmt.setDouble(3, distance);
        stmt.setDouble(4, fare);
        stmt.setDouble(5, taxAmount);
        stmt.setDouble(6, fare + taxAmount);
        stmt.setDouble(7, taxPercent);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    private static void searchDatabase(String sourceAddress, String destinationAddress) {
        try {
            String url = "jdbc:mysql://localhost:3306/taxi_db";
            String user = "root";
            String password = "shaanrout@123";
            Connection conn = DriverManager.getConnection(url, user, password);
            String query = "SELECT * FROM taxi_fares WHERE source_address = ? AND destination_address = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, sourceAddress);
            stmt.setString(2, destinationAddress);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double distance = rs.getDouble("distance_km");
                double fare = rs.getDouble("fare");
                double taxAmount = rs.getDouble("tax_amount");
                double totalFare = rs.getDouble("total_fare");
                double taxPercent = rs.getDouble("tax_percent");

                resultArea.setText(String.format("Search Result:\n" +
                                                 "Distance: %.2f km\n" +
                                                 "Fare: Rs. %.2f\n" +
                                                 "Tax: Rs. %.2f\n" +
                                                 "Total Fare: Rs. %.2f\n" +
                                                 "Tax Percent: %.2f%%\n",
                                                 distance, fare, taxAmount, totalFare, taxPercent));
            } else {
                resultArea.setText("No record found for the provided addresses.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }
}
*/







/*
//basic unstructured without search field
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.json.JSONObject;

public class TaxiMeter {
    private static double baseRate = 10.0; // Default base fare in currency units
    private static double taxPercent = 0.0; // Default tax percent
    private static JFrame frame;
    private static JTextArea resultArea;

    public static void main(String[] args) {
        frame = new JFrame("DIGITAL TAXI METER");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(10, 2)); // Adjusted layout to include dropdowns

        // Font settings
        String[] fontOptions = {"Arial", "Verdana", "Times New Roman"};
        String[] fontSizeOptions = {"12", "14", "16", "18", "20"};
        
        JComboBox<String> fontDropdown = new JComboBox<>(fontOptions);
        JComboBox<String> fontSizeDropdown = new JComboBox<>(fontSizeOptions);
        
        // Inputs
        JLabel sourceLabel = new JLabel("Enter Source Address (e.g., street name, city, and country...):");
        JTextField sourceField = new JTextField("");
        
        JLabel destinationLabel = new JLabel("Enter Destination Address (e.g., street name, city, and country...):");
        JTextField destinationField = new JTextField("");
        
        JLabel baseRateLabel = new JLabel("First Kilometre Fare (Rs.):");
        JTextField baseRateField = new JTextField(""); // Default value for base rate

        JLabel rateLabel = new JLabel("Subsequent Kilometre Fare (Rs.) :");
        JTextField rateField = new JTextField(""); // Default value for rate
        
        
        JLabel taxLabel = new JLabel("Tax Percent (e.g., 10.0 ):");
        JTextField taxField = new JTextField(""); // Default value for tax percent
        
        JButton calculateButton = new JButton("Calculate Total Fare");
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        // Adding components to the frame
        
        frame.add(sourceLabel);
        frame.add(sourceField);
        frame.add(destinationLabel);
        frame.add(destinationField);
        frame.add(rateLabel);
        frame.add(rateField);
        frame.add(baseRateLabel);
        frame.add(baseRateField);
        frame.add(taxLabel);
        frame.add(taxField);
        frame.add(calculateButton);
        frame.add(resultArea);
        frame.add(new JLabel("Choose Font:"));
        frame.add(fontDropdown);
        frame.add(new JLabel("Choose Font Size:"));
        frame.add(fontSizeDropdown);

        // Action Listener for the Calculate button
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get addresses and rates from input fields
                    String sourceAddress = sourceField.getText();
                    String destinationAddress = destinationField.getText();
                    double customRate = Double.parseDouble(rateField.getText());
                    baseRate = Double.parseDouble(baseRateField.getText());
                    taxPercent = Double.parseDouble(taxField.getText());

                    // Get distance from GeocodingService
                    String matrixResponse = GeocodingService.getDistanceMatrix(sourceAddress, destinationAddress);
                    JSONObject matrixJson = new JSONObject(matrixResponse);

                    // Check if the response contains valid data
                    if (!matrixJson.getString("status").equals("OK")) {
                        throw new Exception("Error in API response: " + matrixJson.getString("status"));
                    }

                    // Extracting distance and traffic information
                    double distance = matrixJson.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0)
                                                .getJSONObject("distance")
                                                .getDouble("value") / 1000; // Convert meters to km

                    // Calculate fare
                    double fare = baseRate + (distance * customRate);
                    double taxAmount = (fare * taxPercent) / 100;
                    

                    // Display results with two decimal places
                    resultArea.setText(String.format("Distance: %.2f km\n" +
                                                     "Fare: Rs. %.2f\n" +
                                                     "Tax: Rs. %.2f\n" +
                                                     "Total Fare: Rs. %.2f\n",
                                                     distance, fare, taxAmount, fare+taxAmount));

                    // Store results in CSV file
                    try (PrintWriter writer = new PrintWriter(new FileWriter("taxi_meter_log.csv", true))) {
                        writer.println(sourceAddress + "," + destinationAddress + "," +
                                       String.format("%.2f", distance) + "," +
                                       String.format("%.2f", fare) + "," +
                                       String.format("%.2f", taxAmount) + "," +
                                       String.format("%.2f", fare+taxAmount)+ ","+
                                       String.format("%.2f", taxPercent));
                    }

                    // Store results in MySQL database
                    storeResultsInDatabase(sourceAddress, destinationAddress, distance, fare, taxAmount,taxPercent);

                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for font and size dropdowns
        ActionListener fontChangeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFont = (String) fontDropdown.getSelectedItem();
                int selectedSize = Integer.parseInt((String) fontSizeDropdown.getSelectedItem());
                Font newFont = new Font(selectedFont, Font.PLAIN, selectedSize);
                updateFont(frame, newFont);
            }
        };

        fontDropdown.addActionListener(fontChangeListener);
        fontSizeDropdown.addActionListener(fontChangeListener);

        frame.setVisible(true);
    }

    private static void updateFont(Container container, Font newFont) {
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                updateFont((Container) component, newFont);
            }
            component.setFont(newFont);
        }
    }

    private static void storeResultsInDatabase(String sourceAddress, String destinationAddress, double distance,
                                               double fare, double taxAmount, double taxPercent) throws Exception {
        String url = "jdbc:mysql://localhost:3306/taxi_db";
        String user = "root";
        String password = "shaanrout@123";
        Connection conn = DriverManager.getConnection(url, user, password);
        String query = "INSERT INTO taxi_fares (source_address, destination_address, distance_km, fare, tax_amount, total_fare, tax_percent) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, sourceAddress);
        stmt.setString(2, destinationAddress);
        stmt.setDouble(3, distance);
        stmt.setDouble(4, fare);
        stmt.setDouble(5, taxAmount);
        stmt.setDouble(6, fare+taxAmount);
        stmt.setDouble(7, taxPercent);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }
}


//updated with search field


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONObject;

public class TaxiMeter {
    private static double baseRate = 10.0; // Default base fare in currency units
    private static double taxPercent = 0.0; // Default tax percent
    private static JFrame frame;
    private static JTextArea resultArea;

    public static void main(String[] args) {

        frame = new JFrame("DIGITAL TAXI METER");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(12, 2)); // Adjusted layout to include search fields

        // Font settings
        String[] fontOptions = {"Arial", "Verdana", "Times New Roman"};
        String[] fontSizeOptions = {"12", "14", "16", "18", "20"};
        
        JComboBox<String> fontDropdown = new JComboBox<>(fontOptions);
        JComboBox<String> fontSizeDropdown = new JComboBox<>(fontSizeOptions);
        
        // Inputs
        JLabel sourceLabel = new JLabel("Enter Source Address (e.g., street name, city, and country...):");
        JTextField sourceField = new JTextField("");
        
        JLabel destinationLabel = new JLabel("Enter Destination Address (e.g., street name, city, and country...):");
        JTextField destinationField = new JTextField("");
        
        JLabel baseRateLabel = new JLabel("First Kilometre Fare (Rs.):");
        JTextField baseRateField = new JTextField(""); // Default value for base rate

        JLabel rateLabel = new JLabel("Subsequent Kilometre Fare (Rs.) :");
        JTextField rateField = new JTextField(""); // Default value for rate
        
        JLabel taxLabel = new JLabel("Tax Percent (e.g., 10.0 ):");
        JTextField taxField = new JTextField(""); // Default value for tax percent
        
        JButton calculateButton = new JButton("Calculate Total Fare");
        
        // Search feature
        JLabel searchLabel = new JLabel("Search record by Source and Destination Address:");
        JTextField searchSourceField = new JTextField("");
        JTextField searchDestinationField = new JTextField("");
        JButton searchButton = new JButton("Search");
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        // Adding components to the frame
        frame.add(sourceLabel);
        frame.add(sourceField);
        frame.add(destinationLabel);
        frame.add(destinationField);
        frame.add(baseRateLabel);
        frame.add(baseRateField);
        frame.add(rateLabel);
        frame.add(rateField);
        frame.add(taxLabel);
        frame.add(taxField);
        frame.add(calculateButton);
        frame.add(resultArea);
        frame.add(searchLabel);
        frame.add(new JLabel("Source Address:"));
        frame.add(searchSourceField);
        frame.add(new JLabel("Destination Address:"));
        frame.add(searchDestinationField);
        frame.add(searchButton);
        frame.add(new JLabel("Choose Font:"));
        frame.add(fontDropdown);
        frame.add(new JLabel("Choose Font Size:"));
        frame.add(fontSizeDropdown);

        // Action Listener for the Calculate button
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Get addresses and rates from input fields
                    String sourceAddress = sourceField.getText();
                    String destinationAddress = destinationField.getText();
                    double customRate = Double.parseDouble(rateField.getText());
                    baseRate = Double.parseDouble(baseRateField.getText());
                    taxPercent = Double.parseDouble(taxField.getText());

                    // Get distance from GeocodingService
                    String matrixResponse = GeocodingService.getDistanceMatrix(sourceAddress, destinationAddress);
                    JSONObject matrixJson = new JSONObject(matrixResponse);

                    // Check if the response contains valid data
                    if (!matrixJson.getString("status").equals("OK")) {
                        throw new Exception("Error in API response: " + matrixJson.getString("status"));
                    }

                    // Extracting distance information
                    double distance = matrixJson.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0)
                                                .getJSONObject("distance")
                                                .getDouble("value") / 1000; // Convert meters to km

                    // Calculate fare
                    double fare = baseRate + (distance * customRate);
                    double taxAmount = (fare * taxPercent) / 100;

                    // Display results with two decimal places
                    resultArea.setText(String.format("Distance: %.2f km\n" +
                                                     "Fare: Rs. %.2f\n" +
                                                     "Tax: Rs. %.2f\n" +
                                                     "Total Fare: Rs. %.2f\n",
                                                     distance, fare, taxAmount, fare + taxAmount));

                    // Store results in CSV file
                    try (PrintWriter writer = new PrintWriter(new FileWriter("taxi_meter_log.csv", true))) {
                        writer.println(sourceAddress + "," + destinationAddress + "," +
                                       String.format("%.2f", distance) + "," +
                                       String.format("%.2f", fare) + "," +
                                       String.format("%.2f", taxAmount) + "," +
                                       String.format("%.2f", fare + taxAmount) + "," +
                                       String.format("%.2f", taxPercent));
                    }

                    // Store results in MySQL database
                    storeResultsInDatabase(sourceAddress, destinationAddress, distance, fare, taxAmount, taxPercent);

                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Action Listener for the Search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchSource = searchSourceField.getText();
                String searchDestination = searchDestinationField.getText();
                searchDatabase(searchSource, searchDestination);
            }
        });

        // Action Listener for font and size dropdowns
        ActionListener fontChangeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFont = (String) fontDropdown.getSelectedItem();
                int selectedSize = Integer.parseInt((String) fontSizeDropdown.getSelectedItem());
                Font newFont = new Font(selectedFont, Font.PLAIN, selectedSize);
                updateFont(frame, newFont);
            }
        };

        fontDropdown.addActionListener(fontChangeListener);
        fontSizeDropdown.addActionListener(fontChangeListener);

        frame.setVisible(true);
    }

    private static void updateFont(Container container, Font newFont) {
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                updateFont((Container) component, newFont);
            }
            component.setFont(newFont);
        }
    }

    private static void storeResultsInDatabase(String sourceAddress, String destinationAddress, double distance,
                                               double fare, double taxAmount, double taxPercent) throws Exception {
        String url = "jdbc:mysql://localhost:3306/taxi_db";
        String user = "root";
        String password = "shaanrout@123";
        Connection conn = DriverManager.getConnection(url, user, password);
        String query = "INSERT INTO taxi_fares (source_address, destination_address, distance_km, fare, tax_amount, total_fare, tax_percent) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, sourceAddress);
        stmt.setString(2, destinationAddress);
        stmt.setDouble(3, distance);
        stmt.setDouble(4, fare);
        stmt.setDouble(5, taxAmount);
        stmt.setDouble(6, fare + taxAmount);
        stmt.setDouble(7, taxPercent);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    private static void searchDatabase(String sourceAddress, String destinationAddress) {
        try {
            String url = "jdbc:mysql://localhost:3306/taxi_db";
            String user = "root";
            String password = "shaanrout@123";
            Connection conn = DriverManager.getConnection(url, user, password);

            String query = "SELECT * FROM taxi_fares WHERE source_address = ? AND destination_address = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, sourceAddress);
            stmt.setString(2, destinationAddress);
            ResultSet rs = stmt.executeQuery();

            StringBuilder results = new StringBuilder("Search Results:\n");

            while (rs.next()) {
                results.append(String.format("Source: %s, Destination: %s, Distance: %.2f km, Fare: Rs. %.2f, Tax: Rs. %.2f, Total Fare: Rs. %.2f, Tax Percent: %.2f%%\n",
                                             rs.getString("source_address"),
                                             rs.getString("destination_address"),
                                             rs.getDouble("distance_km"),
                                             rs.getDouble("fare"),
                                             rs.getDouble("tax_amount"),
                                             rs.getDouble("total_fare"),
                                             rs.getDouble("tax_percent")));
            }

            if (results.toString().equals("Search Results:\n")) {
                resultArea.setText("No matching records found.");
            } else {
                resultArea.setText(results.toString());
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }
}
*/
