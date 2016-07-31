package com.ringfulhealth.chatbotbook.slack.channel;

import java.util.concurrent.ConcurrentHashMap;

public class GreetServlet extends BaseServlet {

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("GreetServlet converse");
        if (!context.containsKey("status")) {
            context.put("status", "wait_for_name");
            return "Hello! What is your name? Please enter _firstname lastname_";
        }

        if ("wait_for_name".equalsIgnoreCase((String) context.get("status"))) {
            context.put("name", human);
            context.put("status", "wait_for_gender");
            return "It is great meeting you! What is your gender? Please reply *Female*, *Male*, or *Other*";
        }

        if ("wait_for_gender".equalsIgnoreCase((String) context.get("status"))) {
            String salutation = "";
            if (human.toUpperCase().startsWith("F")) {
                salutation = "Ms. ";
            } else if (human.toUpperCase().startsWith("M")) {
                salutation = "Mr. ";
            } else if (human.toUpperCase().startsWith("O")) {
                salutation = "";
            } else {
                context.put("status", "wait_for_gender");
                return "Sorry, I did not get it. Please reply Female, Male, or Other";
            }

            context.remove("status");
            return "Hello, " + salutation + context.get("name");
        }

        return null;
    }

}