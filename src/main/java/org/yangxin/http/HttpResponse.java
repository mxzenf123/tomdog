package org.yangxin.http;


import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class HttpResponse {

    private HttpStatus httpStatus;
    private OutputStream outputStream;

    public HttpResponse(OutputStream outputStream){
        this.outputStream = outputStream;
    }

    public HttpResponse addHeader(String k, String v){
        try {
            outputStream.write((k+":").getBytes());
            outputStream.write(v.getBytes());
            outputStream.write(Constant.CR);
            outputStream.write(Constant.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void flush() throws IOException {

        outputStream.flush();
    }

    public void close(){
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
