package com.substring.chat.playload;

public class ReactionRequest {

    private String messageId;
    private String user;
    private String reaction;

    public ReactionRequest(){}

    public String getMessageId(){ return messageId; }
    public void setMessageId(String messageId){ this.messageId=messageId; }

    public String getUser(){ return user; }
    public void setUser(String user){ this.user=user; }

    public String getReaction(){ return reaction; }
    public void setReaction(String reaction){ this.reaction=reaction; }
}