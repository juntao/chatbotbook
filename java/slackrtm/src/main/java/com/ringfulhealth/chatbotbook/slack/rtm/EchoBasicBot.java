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

public class EchoBasicBot {

    public void listen() throws IOException {
        SlackSession current_session = SlackSessionFactory.createWebSocketSlackSession("YOUR-SLACK-BOT-TOKEN");

        SlackMessagePostedListener messagePosted = new SlackMessagePostedListener() {
            public void onEvent(SlackMessagePosted slackMessagePosted, SlackSession slackSession) {

                SlackUser user = slackMessagePosted.getSender();
                SlackMessageHandle<SlackChannelReply> reply =  slackSession.openDirectMessageChannel(user);
                SlackChannel channel = reply.getReply().getSlackChannel();

                slackSession.sendMessage(channel, slackMessagePosted.getMessageContent());
            }
        };

        current_session.addMessagePostedListener(messagePosted);
        current_session.connect();
    }
}
