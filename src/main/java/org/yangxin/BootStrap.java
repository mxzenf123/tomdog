package org.yangxin;

import org.yangxin.net.NioEndPoint;

import java.io.IOException;

public class BootStrap {

    public static String root;
    private NioEndPoint endPoint;
    private int port;

    public BootStrap(String r, int port) throws IOException{
        root = r;
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
