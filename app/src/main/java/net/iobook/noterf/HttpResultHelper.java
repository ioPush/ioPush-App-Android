package net.iobook.noterf;

import java.io.InputStream;

/**
 * Created by olivier on 11/02/16.
 */
public class HttpResultHelper {
    private int statusCode;
    private InputStream response;

    public HttpResultHelper() {
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public InputStream getResponse() {
        return response;
    }

    public void setResponse(InputStream response) {
        this.response = response;
    }
}
