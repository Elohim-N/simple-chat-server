package com.niweiru;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static volatile boolean running = true;

    public static void main(String[] args) {
        logger.info("客户端启动，尝试连接服务器 {}:{}", SERVER_IP, SERVER_PORT);

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            logger.info("连接服务器成功！请输入消息（输入 'exit' 退出）:");

            // 创建并启动一个单独的线程来接收服务器消息
            Thread receiveThread = new Thread(() -> {
                try {
                    while (running) {
                        Message receivedMessage = NetworkUtils.receiveMessage(socket);
                        if (receivedMessage != null) {
                            logger.info("收到回复: [{}] {}", 
                                receivedMessage.getSender().getUsername(), 
                                receivedMessage.getContent());
                        } else {
                            // 收到null说明连接可能已关闭
                            logger.info("服务器连接已关闭");
                            running = false;
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (running) { // 只在正常运行状态下报告错误，running=False，终止程序（优雅）
                        logger.error("接收消息时发生错误", e);
                    }
                }
            });
            receiveThread.start();

            // 主程序继续读取用户输入并发送

            // 创建一个固定的用户用于演示（未来会实现登录）
            User currentUser = new User("001", "Alice");
            String userInput;
            
            while (running && (userInput = stdIn.readLine()) != null) {
                if ("exit".equalsIgnoreCase(userInput)) {
                    running = false;
                    // 【新增】主动关闭Socket，这会使得receiveThread中的receiveMessage()抛出IOException，从而跳出循环
                    try {
                        socket.close(); // 关闭Socket，中断阻塞的读取操作
                    } catch (IOException e) {
                        logger.debug("关闭socket时发生异常", e);
                    }
                    break;
                }
                
                Message messageToSend = new Message(currentUser, userInput);
                NetworkUtils.sendMessage(socket, messageToSend);
                logger.info("已发送: {}", userInput);
            }

            // 等待接收线程结束
            receiveThread.join();
            
        } catch (IOException | InterruptedException e) {
            logger.error("客户端发生错误", e);
        } finally {
            running = false;
        }
        logger.info("客户端已关闭。");
    }
}
