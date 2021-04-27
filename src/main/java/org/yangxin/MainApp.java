package org.yangxin;

import java.io.IOException;

/**
 * @author yangxin
 * 启动类
 */
public class MainApp
{
    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main( String[] args ) throws IOException
    {
        /**
         * -p 端口
         * -u 上传目录
         * -r html目录
         */
        String htmlPath = null, uploadPath = null;
        Integer port = 8080;
        if (null == args || args.length < 6) {
            System.exit(-1);
            throw new RuntimeException("至少输入根路径,端口和上传文件目录");
        }
        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i])) {
                port = Integer.valueOf(args[i+1]);
            }

            if ("-u".equals(args[i])) {
                uploadPath = args[i+1];
            }

            if ("-r".equals(args[i])) {
                htmlPath = args[i+1];
            }
        }
        BootStrap bootStrap = new BootStrap(htmlPath, uploadPath, port);
        bootStrap.init();
        bootStrap.start();
        System.out.println("启动完成，服务端口：" + port + "，html路径：" + htmlPath + "，上传文件路径：" + uploadPath );
    }
}
