package com.example.elderhero_caretakers.chats;

public class MessageModel {
    public String messageId;
    public String chatId;
    public String senderId;
    public String receiverId;
    public String messageType;     // "text" | "image" | "voice"
    public String messageText;
    public String messageImageURL;
    public String messageAudioURL;
    public long messageTimeStamp;

    public MessageModel() {}

    public MessageModel(String messageId, String chatId, String senderId, String receiverId,
                        String messageType, String messageText, String messageImageURL,
                        String messageAudioURL, long messageTimeStamp) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.messageText = messageText;
        this.messageImageURL = messageImageURL;
        this.messageAudioURL = messageAudioURL;
        this.messageTimeStamp = messageTimeStamp;
    }
}
