package org.yangxin.net;

import org.yangxin.BootStrap;
import org.yangxin.http.Constant;
import org.yangxin.http.HttpRequest;
import org.yangxin.http.HttpResponse;
import org.yangxin.http.HttpStatus;
import org.yangxin.io.DogInputStream;
import org.yangxin.io.DogOutputStream;
import org.yangxin.pool.PooledObject;
import org.yangxin.until.ByteUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yangxin
 */

public class NioEndPoint {

    private byte[] page404;
    private byte[] pageUpload;
    private Map<String, byte[]> resourcesMap = new HashMap<>();
    private ServerSocketChannel serverSocketChannel;
    private Executor executor;
    private Poller[] pollers;
    private AtomicInteger pollerRotater = new AtomicInteger(0);
    private PooledObject<SocketEvent> pooledObject = new PooledObject(SocketEvent.class);

    public NioEndPoint() throws IOException{
        executor = new ThreadPoolExecutor(1, Constant.COMPUTOR_CORE, 600, TimeUnit.SECONDS, new LinkedBlockingQueue(), Executors.defaultThreadFactory(),new ThreadPoolExecutor.AbortPolicy());
        pollers = new Poller[Constant.COMPUTOR_CORE*2];
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("html/404.html");
        page404 = new byte[is.available()];
        if ( -1 == is.read(page404)) {
            throw new IOException("读取404页面报错");
        }
        resourcesMap.put("/404.html", page404);
        is = Thread.currentThread().getContextClassLoader().getResourceAsStream("html/upload.html");
        pageUpload = new byte[is.available()];
        if ( -1 == is.read(pageUpload)) {
            throw new IOException("读取404页面报错");
        }
        resourcesMap.put("/upload.html", pageUpload);
    }

    public void bind(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port), Constant.COMPUTOR_CORE*50);
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.socket().setSoTimeout(Constant.DEFAULT_REQSUT_TIME_OUT);
    }

    public void start(){
        new Thread(new Acceptor()).start();
        for (int i = pollers.length - 1; i >= 0; i--) {
            pollers[i] = new Poller();
            try {
                pollers[i].setSelector(Selector.open());
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(pollers[i]).start();
        }

    }

    protected class Acceptor implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    SocketChannel socketChannel = serverSocketChannel.accept();
//                    selector.wakeup();
//                    System.out.println("http访问接入");
                    socketChannel.configureBlocking(false);
                    SelectionKey key = socketChannel.register(getPoller0().getSelector(), SelectionKey.OP_READ);
                    SocketEvent socketEvent = pooledObject.borrowObject();
                    socketEvent.setPooledObject(pooledObject);
                    socketEvent.recyle();
                    socketEvent.buildSocketChannel(socketChannel);
                    key.attach(socketEvent);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
            }
        }
    }

    protected class Poller implements Runnable{
        private Selector selector;

        @Override
        public void run() {

            while (true) {
                try {
                    if (selector.select(10) > 0) {
                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        while (it.hasNext()) {
                            processKey(it.next());
                            it.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

        private void processKey(SelectionKey key) {

            SocketEvent socketEvent = (SocketEvent)key.attachment();
            if (!key.isValid()){
                return;
            }
            if ((key.readyOps() & SelectionKey.OP_READ)
                    == SelectionKey.OP_READ) {
                parseHttpRequest(socketEvent);
                key.interestOps(SelectionKey.OP_WRITE);
            }

            if ((key.readyOps() & SelectionKey.OP_WRITE)
                    == SelectionKey.OP_WRITE) {
                try {
                    prepareResponse(socketEvent);
                    writeFile(socketEvent);
                    socketEvent.getResponse().getOutputStream().write(Constant.CR);
                    socketEvent.getResponse().getOutputStream().write(Constant.LF);
                    socketEvent.getResponse().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    socketEvent.back();
                    socketEvent.getResponse().close();
                }
            }

        }

        private void writeFile(SocketEvent socketEvent) throws IOException {
            HttpResponse response = socketEvent.getResponse();
            int size = 0;
            byte[] b = new byte[8 * 1024];
            if ( HttpStatus.NotFound == response.getHttpStatus()) {
//                System.out.println(new String(page404));
                response.getOutputStream().write(page404, 0, page404.length);
            }

            if (HttpStatus.RESOURCES == response.getHttpStatus()) {
                String resourcesUrl = getResourcesUrl(socketEvent.getRequest().getRequestUri());
                response.getOutputStream().write(resourcesMap.get(resourcesUrl), 0, resourcesMap.get(resourcesUrl).length);
            }

            if (response.getHttpStatus() == HttpStatus.OK) {
                RandomAccessFile raf = new RandomAccessFile(socketEvent.getPath().toFile(), "r");

                while( -1 !=(size=raf.read(b)) ) {
                    response.getOutputStream().write(b, 0, size);
                }
            }
        }

        private String getResourcesUrl(String url){
            return resourcesMap.keySet().stream().filter( s -> {
                return url.endsWith(s);
            }).findFirst().get();
        }

        private void prepareResponse(SocketEvent socketEvent) throws IOException {
            HttpResponse response = socketEvent.getResponse();
            String url = socketEvent.getRequest().getRequestUri();
            Path path = Paths.get(BootStrap.root+url.replace('/', File.separatorChar));
            socketEvent.setPath(path);
            long ContentLength = 0;
            if (Files.exists(path)){
                response.setHttpStatus(HttpStatus.OK);
                ContentLength = path.toFile().length();
            } else if(null!=url&&url.startsWith(Constant.DEFAULT_PATH)){
                String resourcesUrl = getResourcesUrl(url);
                if (null != resourcesUrl) {
                    response.setHttpStatus(HttpStatus.RESOURCES);
                    ContentLength = resourcesMap.get(resourcesUrl).length;
                }

            }
            if ( 0==ContentLength ){
                ContentLength = page404.length;
                socketEvent.setBadResponse(HttpStatus.NotFound, "请求资源不存在");
            }
//            preparedResponseLine();
            response.getOutputStream().write("HTTP/1.1 ".getBytes());
            response.getOutputStream().write(response.getHttpStatus().getStatCode());
            response.getOutputStream().write(Constant.SP);
            response.getOutputStream().write(response.getHttpStatus().name().getBytes());
            response.getOutputStream().write(Constant.CRLF);
            response.addHeader("X-Server", "tomdog 1.0");
//            response.addHeader("Content-Type", "text/html");
            response.addHeader("Content-Length", String.valueOf(ContentLength));
            response.getOutputStream().write(Constant.CRLF);
            response.getOutputStream().write(Constant.CRLF);

        }

        private void parseHttpRequest(SocketEvent socketEvent) {
            HttpRequest request = new HttpRequest(new DogInputStream(socketEvent.getSocketChannel()));
            HttpResponse response = new HttpResponse(new DogOutputStream(socketEvent.getSocketChannel()));
            socketEvent.setRequest(request);
            socketEvent.setResponse(response);
            SocketChannel socketChannel = socketEvent.getSocketChannel();
            ByteBuffer byteBuffer = socketEvent.getByteBuffer();

            byte preChr = 0;
            byte chr = 0;
            byte[] tmp = new byte[8 * 1024];
            int length = 0;
            int offset = 0;
            int pos = 0;

            //1 解析请求行 8k请求行和header
            try {
                byteBuffer.clear();
                socketChannel.read(byteBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byteBuffer.flip();
            while (Constant.LF != chr && Constant.CR != preChr && byteBuffer.hasRemaining()) {
                preChr = chr;
                chr = byteBuffer.get();
                tmp[pos++] = chr;
                length++;
            }
            request.buildHttpLine(tmp, offset, length);

            //2 解析headers
            offset += length;
            length = 0;
            while (true) {
                preChr = 0;
                chr = 0;
                while (Constant.LF != chr && Constant.CR != preChr && byteBuffer.hasRemaining()) {
                    preChr = chr;
                    chr = byteBuffer.get();
                    tmp[pos++] = chr;
                    length++;
                }
                if (2 == length) {
                    break;
                }
                request.addHeader(tmp, offset, length);
                offset += length;
                length = 0;
            }
        }

        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }
    }

    public Poller getPoller0() {
        return pollers[Math.abs(pollerRotater.incrementAndGet()) % pollers.length];
    }


}
