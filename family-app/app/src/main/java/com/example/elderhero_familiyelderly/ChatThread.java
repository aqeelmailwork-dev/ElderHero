package com.example.elderhero_familiyelderly;

public class ChatThread {

    public String chatId;
    public String caretakerId;
    public String familyElderlyId;
    public String lastChat;
    public long lastTimestamp;

    public ChatThread() {}

    public ChatThread(String chatId,
                      String caretakerId,
                      String familyElderlyId,
                      String lastChat,
                      long lastTimestamp) {

        this.chatId = chatId;
        this.caretakerId = caretakerId;
        this.familyElderlyId = familyElderlyId;
        this.lastChat = lastChat;
        this.lastTimestamp = lastTimestamp;
    }
}