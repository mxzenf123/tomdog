package org.yangxin.net;

import org.yangxin.BootStrap;
import org.yangxin.http.Constant;
import org.yangxin.http.HttpRequest;
import org.yangxin.http.HttpResponse;
import org.yangxin.http.HttpStatus;
import org.yangxin.io.DogInputStream;
import org.yangxin.io.DogOutputStream;
import org.yangxin.pool.PooledObject;
import org.yangxin.until.KMPAlgorithm;

import java.io.*;
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
    private byte[] success;
    private byte[] ico;
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
            throw new IOException("读取upload页面报错");
        }
        resourcesMap.put("/upload.html", pageUpload);

        is = Thread.currentThread().getContextClassLoader().getResourceAsStream("html/doupload.html");
        success = new byte[is.available()];
        if ( -1 == is.read(success)) {
            throw new IOException("读取upload页面报错");
        }
        resourcesMap.put("/doupload.html", success);

        is = Thread.currentThread().getContextClassLoader().getResourceAsStream("tomdog.ico");
        ico = new byte[is.available()];
        if ( -1 == is.read(ico)) {
            throw new IOException("读取upload页面报错");
        }
        resourcesMap.put("/favicon.ico", ico);
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
                    System.out.println("http访问接入:" + socketChannel.getRemoteAddress() + "-"+(new java.util.Date(System.currentTimeMillis())));
                    socketChannel.configureBlocking(false);
                    SelectionKey key = socketChannel.register(getPoller0().getSelector(), SelectionKey.OP_READ);
                    System.out.println("注册socketChannel等待读取：" + (new java.util.Date(System.currentTimeMillis())));
                    SocketEvent socketEvent = null;
                    try {
                        socketEvent = pooledObject.borrowObject();
                        if (null == socketEvent.getPooledObject()) {
                            socketEvent.setPooledObject(pooledObject);
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                        // TODO 如果并发太大被中断返回繁忙页面
                    }
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
//                    System.out.println("准备开始选择可处理的selectKey - " + (new java.util.Date(System.currentTimeMillis())));
                    if (selector.select(1) > 0) {
                        System.out.println("开始处理selectKey - " + (new java.util.Date(System.currentTimeMillis())));
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

        private void processKey(SelectionKey key) throws IOException {

            SocketEvent socketEvent = (SocketEvent)key.attachment();
            if (!key.isValid()){
                return;
            }
            if ((key.readyOps() & SelectionKey.OP_READ)
                    == SelectionKey.OP_READ) {
                System.out.println("<------解析请求------>" + (new java.util.Date(System.currentTimeMillis())));
                parseHttpRequest(socketEvent);
                key.interestOps(SelectionKey.OP_WRITE);
            }

            if ((key.readyOps() & SelectionKey.OP_WRITE)
                    == SelectionKey.OP_WRITE) {
                try {
                    System.out.println("<------返回页面------>" + (new java.util.Date(System.currentTimeMillis())));

                    prepareResponse(socketEvent);
                    writeFile(socketEvent);
                    socketEvent.getResponse().getOutputStream().write(Constant.CRLF);
                    socketEvent.getResponse().flush();
                    System.out.println("<------请求完成------>" + (new java.util.Date(System.currentTimeMillis())));
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

            byte[] b = new byte[8 * 1024];
            if ( HttpStatus.NotFound == response.getHttpStatus()) {
//                System.out.println(new String(page404));
                response.getOutputStream().write(page404, 0, page404.length);
            }

            if (HttpStatus.RESOURCES == response.getHttpStatus()) {
                String resourcesUrl = getResourcesUrl(socketEvent.getRequest().getRequestUri());
                response.getOutputStream().write(resourcesMap.get(resourcesUrl), 0, resourcesMap.get(resourcesUrl).length);
            }

            int size = 0;
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
            long contentLength = 0;
            System.out.println("url -> " + url);
            if (Files.exists(path)){
                response.setHttpStatus(HttpStatus.OK);
                contentLength = path.toFile().length();
            } else if(null!=url && (url.startsWith(Constant.DEFAULT_PATH)) ||
                                    null!=resourcesMap.get(url)){
                String resourcesUrl = getResourcesUrl(url);
                System.out.println("资源路径 -> " + resourcesUrl);
                if (null != resourcesUrl) {
                    response.setHttpStatus(HttpStatus.RESOURCES);
                    contentLength = resourcesMap.get(resourcesUrl).length;
                }

            }

            if ( 0==contentLength ){
                contentLength = page404.length;
                socketEvent.setBadResponse(HttpStatus.NotFound, "请求资源不存在");
            }

            preparedResponseLine(response, contentLength);

        }

        private void preparedResponseLine(HttpResponse response, long ContentLength) throws IOException {
            response.getOutputStream().write("HTTP/1.1".getBytes());
            response.getOutputStream().write(Constant.SP);
            response.getOutputStream().write(response.getHttpStatus().getStatCode());
            response.getOutputStream().write(Constant.SP);
            response.getOutputStream().write(response.getHttpStatus().name().getBytes());
            response.getOutputStream().write(Constant.CRLF);
            response.addHeader("X-Server", "tomdog 1.0");
            if (ContentLength > 0) {
                response.addHeader("Content-Length", String.valueOf(ContentLength));
            }

            response.getOutputStream().write(Constant.CRLF);
            response.getOutputStream().write(Constant.CRLF);
        }

        private void parseHttpRequest(SocketEvent socketEvent) throws IOException {
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

            String header = request.getHeaders().get("Content-Type");
            if (null != header && header.indexOf("multipart/form-data") > -1) {
                System.out.println("<------开始解析multipart/form-data------>" + (new java.util.Date(System.currentTimeMillis())));
                parseMultipartData(request, socketEvent, header);
                System.out.println("<------完成解析multipart/form-data------>" + (new java.util.Date(System.currentTimeMillis())));
            }
        }

        private void parseMultipartData(HttpRequest request, SocketEvent socketEvent, String header) throws IOException {
            String[] strs = header.split(";");
            int boundaryLen = 0;
            String boundary = null;
            int[] next = null;
            for (int i = strs.length - 1; i >= 0; i--) {
                //有multipart头，但是没有bondary？
                if (null!=strs[i] && strs[i].trim().startsWith("boundary")) {
                    boundary = strs[i].split("=")[1].trim().replaceAll("\"","");
                    boundaryLen = boundary.length();
                    next = KMPAlgorithm.kmpNext(boundary);
                }
            }

            ByteBuffer byteBuffer = socketEvent.getByteBuffer();
            byte preChr = 0;
            byte chr = 0;
            byte[] tmp = new byte[8 * 1024];
            int length = 0;
            int offset = 0;
            int pos = 0;
            int size = 0;
            Map<String, String> headers = new HashMap<>(5);
            String[] headerStr;

            // 1，先解析boundary
            while (Constant.LF != chr && Constant.CR != preChr) {
                preChr = chr;
                if (readChannelIfNecessary(byteBuffer, socketEvent) > 0) {
                    chr = byteBuffer.get();
                    tmp[pos++] = chr;
                    length++;
                }

            }

            // 2，解析Conent-*
            pos = length = 0;
            while (true) {
                preChr = 0;
                chr = 0;
                while (Constant.LF != chr && Constant.CR != preChr) {
                    preChr = chr;
                    if (readChannelIfNecessary(byteBuffer, socketEvent) > 0) {
                        chr = byteBuffer.get();
                        tmp[pos++] = chr;
                        length++;
                    }

                }
                if (2 == length) {
                    break;
                }
                headerStr = new String(tmp, offset, length).split(":");
                headers.put(headerStr[0].trim(), headerStr[1].trim());
                offset += length;
                length = 0;
            }

            // 3，保存文件
            String fileName = String.valueOf(System.currentTimeMillis());
            String[] conentDispositionStr = headers.get("Content-Disposition").split(";");
            for ( int i = conentDispositionStr.length - 1; i >= 0; i--) {
                if (conentDispositionStr[i].trim().startsWith("filename")) {
                    fileName = conentDispositionStr[i].split("=")[1].replaceAll("\"","");
                }
            }
            try(FileOutputStream fos = new FileOutputStream(BootStrap.upload + File.separator + fileName)){
                pos = 0;
                while (true) {
                    size = readChannelIfNecessary(byteBuffer, socketEvent);

                    if ( size > 0) {
                        chr = byteBuffer.get();
                        tmp[pos++] = chr;
                        if ( tmp.length ==  pos) {
                            if ((offset=KMPAlgorithm.kmpSearch(tmp, boundary, next)) > -1) {
                                fos.write(tmp, 0, offset);
                                break;
                            }
                            fos.write(tmp, 0, tmp.length-boundaryLen);
                            System.arraycopy(tmp,tmp.length-boundaryLen,tmp,0,boundaryLen);
                            pos = boundaryLen;
                        }
                    } else {
                        if ((offset=KMPAlgorithm.kmpSearch(tmp, boundary, next)) > -1) {
                            fos.write(tmp, 0, offset);
                            break;
                        }
                    }

                }
            }
        }

        private int readChannelIfNecessary(ByteBuffer byteBuffer, SocketEvent socketEvent) throws IOException {
            if (!byteBuffer.hasRemaining()) {
                byteBuffer.clear();
                int size = socketEvent.getSocketChannel().read(byteBuffer);
                byteBuffer.flip();
                return size;
            }
            return byteBuffer.remaining();
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
