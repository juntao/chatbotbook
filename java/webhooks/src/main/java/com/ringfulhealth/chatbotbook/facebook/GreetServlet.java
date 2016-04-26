package com.ringfulhealth.chatbotbook.facebook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class GreetServlet extends BaseServlet {

    public GreetServlet () {
        page_access_token = "YOUR-FB-PAGE-ACCESS-TOKEN";
    }

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("GreetServlet converse");
        if (!context.containsKey("status")) {
            context.put("status", "wait_for_name");
            return "Hello! What is your name? Please enter firstname lastname";
        }

        if ("wait_for_name".equalsIgnoreCase((String) context.get("status"))) {
            context.put("name", human);
            context.put("status", "wait_for_gender");

            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "It is great meeting you! What is your gender?");

                String [] button_titles   = {"Female", "Male", "Other"};
                String [] button_payloads = {"F", "M", "O"};

                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;

            } catch (Exception e) {
                e.printStackTrace();
            }
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