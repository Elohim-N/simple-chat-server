package com.niweiru;

/**
 * 消息模型类
 * 用于表示一条聊天消息
 */
public class Message {
    private User sender; // 发送者（这里用了User对象，体现了OOP的“关联”关系）
    private String content; // 消息内容
    private long timestamp; // 时间戳

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis(); // 自动设置为当前时间
    }

    // Getter 和 Setter
    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{sender=" + sender + ", content='" + content + "', timestamp=" + timestamp + "}";
    }
}
