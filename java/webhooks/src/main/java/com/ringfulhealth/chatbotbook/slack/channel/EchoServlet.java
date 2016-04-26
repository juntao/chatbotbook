package com.ringfulhealth.chatbotbook.slack.channel;

import java.util.concurrent.ConcurrentHashMap;

public class EchoServlet extends BaseServlet {

    public String converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("EchoServlet converse");
        return human;
    }

}