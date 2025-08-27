package com.niweiru;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 网络工具类，提供消息发送和接收的通用方法
 */
public class NetworkUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);
    private static final Gson gson = new Gson();

    /**
     * 从Socket中读取一行JSON字符串并解析为Message对象
     */
    public static Message receiveMessage(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String jsonMessage = reader.readLine(); // 读取一行JSON字符串
        if (jsonMessage != null) {
            logger.debug("收到原始数据: {}", jsonMessage);
            // 使用Gson将JSON字符串转换回Message对象
            return gson.fromJson(jsonMessage, Message.class);
        }
        return null;
    }

    /**
     * 将Message对象序列化为JSON字符串并通过Socket发送
     */
    public static void sendMessage(Socket socket, Message message) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        // 使用Gson将Message对象转换为JSON字符串
        String jsonMessage = gson.toJson(message);
        logger.debug("发送原始数据: {}", jsonMessage);
        writer.println(jsonMessage); // 发送JSON字符串，println会自动添加换行符
    }
}
