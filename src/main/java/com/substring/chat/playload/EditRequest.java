package com.substring.chat.playload;

public class EditRequest {

    private String messageId;
    private String content;

    public EditRequest(){}

    public String getMessageId(){ return messageId; }
    public void setMessageId(String messageId){ this.messageId=messageId; }

    public String getContent(){ return content; }
    public void setContent(String content){ this.content=content; }
}