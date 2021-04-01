package org.yangxin.net;

import org.yangxin.BootStrap;
import org.yangxin.http.Constant;
import org.yangxin.http.HttpRequest;
import org.yangxin.http.HttpResponse;
import org.yangxin.http.HttpStatus;
import org.yangxin.pool.PooledObject;
import org.yangxin.until.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yangxin
 */
public class SocketEvent {

    private PooledObject pooledObject;
    private SocketChannel socketChannel;
    private HttpRequest request;
    private HttpResponse response;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(8 * 1024);//8k
    private Path path;

    public void recyle(){
        socketChannel = null;
        request = null;
        response = null;
        byteBuffer.clear();
        path = null;
    }

    public SocketEvent buildSocketChannel(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
        return this;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public PooledObject getPooledObject() {
        return pooledObject;
    }

    public void setPooledObject(PooledObject pooledObject) {
        this.pooledObject = pooledObject;
    }

    public void flush(){

    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setBadResponse(HttpStatus httpStatus, String msg){
        response.setHttpStatus(httpStatus);
    }

    public void back(){
        this.pooledObject.backObject(this);
    }
}
