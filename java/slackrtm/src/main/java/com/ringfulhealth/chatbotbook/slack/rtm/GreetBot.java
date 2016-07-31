package com.ringfulhealth.chatbotbook.slack.rtm;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackMessageHandle;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import com.ullink.slack.simpleslackapi.replies.SlackChannelReply;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class GreetBot extends BaseBot {

    public GreetBot () {
        // token = "YOUR-SLACK-BOT-TOKEN";
        token = "xoxb-36964407043-kIy2SKWeOaDFU1vf6UdndTYx";
    }

    public String converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("GreetBot converse");
        if (!context.containsKey("status")) {
            context.put("status", "wait_for_name");
            return "Hello! What is your name? Please enter firstname lastname";
        }

        if ("wait_for_name".equalsIgnoreCase((String) context.get("status"))) {
            context.put("name", human);
            context.put("status", "wait_for_gender");
            return "It is great meeting you! What is your gender? Please reply Female, Male, or Other";
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
