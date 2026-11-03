package com.example.elderhero_caretakers.chats;

public class ChatThread {
    public String chatId;
    public String caretakerId;
    public String familyElderlyId;
    public String lastMessage;
    public long lastTimestamp;

    public ChatThread() {}

    public ChatThread(String chatId, String caretakerId, String familyElderlyId, String lastMessage, long lastTimestamp) {
        this.chatId = chatId;
        this.caretakerId = caretakerId;
        this.familyElderlyId = familyElderlyId;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }
}
