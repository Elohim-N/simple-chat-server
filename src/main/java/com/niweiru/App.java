package com.niweiru;

// 导入日志类
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    // 创建一个日志记录器（Logger），关联到当前App类
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("程序启动成功！开始创建用户和消息...");

        // 1. 创建用户对象 (OOP: 实例化)
        User alice = new User("001", "Alice");
        User bob = new User("002", "Bob");

        // 2. 创建消息对象 (OOP: 对象之间的关联)
        Message message1 = new Message(alice, "你好，Bob！");
        Message message2 = new Message(bob, "Hello, Alice!");

        // 3. 打印消息对象 (体验toString方法的好处)
        // 使用日志输出，而不是System.out.println
        logger.info("消息1: {}", message1); // {} 是占位符，会自动替换为message1.toString()的内容
        logger.info("消息2: {}", message2);

        logger.info("程序执行完毕。");
    }
}
