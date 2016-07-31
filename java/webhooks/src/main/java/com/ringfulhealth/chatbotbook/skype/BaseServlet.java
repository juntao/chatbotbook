package com.ringfulhealth.chatbotbook.skype;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseServlet extends HttpServlet {

    protected static String app_id = "";
    protected static String app_secret = "";
    protected static String token = null;
    protected static Date token_exp_date = new Date (0);

    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache;
    static {
        cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> ();
    }

    public abstract Object converse (String human, ConcurrentHashMap<String, Object> context);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        // Read POST body from request
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String body = buffer.toString();
        System.out.println(body);

        // Expire the token if needed
        if ((new Date ()).after(token_exp_date)) {
            token = null;
        }
        if (token == null || token.trim().isEmpty()) {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            try {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("client_id", app_id));
                nvps.add(new BasicNameValuePair("client_secret", app_secret));
                nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
                nvps.add(new BasicNameValuePair("scope", "https://graph.microsoft.com/.default"));

                HttpPost httpPost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
                httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                response = httpclient.execute(httpPost);

                HttpEntity entity = response.getEntity();
                String resp_body = EntityUtils.toString(entity);
                System.out.println(resp_body);

                JSONObject json = new JSONObject(resp_body);
                token = json.getString("access_token");

                int expires_in = json.getInt("expires_in");
                if (expires_in > 10) {
                    expires_in = expires_in - 10;
                }
                token_exp_date = new Date(System.currentTimeMillis() + expires_in * 1000L);

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

        if (token != null && !token.trim().isEmpty()) {
            try {
                JSONObject entry = new JSONObject(body);
                if (!"message".equals(entry.getString("type")) && !"message/text".equals(entry.getString("type"))) {
                    // We will only process text messages
                    return;
                }
                String content = entry.getString("text");
                String cid = entry.getJSONObject("conversation").getString("id");
                String fromDisplayName = entry.getJSONObject("from").getString("name");

                ConcurrentHashMap<String, Object> context = cache.get(cid + "_context");
                if (context == null) {
                    context = new ConcurrentHashMap<String, Object> ();
                    cache.put(cid + "_context", context);
                }
                context.put("cid", cid);
                context.put("fromDisplayName", fromDisplayName);

                Object bot_says = converse(content.trim(), context);

                if (bot_says instanceof List) {
                    for (Object bs : (List) bot_says) {
                        sendReply(bs, cid);
                    }
                } else {
                    sendReply(bot_says, cid);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendReply (Object reply, String cid) throws Exception {
        if (reply == null) {
            return; // No action
        }

        JSONObject r = new JSONObject();
        if (reply instanceof String) {
            if (reply == null || ((String) reply).trim().isEmpty()) {
                return; // No action
            }
            r.put("type", "message/text");
            r.put("text", reply);

        } else if ((reply instanceof JSONObject)) {
            r.put("type", "message/card.carousel");
            r.put("attachments", (new JSONArray()).put(reply));

        } else if ((reply instanceof JSONArray)) {
            r.put("type", "message/card.carousel");
            r.put("attachments", reply);

        } else {
            throw new Exception ("Invalid return value from converse() method");
        }
        System.out.println(r.toString());

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://df-apis.skype.com/v3/conversations/" + cid + "/activities");
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setEntity(new StringEntity(r.toString(), StandardCharsets.UTF_8));
            response = httpclient.execute(httpPost);

            System.out.println("STATUS: " + response.getStatusLine());

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

    public static JSONObject createHero (String title, String subtitle, String [] images, HashMap <String, String> buttons) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("contentType", "application/vnd.microsoft.card.hero");

        JSONObject content = new JSONObject();
        if (title != null && !title.trim().isEmpty()) {
            content.put("title", title);
        }
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            content.put("subtitle", subtitle);
        }
        if (images != null && images.length > 0) {
            JSONArray images_arr = new JSONArray();
            for (String image : images) {
                images_arr.put((new JSONObject()).put("url", image));
            }
            content.put("images", images_arr);
        }
        if (buttons != null && buttons.size() > 0) {
            JSONArray buttons_arr = new JSONArray();
            for (Map.Entry<String, String> button : buttons.entrySet()) {
                if (button.getValue().startsWith("http")) {
                    buttons_arr.put((new JSONObject()).put("type", "openUrl").put("title", button.getKey()).put("value", button.getValue()));
                } else {
                    buttons_arr.put((new JSONObject()).put("type", "imBack").put("title", button.getKey()).put("value", button.getValue()));
                }
            }
            content.put("buttons", buttons_arr);
        }

        obj.put("content", content);
        return obj;
    }

    public static boolean containsWord (String test, String [] list) {
        for (String s : list) {
            if (test.toUpperCase().contains(" " + s.toUpperCase()) || test.toUpperCase().contains(s.toUpperCase() + " ") || test.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> splitByWord (String str, int segment_length) {
        // http://stackoverflow.com/questions/25853393/split-a-string-in-java-into-equal-length-substrings-while-maintaining-word-bound
        List <String> split = new ArrayList <String> ();
        Pattern p = Pattern.compile("\\G\\s*(.{1," + segment_length + "})(?=\\s|$)", Pattern.DOTALL);
        Matcher m = p.matcher(str);
        while (m.find()) {
            split.add(m.group(1));
        }
        return split;
    }
}
