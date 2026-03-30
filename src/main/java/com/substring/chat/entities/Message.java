package com.substring.chat.entities;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Message {

    private String id;
    private String sender;
    private String recipient;
    private String content;
    private String type;
    private String fileUrl;
    private LocalDateTime timeStamp;
    private boolean privateMessage = false;

    private boolean edited = false;
    private boolean seen = false;

    private String replyTo;

    private Map<String,String> reactions = new HashMap<>();

    public Message(){}

    public String getId(){ return id; }
    public void setId(String id){ this.id=id; }

    public String getSender(){ return sender; }
    public void setSender(String sender){ this.sender=sender; }

    public String getRecipient(){ return recipient; }
    public void setRecipient(String recipient){ this.recipient=recipient; }

    public String getContent(){ return content; }
    public void setContent(String content){ this.content=content; }

    public String getType(){ return type; }
    public void setType(String type){ this.type=type; }

    public String getFileUrl(){ return fileUrl; }
    public void setFileUrl(String fileUrl){ this.fileUrl=fileUrl; }

    public LocalDateTime getTimeStamp(){ return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp){ this.timeStamp=timeStamp; }

    public boolean isPrivateMessage(){ return privateMessage; }
    public void setPrivateMessage(boolean privateMessage){ this.privateMessage=privateMessage; }

    public boolean isEdited(){ return edited; }
    public void setEdited(boolean edited){ this.edited=edited; }

    public boolean isSeen(){ return seen; }
    public void setSeen(boolean seen){ this.seen=seen; }

    public String getReplyTo(){ return replyTo; }
    public void setReplyTo(String replyTo){ this.replyTo=replyTo; }

    public Map<String,String> getReactions(){ return reactions; }
    public void setReactions(Map<String,String> reactions){ this.reactions=reactions; }

}
