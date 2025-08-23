package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.Socket;

/**
 * 客户端 - 主动连接服务器
 */
public class Client {
    // 创建一个日志记录器（Logger），关联到当前Client类
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    // 服务器的地址和端口，如果是连接本机，就用 localhost 或 127.0.0.1
    private static final String SERVER_IP = "localhost";
    // 8080被广泛采纳为Web服务的默认替代端口。
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        logger.info("客户端启动，尝试连接服务器 {}:{}", SERVER_IP, SERVER_PORT);

        // 使用 try-with-resources 确保 Socket 会被自动关闭
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            logger.info("连接服务器成功！");

            // TODO: 这里将来会添加向服务器发送数据的逻辑
            // 现在我们先只是连接

            // 同样，等待一下，避免程序立刻结束
            logger.info("按回车键退出客户端...");
            System.in.read();

        } catch (IOException e) {
            logger.error("客户端连接失败", e);
        }
        logger.info("客户端已关闭。");
    }
}
