package net.iobook.noterf;

import android.app.IntentService;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * Created by olivier on 19/02/16.
 * HTTP methods used by the project
 */
public class HttpHelper {

    private static final String TAG = "httpHelper";
    private static final String POST = "POST";
    private int timeOut;
    private String user;
    private String password;

    HttpHelper(String user, String password, int timeOut) throws IOException {
        this.timeOut = timeOut;
        this.user = user;
        this.password = password;
    }


    public HttpResultHelper post(String urlStr, String data, ArrayList<String[]> headers) throws IOException
    {
        // Set url
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // If secure connection
        if (urlStr.startsWith("https")) {
            try {
                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());
                ((HttpsURLConnection)conn).setSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                Log.d(TAG, "Failed to construct SSL object", e);
            }
        }


        // Use this if you need basic authentication
        if ((this.user != null) && (this.password != null)) {
            String userPass = this.user + ":" + this.password;
            String basicAuth = "Basic " + Base64.encodeToString(userPass.getBytes(), Base64.DEFAULT);
            conn.setRequestProperty("Authorization", basicAuth);
        }

        // Set Timeout and method
        conn.setReadTimeout(this.timeOut);
        conn.setConnectTimeout(this.timeOut);
        conn.setRequestMethod(POST);
        conn.setDoOutput(true);

        // Set headers
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                conn.setRequestProperty(headers.get(i)[0], headers.get(i)[1]);
            }
        }

        if (data != null) {
            conn.setFixedLengthStreamingMode(data.getBytes().length);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            os.close();
        }

        InputStream inputStream = null;
        try
        {
            inputStream = conn.getInputStream();
        }
        catch(IOException exception)
        {
            inputStream = conn.getErrorStream();
        }

        HttpResultHelper result = new HttpResultHelper();
        result.setStatusCode(conn.getResponseCode());
        result.setResponse(inputStream);

        return result;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
