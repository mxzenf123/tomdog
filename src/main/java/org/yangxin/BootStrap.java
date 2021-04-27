package org.yangxin;

import org.yangxin.net.NioEndPoint;

import java.io.IOException;

public class BootStrap {

    public static String root;
    public static String upload;
    private NioEndPoint endPoint;
    private int port;

    public BootStrap(String r, String u, int port) throws IOException{
        root = r;
        upload = u;
        this.port = port;
        endPoint = new NioEndPoint();
    }

    public void init() throws IOException {
        endPoint.bind(this.port);
    }

    public void start(){
        endPoint.start();
    }

}
