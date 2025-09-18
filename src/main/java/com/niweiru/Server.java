package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
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
    // 在Server类中添加一个用户名字典，用于快速查找用户名是否已存在
    private static final Set<String> onlineUsernames = ConcurrentHashMap.newKeySet();

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
    // 将 clientUser 声明在 try 块外部，使其在 finally 块中可见
    User clientUser = null;
    
    try {
        logger.info("[{}] 开始处理这个客户端的请求。", clientName);

        // 1. 接收客户端发送的第一个消息，假设为登录消息，包含用户信息
        // 将 loginMessage 声明在 try 块内部是可以的，因为我们只需要在 try 块中使用它
        Message loginMessage = NetworkUtils.receiveMessage(clientSocket);
        if (loginMessage == null || loginMessage.getSender() == null) {
            logger.warn("[{}] 客户端未发送有效的登录信息，连接关闭。", clientName);
            return;
        }

        // 使用已声明的 clientUser 变量，不要再次声明
        clientUser = loginMessage.getSender();
        String username = clientUser.getUsername();

        // 检查用户名是否已在线
        if (onlineUsernames.contains(username)) {
            sendSystemMessage(clientSocket, "用户名 " + username + " 已被使用，请选择其他用户名");
            clientSocket.close();
            return;
        }

        // 用户名可用，添加到在线集合
        onlineUsernames.add(username);
        onlineClients.put(clientSocket, clientUser);
        logger.info("[{}] 用户 [{}] 已加入聊天室。当前在线人数: {}", clientName, username, onlineClients.size());

        // 广播系统通知：某某用户加入了聊天室
        broadcastSystemMessage(username + " 加入了聊天室");

        // 4. 进入消息循环
        Message clientMessage;
        while ((clientMessage = NetworkUtils.receiveMessage(clientSocket)) != null) {
            String content = clientMessage.getContent();
            logger.info("[{}] 收到消息: [{}] {}", clientName, clientUser.getUsername(), content);
            
            // 判断是否是命令（以/开头）
            if (content.startsWith("/")) {
                // 处理命令
                handleCommand(content, clientSocket);
            } 
            // 判断是否是私聊消息（以@开头）
            else if (content.startsWith("@")) {
                // 处理私聊消息
                handlePrivateMessage(clientMessage, clientSocket);
            } else {
                // 处理广播消息
                broadcastMessage(clientMessage, clientSocket);
            }
        }

        logger.info("[{}] 客户端断开连接。", clientName);

    } catch (IOException e) {
        logger.error("[{}] 处理请求时发生异常", clientName, e);
    } finally {
        // 6. 无论如何，最终都要从在线列表中移除该客户端
        // 先获取用户信息，然后再移除
        if (clientUser != null) {
            onlineUsernames.remove(clientUser.getUsername());
        }
        onlineClients.remove(clientSocket);
        logger.info("[{}] 用户已从在线列表移除。当前在线人数: {}", clientName, onlineClients.size());
        
        // 广播系统通知：某某用户离开了聊天室
        if (clientUser != null) {
            broadcastSystemMessage(clientUser.getUsername() + " 离开了聊天室");
        }
        
        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.error("[{}] 关闭连接时发生异常", clientName, e);
        }
    }
}

    /**
     * 处理私聊消息
     * @param message 原始消息
     * @param senderSocket 发送者的Socket
     */
    private static void handlePrivateMessage(Message message, Socket senderSocket) {
        String content = message.getContent();
        // 解析消息格式：@username message
        int spaceIndex = content.indexOf(' ');
        if (spaceIndex == -1) {
            // 如果没有空格，说明格式错误
            sendSystemMessage(senderSocket, "私聊格式错误，请使用: @用户名 消息内容");
            return;
        }
        
        String targetUsername = content.substring(1, spaceIndex); // 去掉@，取用户名
        String privateContent = content.substring(spaceIndex + 1); // 取消息内容
        
        // 查找目标用户
        Socket targetSocket = findSocketByUsername(targetUsername);
        if (targetSocket == null) {
            sendSystemMessage(senderSocket, "用户 " + targetUsername + " 不存在或不在线");
            return;
        }
        
        // 创建私聊消息（可以修改原消息或创建新消息）
        Message privateMessage = new Message(message.getSender(), privateContent);
        privateMessage.setType("private"); // 可以添加类型字段区分
        
        try {
            // 发送私聊消息给目标用户
            NetworkUtils.sendMessage(targetSocket, privateMessage);
            // 可选：也发送给发送者自己，像许多聊天软件那样
            NetworkUtils.sendMessage(senderSocket, privateMessage);
            logger.info("私聊消息已从 [{}] 发送给 [{}]", 
                    message.getSender().getUsername(), targetUsername);
        } catch (IOException e) {
            logger.error("发送私聊消息失败", e);
            sendSystemMessage(senderSocket, "发送私聊消息失败");
        }
    }

    /**
     * 根据用户名查找对应的Socket连接
     * @param username 要查找的用户名
     * @return 对应用户的Socket，如果找不到返回null
     */
    private static Socket findSocketByUsername(String username) {
        for (Map.Entry<Socket, User> entry : onlineClients.entrySet()) {
            if (username.equals(entry.getValue().getUsername())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 向指定客户端发送系统消息
     * @param socket 目标客户端Socket
     * @param content 消息内容
     */
    private static void sendSystemMessage(Socket socket, String content) {
        try {
            User systemUser = new User("system", "System");
            Message systemMessage = new Message(systemUser, content);
            NetworkUtils.sendMessage(socket, systemMessage);
        } catch (IOException e) {
            logger.error("发送系统消息失败", e);
        }
    }
    private static void handleCommand(String command, Socket senderSocket) {
        if ("/list".equals(command)) {
            // 响应/list命令，列出所有在线用户
            StringBuilder userList = new StringBuilder("在线用户:\n");
            for (User user : onlineClients.values()) {
                userList.append("- ").append(user.getUsername()).append("\n");
            }
            sendSystemMessage(senderSocket, userList.toString());
        }
        // 可以扩展其他命令，如/help等
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
