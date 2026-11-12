package com.example.messageapp.model;

/** Một cuộc trò chuyện / liên hệ. */
public class Contact {
    private final long id;
    private String name;
    private String phone;
    // 2 trường dưới không lưu trong bảng contacts, được lấy từ bảng messages để hiển thị
    private String lastMessage;
    private long lastTime;

    public Contact(long id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLastMessage() { return lastMessage; }
    public long getLastTime() { return lastTime; }

    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastTime(long lastTime) { this.lastTime = lastTime; }
}
