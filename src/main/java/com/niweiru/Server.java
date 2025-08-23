package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 服务端 - 等待客户端的连接
 */
public class Server {
    // 创建一个日志记录器（Logger），关联到当前Server类
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    // 服务器监听的端口号，可以随意选一个1024以上的，这里用8080
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // 使用 try-with-resources 确保 ServerSocket 会被自动关闭
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("服务器启动成功，正在端口 {} 监听等待客户端连接...", PORT);

            // accept() 方法是一个阻塞调用，会一直等待直到有客户端连接上来
            Socket clientSocket = serverSocket.accept();
            logger.info("有客户端连接成功了！客户端地址：{}", clientSocket.getRemoteSocketAddress());

            // TODO: 这里将来会添加读取客户端发送数据的逻辑
            // 现在先暂时不读取，只是建立连接

            // 为了让服务器保持运行，加一个等待
            logger.info("按回车键退出服务器...");
            System.in.read();

        } catch (IOException e) {
            logger.error("服务器发生异常", e);
        }
        logger.info("服务器已关闭。");
    }
}
