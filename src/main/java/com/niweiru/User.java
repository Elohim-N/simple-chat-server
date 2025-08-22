package com.niweiru;

/**
 * 用户模型类
 * 用于表示一个聊天系统的用户
 */
public class User {
    // 属性（字段）
    private String id; // 用户唯一标识
    private String username; // 用户名

    // 构造方法（用于创建对象时初始化）
    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getter 和 Setter 方法（用于读取和修改私有属性）
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // toString 方法（方便打印对象信息）
    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "'}";
    }
}
