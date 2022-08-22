import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class RedditJSONHandler {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        URL myUrl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) myUrl.openConnection();
        con.setRequestProperty("user-agent","Redditor Discord Bot Agent");
        BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String jsonText = readAll(rd);
        JSONObject json = new JSONObject(jsonText);
        rd.close();
        con.disconnect();
        return json;
    }
}
