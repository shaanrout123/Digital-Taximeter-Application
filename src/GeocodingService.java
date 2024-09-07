import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeocodingService {
    
    private static final String MATRIX_API_KEY = "AIzaSyBvMiUi5UZ6XB4gEy1VYV81XIqJ-fBLE4U";
    
    public static String getDistanceMatrix(String origins, String destinations) throws Exception {
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" 
                     + origins.replace(" ", "%20") 
                     + "&destinations=" + destinations.replace(" ", "%20") 
                     + "&key=" + MATRIX_API_KEY;
        return sendGetRequest(url);
    }
    
    @SuppressWarnings("deprecation")
    private static String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }
}
