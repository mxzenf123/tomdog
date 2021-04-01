package org.yangxin;

import java.io.IOException;

/**
 * @author yangxin
 * 启动类
 */
public class MainApp
{
    public static void main( String[] args ) throws IOException
    {

        if (null == args || args.length < 2) {
            throw new RuntimeException("至少输入根路径和端口");
        }
        BootStrap bootStrap = new BootStrap(args[0], Integer.valueOf(args[1]));
        bootStrap.init();
        bootStrap.start();
        System.out.println("启动完成，服务端口：" + args[1] + "，html路径：" + args[0] );
    }
}
