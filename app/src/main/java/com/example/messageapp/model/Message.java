package com.example.messageapp.model;

/** Một tin nhắn trong cuộc trò chuyện. */
public class Message {
    private final long id;
    private final long contactId;
    private String content;
    private final boolean fromMe;   // true = mình gửi, false = người kia gửi
    private final long createdAt;

    public Message(long id, long contactId, String content, boolean fromMe, long createdAt) {
        this.id = id;
        this.contactId = contactId;
        this.content = content;
        this.fromMe = fromMe;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getContactId() { return contactId; }
    public String getContent() { return content; }
    public boolean isFromMe() { return fromMe; }
    public long getCreatedAt() { return createdAt; }

    public void setContent(String content) { this.content = content; }
}
