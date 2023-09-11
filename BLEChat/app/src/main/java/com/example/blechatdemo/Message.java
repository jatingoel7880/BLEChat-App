package com.example.blechatdemo;

import java.util.Date;

public class Message {

    private String senderName;
    private String sender;
    private String receiverName;
    private String receiver;
    private Date createdAt = new Date();
    private Boolean isSent;
    private String address;
    private String text;

    private String conteType;

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getSent() {
        return isSent;
    }

    public void setSent(Boolean sent) {
        isSent = sent;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getText() {
        return text;
    }

    public String getConteType() {
      return conteType;
    }// new code

   public void setConteType(String conteType) {
       this.conteType = conteType;
   } //new code

    public void setText(String text) {
        this.text = text;
    }


}
