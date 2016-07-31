package com.ringfulhealth.chatbotbook.skype;

import java.util.concurrent.ConcurrentHashMap;

public class EchoServlet extends BaseServlet {

    public EchoServlet () {
        app_id = "YOUR-MS-APP-ID";
        app_secret = "YOUR-MS-APP-SECRET";
    }

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("EchoServlet converse");
        return human;
    }

}