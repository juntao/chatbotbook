package com.ringfulhealth.chatbotbook.skype;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class GreetServlet extends BaseServlet {

    public GreetServlet () {
        app_id = "YOUR-MS-APP-ID";
        app_secret = "YOUR-MS-APP-SECRET";
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
                return createHero(
                        "It is great meeting you! What is your gender?",
                        null,
                        null,
                        new HashMap<String, String>() {{
                            put("Female", "Female");
                            put("Male", "Male");
                            put("Other", "Other");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace ();
                return "Sorry, there is an error";
            }

            // return Arrays.asList("It is great meeting you! What is your gender?", "Please reply Female, Male, or Other");
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
