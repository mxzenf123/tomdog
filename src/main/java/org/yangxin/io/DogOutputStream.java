package org.yangxin.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class DogOutputStream extends OutputStream {

    private SocketChannel socketChannel;
    private ByteBuffer bb = ByteBuffer.allocate( 8*1024 );

    public DogOutputStream(SocketChannel socketChannel){

        this.socketChannel = socketChannel;
    }

    public DogOutputStream() {
        super();
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (bb.limit()-bb.position() < len) {
            this.flush();
        }
        bb.put(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        bb.flip();
        while (bb.hasRemaining() && socketChannel.isConnected()) {
            socketChannel.write(bb);
        }
        bb.compact();
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    @Override
    public void write(int b) throws IOException {
        bb.put((byte)b);
    }
}
