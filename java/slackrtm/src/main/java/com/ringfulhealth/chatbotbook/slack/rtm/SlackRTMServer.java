package com.ringfulhealth.chatbotbook.slack.rtm;

import java.io.IOException;

public class SlackRTMServer {

    public static void main(String[] args) throws IOException {
        // EchoBasicBot bot = new EchoBasicBot();
        // EchoBot bot = new EchoBot();
        GreetBot bot = new GreetBot();
        bot.listen();
    }
}
