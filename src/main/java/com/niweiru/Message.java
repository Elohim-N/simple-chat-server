package com.niweiru;

// 导入Gson注解
import com.google.gson.annotations.SerializedName;

/**
 * 消息模型类
 * 用于表示一条聊天消息，并支持Gson序列化/反序列化
 */
public class Message {
    // @SerializedName 注解告诉Gson，Java字段"type"对应JSON中的键"type"
    @SerializedName("type")
    private String type; // 消息类型，如 "chat", "login", "system"

    @SerializedName("sender")
    private User sender; // 发送者

    @SerializedName("content")
    private String content; // 消息内容

    @SerializedName("timestamp")
    private long timestamp; // 时间戳

    // 无参构造方法（Gson反序列化时需要）
    public Message() {
    }

    public Message(User sender, String content) {
        this.type = "chat";
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message{sender=" + sender + ", content='" + content + "', timestamp=" + timestamp + "}";
    }
}
