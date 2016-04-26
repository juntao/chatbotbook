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

public abstract class BaseBot {

    protected String token;
    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache;
    static {
        cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> ();
    }

    public abstract String converse (String human, ConcurrentHashMap<String, Object> context);

    public void listen() throws IOException {
        SlackSession current_session = SlackSessionFactory.createWebSocketSlackSession(token);

        SlackMessagePostedListener messagePosted = new SlackMessagePostedListener() {
            public void onEvent(SlackMessagePosted slackMessagePosted, SlackSession slackSession) {

                SlackUser user = slackMessagePosted.getSender();
                SlackMessageHandle<SlackChannelReply> reply =  slackSession.openDirectMessageChannel(user);
                SlackChannel channel = reply.getReply().getSlackChannel();

                String chat_id = user.getId();
                ConcurrentHashMap<String, Object> context = cache.get(chat_id + "_context");
                if (context == null) {
                    context = new ConcurrentHashMap<String, Object> ();
                    cache.put(chat_id + "_context", context);

                }

                String bot_says = converse(slackMessagePosted.getMessageContent(), context);
                if (bot_says == null || bot_says.trim().isEmpty()) {
                    bot_says = "Sorry, I cannot understand you!";
                }

                slackSession.sendMessage(channel, bot_says);
            }
        };

        current_session.addMessagePostedListener(messagePosted);
        current_session.connect();
    }
}
