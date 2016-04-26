package com.ringfulhealth.chatbotbook.slack.channel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseServlet extends HttpServlet {

    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache;
    static {
        cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> ();
    }

    public abstract String converse (String human, ConcurrentHashMap<String, Object> context);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if ("slackbot".equalsIgnoreCase(req.getParameter("user_name"))) {
            return;
        }

        String chat_id = req.getParameter("token") + "_" + req.getParameter("user_name");
        ConcurrentHashMap<String, Object> context = cache.get(chat_id + "_context");
        if (context == null) {
            context = new ConcurrentHashMap<String, Object> ();
            cache.put(chat_id + "_context", context);

        }

        String bot_says = converse(req.getParameter("text"), context);
        if (bot_says == null || bot_says.trim().isEmpty()) {
            bot_says = "Sorry, I cannot understand you!";
        }

        JSONObject json = new JSONObject();
        try {
            json.put("text", bot_says);
            json.put("mrkdwn", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.setContentType("application/json");
        resp.getOutputStream().println(json.toString());
        return;
    }

}
