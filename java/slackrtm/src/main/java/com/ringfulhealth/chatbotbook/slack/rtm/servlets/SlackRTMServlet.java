package com.ringfulhealth.chatbotbook.slack.rtm.servlets;

import com.ringfulhealth.chatbotbook.slack.rtm.GreetBot;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class SlackRTMServlet extends GenericServlet {

    // Single thread for now since all clients gets the same events from Slack. We need a single thread dispatcher in any case.
    ExecutorService executor = Executors.newFixedThreadPool(1);

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        Future<String> f1 = executor.submit(new Callable <String>() {
            public String call() throws Exception {
                GreetBot bot = new GreetBot();
                bot.listen();
                return null;
            }
        });
    }

    public void service(ServletRequest serveletRequest, ServletResponse servletResponse) throws ServletException, IOException {
    }

    public void destroy() {
        executor.shutdown();
    }
}