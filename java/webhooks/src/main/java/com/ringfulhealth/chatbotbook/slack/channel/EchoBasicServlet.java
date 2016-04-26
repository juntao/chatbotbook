package com.ringfulhealth.chatbotbook.slack.channel;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EchoBasicServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if ("slackbot".equalsIgnoreCase(req.getParameter("user_name"))) {
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("text", req.getParameter("text"));
            json.put("mrkdwn", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.setContentType("application/json");
        resp.getOutputStream().println(json.toString());
        return;
    }

}
