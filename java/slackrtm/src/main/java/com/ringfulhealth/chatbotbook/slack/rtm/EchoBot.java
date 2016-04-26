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

public class EchoBot extends BaseBot {

    public EchoBot () {
        token = "YOUR-SLACK-BOT-TOKEN";
    }

    public String converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("EchoBot converse");
        return human;
    }
}
