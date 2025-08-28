package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // 新增：在线客户端映射表
    // Key: 客户端的Socket连接
    // Value: 客户端对应的用户信息
    private static final Map<Socket, User> onlineClients = new ConcurrentHashMap<>();

/**
 * 广播聊天消息给所有其他客户端（排除发送者自己）
 * @param message 要广播的消息
 * @param senderSocket 消息发送者的Socket（用于排除自己）
 */
    private static void broadcastMessage(Message message, Socket senderSocket) {
        // 遍历在线客户端映射表的所有条目
        for (Map.Entry<Socket, User> entry : onlineClients.entrySet()) {
            Socket clientSocket = entry.getKey();
            // 排除消息发送者自己，不然自己也会收到自己发的消息
            if (!clientSocket.equals(senderSocket)) {
                try {
                    NetworkUtils.sendMessage(clientSocket, message);
                } catch (IOException e) {
                    logger.error("向客户端 [{}] 广播消息失败", entry.getValue().getUsername(), e);
                }
            }
        }
    }

    /**
     * 广播系统消息给所有客户端
     * @param content 系统消息内容
     */
    private static void broadcastSystemMessage(String content) {
        User systemUser = new User("system", "System");
        Message systemMessage = new Message(systemUser, content);
        // 系统消息不需要排除任何人，发给所有客户端
        for (Map.Entry<Socket, User> entry : onlineClients.entrySet()) {
            try {
                NetworkUtils.sendMessage(entry.getKey(), systemMessage);
            } catch (IOException e) {
                logger.error("向客户端 [{}] 广播系统消息失败", entry.getValue().getUsername(), e);
            }
        }
    }
/**
 * 处理客户端的请求
 * @param clientSocket 客户端的Socket连接
 */
    private static void handleClient(Socket clientSocket) {
        String clientName = clientSocket.getRemoteSocketAddress().toString();
        try {
            logger.info("[{}] 开始处理这个客户端的请求。", clientName);

            // 1. 接收客户端发送的第一个消息，假设为登录消息，包含用户信息
            Message loginMessage = NetworkUtils.receiveMessage(clientSocket);
            if (loginMessage == null || loginMessage.getSender() == null) {
                logger.warn("[{}] 客户端未发送有效的登录信息，连接关闭。", clientName);
                return;
            }

            User clientUser = loginMessage.getSender();
            // 2. 将新客户端添加到在线列表
            onlineClients.put(clientSocket, clientUser);
            logger.info("[{}] 用户 [{}] 已加入聊天室。当前在线人数: {}", clientName, clientUser.getUsername(), onlineClients.size());

            // 3. 广播系统通知：某某用户加入了聊天室
            broadcastSystemMessage(clientUser.getUsername() + " 加入了聊天室");

            // 4. 进入消息循环
            Message clientMessage;
            while ((clientMessage = NetworkUtils.receiveMessage(clientSocket)) != null) {
                logger.info("[{}] 收到消息: [{}] {}", clientName, clientMessage.getSender().getUsername(), clientMessage.getContent());

                // 5. 【核心功能】将收到的聊天消息广播给所有其他客户端
                broadcastMessage(clientMessage, clientSocket); // 排除消息发送者自己
            }

            logger.info("[{}] 客户端断开连接。", clientName);

        } catch (IOException e) {
            logger.error("[{}] 处理请求时发生异常", clientName, e);
        } finally {
            // 6. 无论如何，最终都要从在线列表中移除该客户端
            User removedUser = onlineClients.get(clientSocket);
            onlineClients.remove(clientSocket);
            logger.info("[{}] 用户已从在线列表移除。当前在线人数: {}", clientName, onlineClients.size());
            // 广播系统通知：某某用户离开了聊天室
            if (removedUser != null) {
            broadcastSystemMessage(removedUser.getUsername() + " 离开了聊天室");
        }
            try {
                clientSocket.close();
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
