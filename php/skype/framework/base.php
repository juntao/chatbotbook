<?php
  require_once (dirname(__FILE__).'/phpFastCache/phpFastCache.php');
  use phpFastCache\CacheManager;

  $config = array(
    "storage"   =>  "Files",
    "path" => sys_get_temp_dir()
  );
  CacheManager::setup($config);

  $token = CacheManager::get("token");
  $token_exp_date = CacheManager::get("token_exp_date");
  if (empty($token_exp_date)) {
    // Any past time will do
    $token_exp_date = strtotime('-3 days');
  }

  if (time() > $token_exp_date) {
    $token = NULL;
  }
 
  if (empty($token)) {
    $url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    $post_data = array(
      'client_id' => $app_id,
      'client_secret' => $app_secret,
      'grant_type' => 'client_credentials',
      'scope' => 'https://graph.microsoft.com/.default'
    );

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HEADER, false);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $post_data);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

    $resp = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    error_log($status . " " . $resp);

    $json = json_decode($resp, true);
    $token = $json['access_token'];
    $expires_in = $json['expires_in'];
    if ($expires_in > 10) {
      $expires_in = $expires_in - 10;
    }

    CacheManager::set("token", $token);
    CacheManager::set("token_exp_date", strtotime('+' . $expires_in . ' seconds'));
  }

  $body = file_get_contents('php://input');
  error_log ($body);
  $entry = json_decode($body, true);

  if (!empty($token)) {
    if ($entry['type'] != 'message' && $entry['type'] != 'message/text') {
      continue;
    }
    $content = $entry['text'];
    $cid = $entry['conversation']['id'];
    $fromDisplayName = $entry['from']['name'];

    error_log("content = " . $content);

    $chat_context = CacheManager::get($cid . "_context");
    if (is_null($chat_context)) {
      $chat_context = array ();
    }
    $chat_context['cid'] = $cid;
    $chat_context['fromDisplayName'] = $fromDisplayName;

    $bot_says = converse ($content, $chat_context);
    CacheManager::set($cid . "_context", $chat_context, 600); // cache for 600 seconds

    if (is_array($bot_says) && (!is_assoc($bot_says))) {
      // this is a "regular" non-assoc array
      foreach ($bot_says as $bs) {
        sendReply ($bs);
      }
    } else {
      sendReply ($bot_says);
    }
  }

  // INPUT PARAMS
  //   $human is the human message to this bot
  //   $context is an array that contains any data related to this chat session. You can put any data here and access it later.

  // RETURNS the bot's next message to the human

  // function converse ($human, &$context) {
    // IMPLEMENT ME!
  // }

  function createHero ($title, $subtitle, $images, $buttons) {
    $arr = array ();
    $arr['contentType'] = "application/vnd.microsoft.card.hero";
    $arr['content'] = array ();

    if (!empty($title)) {
      $arr['content']['title'] = $title;
    }
    if (!empty($subtitle)) {
      $arr['content']['subtitle'] = $subtitle;
    }
    if (!empty($images)) {
      $arr['content']['images'] = array ();
      foreach ($images as $image) {
        array_push($arr['content']['images'], array ("url" => $image));
      }
    }
    if (!empty($buttons)) {
      $arr['content']['buttons'] = array ();
      foreach ($buttons as $button) {
        if (substr($button['value'], 0, 4) === "http") {
          array_push($arr['content']['buttons'], array(
            "type" => "openUrl",
            "title" => $button['title'],
            "value" => $button['value']
          ));
        } else {
          array_push($arr['content']['buttons'], array(
            "type" => "imBack",
            "title" => $button['title'],
            "value" => $button['value']
          ));
        }
      }
    }
    return $arr;
  }

  function is_assoc ($arr) {
    return (is_array($arr) && count(array_filter(array_keys($arr),'is_string')) == count($arr));
  }

  // This $msg must be a string or an associative array
  function sendReply ($msg) {
    global $token, $cid;

    $arr = array ();
    if (is_string($msg)) {
      $arr = array(
        "type" => "message/text",
        "text" => $msg
      );
    }

    if (is_assoc($msg)) {
      $arr = array(
        "type" => "message/card.carousel",
        "attachments" => array($msg)
      );
    }

    if (empty($arr)) {
      error_log("Not a valid reply message!");
      return;
    }

    $url = "https://df-apis.skype.com/v3/conversations/" . $cid . "/activities";
    $post_data = json_encode($arr);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER,
      array("Content-type: application/json", "Authorization: Bearer ".$token)
    );
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
    curl_setopt($ch, CURLOPT_POSTFIELDS, $post_data);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

    $resp = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    error_log($status . " " . $resp);
  }
?>
