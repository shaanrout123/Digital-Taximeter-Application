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













