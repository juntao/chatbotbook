package com.ringfulhealth.chatbotbook.facebook;

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

    protected String page_access_token = "";
    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache;
    static {
        cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> ();
    }

    public abstract Object converse (String human, ConcurrentHashMap<String, Object> context);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (req.getParameter("hub.verify_token") != null) {
            resp.setContentType("text/plain");
            resp.getOutputStream().println(req.getParameter("hub.challenge"));
            return;
        }

        // Read POST body from request
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String body = buffer.toString();
        System.out.println(body);

        try {
            JSONObject json = new JSONObject(body);
            JSONArray entries = json.getJSONArray("entry");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                JSONArray messagings = entry.getJSONArray("messaging");
                for (int j = 0; j < messagings.length(); j++) {
                    JSONObject messaging = messagings.getJSONObject(j);

                    String sender_id = messaging.getJSONObject("sender").getString("id");

                    String message = null;
                    if (messaging.has("message")) {
                        message = messaging.getJSONObject("message").getString("text");
                    } else if (messaging.has("postback")) {
                        message = messaging.getJSONObject("postback").getString("payload");
                    } else {
                        throw new Exception ("The incoming Facebook JSON is improperly formatted");
                    }

                    ConcurrentHashMap<String, Object> context = cache.get(sender_id + "_context");
                    if (context == null) {
                        context = new ConcurrentHashMap<String, Object> ();
                        cache.put(sender_id + "_context", context);

                    }

                    Object bot_says = converse(message, context);

                    JSONObject r = new JSONObject();
                    if (bot_says instanceof String) {
                        r.put("recipient", (new JSONObject()).put("id", sender_id));
                        r.put("message", (new JSONObject()).put("text", bot_says));
                    } else if ((bot_says instanceof JSONObject) && ((JSONObject) bot_says).has("template_type")) {
                        r.put("recipient", (new JSONObject()).put("id", sender_id));
                        r.put("message", (new JSONObject()).put("attachment", new JSONObject()));
                        r.getJSONObject("message").getJSONObject("attachment").put("type", "template");
                        r.getJSONObject("message").getJSONObject("attachment").put("payload", bot_says);
                    } else if ((bot_says instanceof JSONObject) && ((JSONObject) bot_says).has("url")) {
                        r.put("recipient", (new JSONObject()).put("id", sender_id));
                        r.put("message", (new JSONObject()).put("attachment", new JSONObject()));
                        r.getJSONObject("message").getJSONObject("attachment").put("type", "image");
                        r.getJSONObject("message").getJSONObject("attachment").put("payload", bot_says);
                    } else {
                        throw new Exception ("Invalid return value from converse() method");
                    }
                    System.out.println(r.toString());

                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    CloseableHttpResponse response = null;
                    try {
                        HttpPost httpPost = new HttpPost("https://graph.facebook.com/v2.6/me/messages?access_token=" + page_access_token);
                        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        httpPost.setEntity(new StringEntity(r.toString(), StandardCharsets.UTF_8));
                        response = httpclient.execute(httpPost);

                        HttpEntity entity = response.getEntity();
                        System.out.println(EntityUtils.toString(entity));

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (httpclient != null) {
                                httpclient.close();
                            }
                        } catch (Exception e) {}
                        try {
                            if (response != null) {
                                response.close();
                            }
                        } catch (Exception e) {}
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException (e);
        }

    }

    public JSONArray createButtons (String [] titles, String [] payloads) throws Exception {
        if (titles == null || payloads == null || titles.length == 0 || payloads.length == 0 || titles.length != payloads.length) {
            throw new Exception ("Buttons titles and payloads mismatch");
        }

        JSONArray arr = new JSONArray ();
        for (int i = 0; i < titles.length; i++) {
            arr.put((new JSONObject()).put("type", "postback").put("title", titles[i]).put("payload", payloads[i]));
        }
        return arr;
    }

}
