package com.ringfulhealth.chatbotbook.facebook;

import java.util.concurrent.ConcurrentHashMap;

public class EchoServlet extends BaseServlet {

    public EchoServlet () {
        page_access_token = "FB-PAGE-TOKEN";
    }

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("EchoServlet converse");
        return human;
    }

}