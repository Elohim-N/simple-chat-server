package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端 - 等待客户端的连接
 */
public class Server {
    // 创建一个日志记录器（Logger），关联到当前Server类
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    // 服务器监听的端口号，可以随意选一个1024以上的，这里用8080
    private static final int PORT = 8080;

    // 在类中添加一个线程池
    // 使用 Executors 工厂方法创建（简单方式）
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();  //无界队列

    // 如果使用更推荐的 ThreadPoolExecutor，可以这样写：
    // private static final ExecutorService threadPool = new ThreadPoolExecutor(
    //         10, // 核心线程数：即使空闲也不会被回收的线程数
    //         100, // 最大线程数：线程池能容纳的最大线程数
    //         60L, TimeUnit.SECONDS, // 空闲线程存活时间：非核心线程空闲超过60秒则回收
    //         new LinkedBlockingQueue<>(500) // 工作队列：大小500，而不是无界队列
    // );

/**
 * 处理一个客户端的请求
 * @param clientSocket 客户端的Socket连接
 */
    private static void handleClient(Socket clientSocket) {
        // 注意：这个方法是运行在线程池中的某个线程里，而不是主线程！
        String clientName = clientSocket.getRemoteSocketAddress().toString();
        try {
            logger.info("[{}] 开始处理这个客户端的请求。", clientName);

            // TODO: 这里将来会添加读取客户端消息的逻辑
            // 现在我们先模拟一个耗时操作
            Thread.sleep(5000); // 模拟处理业务用了5秒

            logger.info("[{}] 请求处理完毕。", clientName);

        } catch (Exception e) {
            logger.error("[{}] 处理请求时发生异常", clientName, e);
        } finally {
            try {
                clientSocket.close(); // 处理完毕，关闭连接
            } catch (IOException e) {
                logger.error("[{}] 关闭连接时发生异常", clientName, e);
            }
        }
    }

    // public static void main(String[] args) {
    //     // 使用 try-with-resources 确保 ServerSocket 会被自动关闭
    //     try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    //         logger.info("服务器启动成功，正在端口 {} 监听等待客户端连接...", PORT);

    //         // accept() 方法是一个阻塞调用，会一直等待直到有客户端连接上来
    //         Socket clientSocket = serverSocket.accept();
    //         logger.info("有客户端连接成功了！客户端地址：{}", clientSocket.getRemoteSocketAddress());

    //         // TODO: 这里将来会添加读取客户端发送数据的逻辑
    //         // 现在先暂时不读取，只是建立连接

    //         // 为了让服务器保持运行，加一个等待
    //         logger.info("按回车键退出服务器...");
    //         System.in.read();

    //     } catch (IOException e) {
    //         logger.error("服务器发生异常", e);
    //     }
    //     logger.info("服务器已关闭。");
    // }

    public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        logger.info("服务器启动成功，正在端口 {} 监听等待客户端连接...", PORT);

        // 无限循环，持续接受客户端连接
        while (true) {
            // 1. 等待客户端连接（阻塞）
            Socket clientSocket = serverSocket.accept();
            logger.info("有客户端连接成功！地址：{}", clientSocket.getRemoteSocketAddress());

            // 2. 将处理客户端的任务提交给线程池，而不是自己处理
            // 使用Lambda表达式创建一个Runnable任务
            threadPool.submit(() -> {
                handleClient(clientSocket);
            });
            // 主线程迅速回到accept()，继续等待下一个客户端，实现并发
        }
    } catch (IOException e) {
        logger.error("服务器发生异常", e);
    } finally {
        threadPool.shutdown(); // 关闭线程池
    }
}
}
