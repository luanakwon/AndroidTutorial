package com.example.helloworld;

import java.util.ArrayList;
import java.util.List;

public class Contacts {
    private String name;
    private String phoneNo;
    private List<Message> msgDialog = new ArrayList<>();

    public Contacts(String name, String phoneNo) {
        this.phoneNo = phoneNo;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public List<Message> getMsgDialog() {
        return msgDialog;
    }

    public void sendMsgDialog(String msg, String time) {
        this.msgDialog.add(new Message(msg, time, true));
    }
    public void sendMsgDialog(String msg) {
        this.msgDialog.add(new Message(msg, "NOTIMEINFO", true));
    }

    private class Message {
        private String msg;
        private String time;
        private boolean send; // false if receive

        public Message(String msg, String time, boolean send) {
            this.msg = msg;
            this.time = time;
            this.send = send;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public boolean isSend() {
            return send;
        }

        public void setSend(boolean send) {
            this.send = send;
        }
    }
}
