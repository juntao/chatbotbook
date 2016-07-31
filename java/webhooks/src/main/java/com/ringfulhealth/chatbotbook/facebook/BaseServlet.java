package com.ringfulhealth.chatbotbook.facebook;

import org.apache.commons.codec.binary.StringUtils;
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

    protected String page_access_token = "";

    protected String zencoder_apikey = "";
    protected String audio_bluemix_username = "";
    protected String audio_bluemix_password = "";

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

        // HTTP connection closes here. But the servlet continues to run.
        resp.getOutputStream().close();

        try {
            JSONObject json = new JSONObject(body);
            JSONArray entries = json.getJSONArray("entry");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                JSONArray messagings = entry.getJSONArray("messaging");
                for (int j = 0; j < messagings.length(); j++) {
                    JSONObject messaging = messagings.getJSONObject(j);

                    String sender_id = messaging.getJSONObject("sender").getString("id");
                    ConcurrentHashMap<String, Object> context = cache.get(sender_id + "_context");
                    if (context == null) {
                        context = new ConcurrentHashMap<String, Object> ();
                        cache.put(sender_id + "_context", context);
                    }
                    context.put("sender_id", sender_id);

                    String message = null;
                    if (messaging.has("message")) {
                        JSONObject msg_obj = messaging.getJSONObject("message");
                        try {
                            if (msg_obj.has("quick_reply")) {
                                message = msg_obj.getJSONObject("quick_reply").getString("payload");
                            } else {
                                message = msg_obj.getString("text");
                            }
                        } catch (Exception e) {
                            // message remains null.
                        }

                        if (message == null) {
                            try {
                                String t = msg_obj.getJSONArray("attachments").getJSONObject(0).getString("type");
                                String u = msg_obj.getJSONArray("attachments").getJSONObject(0).getJSONObject("payload").getString("url");

                                if ("audio".equalsIgnoreCase(t)) {
                                    if (zencoder_apikey.isEmpty() || audio_bluemix_username.isEmpty() || audio_bluemix_password.isEmpty()) {
                                        // message == null
                                        sendReply("Sorry, voice recognition is not currently supported. Please reply with text.", sender_id);
                                        return;
                                    } else {
                                        sendReply("Please wait for a few seconds while we try to figure out what you just said ...", sender_id);
                                        message = speechToText(transcodeAudio(u, "ogg", 16000), "ogg");
                                        if (message == null) {
                                            sendReply("Sorry, I cannot understand you. Please try reply with text.", sender_id);
                                            return;
                                        } else {
                                            sendReply("I understand that you said: \"" + message + "\"", sender_id);
                                        }
                                    }
                                } else if ("video".equalsIgnoreCase(t)) {
                                    sendReply("Sorry, video reply is not currently supported. Please reply with text.", sender_id);
                                    return;

                                } else if ("image".equalsIgnoreCase(t)) {
                                    sendReply("Sorry, image reply is not currently supported. Please reply with text.", sender_id);
                                    return;

                                } else {
                                    sendReply("Sorry, I cannot understand you. Please reply with text.", sender_id);
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                sendReply("Sorry, I cannot understand you. Please reply with text.", sender_id);
                                return;
                            }
                        }
                    } else if (messaging.has("postback")) {
                        message = messaging.getJSONObject("postback").getString("payload");
                    } else {
                        // Ignore all other events such as message READ events etc.
                        // sendReply("Sorry, I cannot understand you.", sender_id);
                        return;
                    }

                    if (message == null || message.trim().isEmpty()) {
                        // No message. Return now.
                        return;
                    }
                    Object bot_says = converse(message.trim(), context);

                    if (bot_says instanceof List) {
                        for (Object bs : (List) bot_says) {
                            sendReply(bs, sender_id);
                        }
                    } else {
                        sendReply(bot_says, sender_id);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException (e);
        }

    }

    public void sendReply (Object reply, String uid) throws Exception {
        if (reply == null) {
            return; // No action
        }

        JSONObject r = new JSONObject();
        if (reply instanceof String) {
            r.put("recipient", (new JSONObject()).put("id", uid));
            r.put("message", (new JSONObject()).put("text", reply));

        } else if ((reply instanceof JSONObject) && ((JSONObject) reply).has("template_type")) {
            r.put("recipient", (new JSONObject()).put("id", uid));
            r.put("message", (new JSONObject()).put("attachment", new JSONObject()));
            r.getJSONObject("message").getJSONObject("attachment").put("type", "template");
            r.getJSONObject("message").getJSONObject("attachment").put("payload", reply);

        } else if ((reply instanceof JSONObject) && ((JSONObject) reply).has("quick_replies")) {
            r.put("recipient", (new JSONObject()).put("id", uid));
            r.put("message", reply);

        } else if ((reply instanceof JSONObject) && ((JSONObject) reply).has("payload") && ((JSONObject) reply).getJSONObject("payload").has("url")) {
            r.put("recipient", (new JSONObject()).put("id", uid));
            r.put("message", (new JSONObject()).put("attachment", reply));

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

    public static boolean containsWord (String test, String [] list) {
        for (String s : list) {
            if (test.toUpperCase().contains(" " + s.toUpperCase()) || test.toUpperCase().contains(s.toUpperCase() + " ") || test.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    // This is for the buttons template
    public static JSONObject createButtons (String title, HashMap <String, String> buttons) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("template_type", "button");
        if (title != null && !title.trim().isEmpty()) {
            payload.put("text", title);
        }
        payload.put("buttons", createButtons(buttons.keySet().toArray(new String[] {}), buttons.values().toArray(new String[] {})));
        return payload;
    }

    // This is for the buttons template
    public static JSONArray createButtons (String [] button_titles, String [] button_payloads) throws Exception {
        if (button_titles == null || button_payloads == null || button_titles.length == 0 || button_payloads.length == 0 || button_titles.length != button_payloads.length) {
            throw new Exception ("Buttons titles and payloads mismatch");
        }

        JSONArray arr = new JSONArray ();
        for (int i = 0; i < button_titles.length; i++) {
            if (button_payloads[i].startsWith("http://") || button_payloads[i].startsWith("https://")) {
                arr.put((new JSONObject()).put("type", "web_url").put("title", button_titles[i]).put("url", button_payloads[i]));
            } else {
                arr.put((new JSONObject()).put("type", "postback").put("title", button_titles[i]).put("payload", button_payloads[i]));
            }
        }
        return arr;
    }

    // This is for the quick replies buttons
    public static JSONArray createQuickReplies (String [] button_titles, String [] button_payloads) throws Exception {
        if (button_titles == null || button_payloads == null || button_titles.length == 0 || button_payloads.length == 0 || button_titles.length != button_payloads.length) {
            throw new Exception ("Buttons titles and payloads mismatch");
        }

        JSONArray arr = new JSONArray ();
        for (int i = 0; i < button_titles.length; i++) {
            // Max out at 10
            if (i > 9) {
                break;
            }
            arr.put((new JSONObject()).put("content_type", "text").put("title", trim(button_titles[i], 20)).put("payload", button_payloads[i]));
        }
        return arr;
    }

    public static JSONObject createQuickReplies (String title, HashMap <String, String> buttons) throws Exception {
        JSONObject message = new JSONObject();
        message.put("text", title);
        message.put("quick_replies", createQuickReplies(
                buttons.keySet().toArray(new String[] {}),
                buttons.values().toArray(new String[] {})
        ));
        return message;
    }

    public HashMap <String, Object> getUserProfile (String fbid) {
        HashMap <String, Object> profile = new HashMap ();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("https://graph.facebook.com/v2.6/" + fbid + "?access_token=" + page_access_token);
            response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            response.close();

            JSONObject json = new JSONObject (respBody);
            Iterator<String> keysItr = json.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = json.get(key);
                profile.put(key, value);
            }


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

        return profile;
    }

    // This one returns an URL of the transcoded file. It uses zencoder to do this.
    public String transcodeAudio (String orgUrl, String target_format, int sample_rate) {
        String target_url = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://app.zencoder.com/api/v2/jobs");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader("Zencoder-Api-Key", zencoder_apikey);

            JSONObject json = new JSONObject();
            JSONArray outputs = new JSONArray ();
            JSONObject output = new JSONObject ();
            output.put("format", target_format);
            output.put("audio_sample_rate", sample_rate);
            outputs.put(output);
            json.put("input", orgUrl);
            json.put("outputs", outputs);
            System.out.println(json.toString());

            httpPost.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            response.close();
            System.out.println(respBody);

            json = new JSONObject(respBody);
            long job_id = json.getLong("id");
            target_url = ((JSONObject) json.getJSONArray("outputs").get(0)).getString("url");

            while (!respBody.startsWith("{\"state\":\"finished\"")) {
                Thread.sleep(1000);
                HttpGet httpGet = new HttpGet("https://app.zencoder.com/api/v2/jobs/" + job_id + "/progress.json?api_key=" + zencoder_apikey);
                response = httpclient.execute(httpGet);
                entity = response.getEntity();
                respBody = EntityUtils.toString(entity);
                response.close();
                System.out.println(respBody);
            }

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
        return target_url;
    }

    // This one uses IBM Watson Speech to Text service
    public String speechToText (String url, String format) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        byte [] sound_data;
        try {
            System.out.println("sound_data url is " + url);
            HttpGet httpGet = new HttpGet(url);
            response = httpclient.execute(httpGet);

            HttpEntity entity = response.getEntity();
            sound_data = EntityUtils.toByteArray(entity);
            System.out.println("sound_data length is " + sound_data.length);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String transcript = null;
        double confidence;
        try {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(audio_bluemix_username, audio_bluemix_password)
            );
            // HttpClientContext context = HttpClientContext.create();
            // context.setCredentialsProvider(credsProvider);
            httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            HttpPost httpPost = new HttpPost("https://stream.watsonplatform.net/speech-to-text/api/v1/recognize?timestamps=true&word_alternatives_threshold=0.9");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "audio/" + format);
            httpPost.setEntity(new ByteArrayEntity(sound_data));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            System.out.println(respBody);

            JSONObject json = new JSONObject(respBody);
            JSONObject alt = json.getJSONArray("results").getJSONObject(0).getJSONArray("alternatives").getJSONObject(0);
            transcript = alt.getString("transcript");
            confidence = alt.getDouble("confidence");

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

        return transcript;
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

    // This is for the generic template
    public static JSONObject createCarousel (List <String> titles, List <String> subtitles, List <String> image_urls, int num_of_buttons, List <String> button_titles, List <String> button_payloads) throws Exception {
        if (num_of_buttons > 3) {
            num_of_buttons = 3;
        }

        JSONArray arr = new JSONArray ();
        for (int i = 0; i < titles.size(); i++) {
            if (i > 9) {
                break;
            }

            JSONObject obj = new JSONObject ();
            if (!titles.isEmpty()) {
                obj.put("title", titles.get(i));
            }
            if (!subtitles.isEmpty()) {
                obj.put("subtitle", subtitles.get(i));
            }
            if (!image_urls.isEmpty()) {
                obj.put("image_url", image_urls.get(i));
            }

            JSONArray buttons = new JSONArray ();
            for (int j = 0; j < num_of_buttons; j++) {
                int index = i * num_of_buttons + j;
                if (button_payloads.get(index).startsWith("http://") || button_payloads.get(index).startsWith("https://")) {
                    buttons.put((new JSONObject()).put("type", "web_url").put("title", button_titles.get(index)).put("url", button_payloads.get(index)));
                } else {
                    buttons.put((new JSONObject()).put("type", "postback").put("title", button_titles.get(index)).put("payload", button_payloads.get(index)));
                }
            }
            if (buttons.length() > 0) {
                obj.put("buttons", buttons);
            }
            arr.put(obj);
        }
        // return arr;

        JSONObject payload = new JSONObject();
        payload.put("template_type", "generic");
        payload.put("elements", arr);
        return payload;
    }

    public static String trim(String s, int len) {
        return s.substring(0, Math.min(s.length(), len));
    }

    public static String fillSlot (List<Hashtable<String, String>>slots, String word) {
        for (Hashtable<String, String> slot : slots) {
            if (slot.get("value").isEmpty()) {
                if (!word.isEmpty()) {
                    if (word.trim().matches(slot.get("valid"))) {
                        slot.put("value", word.trim());
                        break;
                    } else {
                        return "Sorry, your input is not valid. " + slot.get("prompt");
                    }
                }
            }
        }

        for (Hashtable<String, String> slot : slots) {
            if (slot.get("value").isEmpty()) {
                return slot.get("prompt");
            }
        }

        return "";
    }
}
