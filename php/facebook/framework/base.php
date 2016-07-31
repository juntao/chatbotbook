<?php
  // This block will be skipped if the hub.verify_token is null or empty
  if (strlen($_GET['hub_verify_token']) > 0) {
    echo $_GET['hub_challenge'];
    return;
  }

  require_once (dirname(__FILE__).'/phpFastCache/phpFastCache.php');
  use phpFastCache\CacheManager;

  $config = array(
    "storage"   =>  "Files",
    "path" => sys_get_temp_dir()
  );
  CacheManager::setup($config);

  $body = file_get_contents('php://input');
  error_log ($body);
  $json = json_decode($body, true);
  for($i=0; $i<count($json['entry']); $i++) {
    $messagings = $json['entry'][$i]['messaging'];
    for($j=0; $j<count($messagings); $j++) {
      $messaging = $messagings[$j];
      $sender_id = $messaging['sender']['id'];
      if (!empty($messaging['message']['quick_reply']['payload'])) {
        $message = $messaging['message']['quick_reply']['payload'];
      } elseif (!empty($messaging['message'])) {
        $message = $messaging['message']['text'];
      } elseif (!empty($messaging['postback'])) {
        $message = $messaging['postback']['payload'];
      }

      $chat_context = CacheManager::get($sender_id . "_context");
      if (is_null($chat_context)) {
        $chat_context = array ();
      }

      $bot_says = converse ($message, $chat_context);
      CacheManager::set($sender_id . "_context", $chat_context, 600); // cache for 600 seconds

      if (is_array($bot_says) && (!is_assoc($bot_says))) {
        // this is a "regular" non-assoc array
        foreach ($bot_says as $bs) {
          sendReply ($bs);
        }
      } else {
        // This has to be a string or an associative array!
        sendReply ($bot_says);
      }

    }
  }
  
  // INPUT PARAMS
  //   $human is the human message to this bot
  //   $context is an array that contains any data related to this chat session. You can put any data here and access it later.

  // RETURNS the bot's next message to the human

  // function converse ($human, &$context) {
    // IMPLEMENT ME!
  // }

  function is_assoc ($arr) {
    return (is_array($arr) && count(array_filter(array_keys($arr),'is_string')) == count($arr));
  }
 
  // This $msg must be a string or an associative array
  function sendReply ($msg) {
    global $sender_id, $page_access_token;
    $arr = array ();
    if (is_string($msg)) {
      $arr = array(
        "recipient" => array("id" => $sender_id),
        "message" => array("text" => $msg)
      );

    } elseif (is_assoc($msg) && (!empty($msg['template_type']))) {
      $arr = array(
        "recipient" => array("id" => $sender_id),
        "message" => array(
          "attachment" => array (
            "type" => "template",
            "payload" => $msg
          )
        )
      );

    } elseif (is_assoc($msg) && (!empty($msg['quick_replies']))) {
      $arr = array(
        "recipient" => array("id" => $sender_id),
        "message" => $msg
      );

    } elseif (is_assoc($msg) && (!empty($msg['payload']['url']))) {
      $arr = array(
        "recipient" => array("id" => $sender_id),
        "message" => array(
          "attachment" => $msg
        )
      );
    }

    if (empty($arr)) {
      error_log("Not a valid reply message!");
      return;
    }

    $url = "https://graph.facebook.com/v2.6/me/messages?access_token=" . $page_access_token;
    $content = json_encode($arr);
    error_log($url);
    error_log($content);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER,
      array("Content-type:application/json")
    );
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
    curl_setopt($ch, CURLOPT_POSTFIELDS, $content);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

    $resp = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    error_log($status . " " . $resp);
  }

  function createButtons ($title, $arr) {
    $payload = array ();
    $payload['template_type'] = "button";
    if (!empty($title)) {
        $payload['text'] = $title;
    }
    $payload['buttons'] = array ();
    foreach ($arr as $key => $value) {
      if (substr($value, 0, 4) === "http") {
        array_push($payload['buttons'], array (
          "type" => "web_url",
          "title" => $key,
          "url" => $value
        ));
      } else {
        array_push($payload['buttons'], array (
          "type" => "postback",
          "title" => $key,
          "payload" => $value
        ));
      }
    }
    return $payload;
  }

  function createCarousel ($titles, $subtitles, $image_urls, $num_of_buttons, $button_titles, $button_payloads) {
    if ($num_of_buttons > 3) {
      $num_of_buttons = 3;
    }

    $arr = array ();
    for ($i = 0; $i < count($titles); $i++) {
      if ($i > 9) {
        break;
      }
      $obj = array ();
      if (!empty($titles)) {
        $obj['title'] = $titles[$i];
      }
      if (!empty($subtitles)) {
        $obj['subtitle'] = $subtitles[$i];
      }
      if (!empty($image_urls)) {
        $obj['image_url'] = $image_urls[$i];
      }

      $buttons = array ();
      for ($j = 0; $j < $num_of_buttons; $j++) {
        $index = $i * $num_of_buttons + $j;
        if (substr($button_payloads[$index], 0, 4) === "http") {
          array_push($buttons, array(
            "type" => "web_url",
            "title" => $button_titles[$index],
            "url" => $button_payloads[$index]
          ));
        } else {
          array_push($buttons, array(
            "type" => "postback",
            "title" => $button_titles[$index],
            "payload" => $button_payloads[$index]
          ));
        }
      }
      if (!empty($buttons)) {
        $obj['buttons'] = $buttons;
      }
      array_push($arr, $obj);
    }
    // return $arr;
    return array(
      "template_type" => "generic",
      "elements" => $arr
    );
  }

  function createQuickReplies ($title, $arr) {
    // This will truncate $arr to first 10 elements if needed
    $i = 0;
    $qrs = array ();
    foreach ($arr as $key => $value) {
      $i = $i + 1;
      if ($i > 10) {
        break;
      }
      array_push($qrs, array (
        "content_type" => "text",
        "title" => (strlen($key) > 19) ? substr($key,0,16).'...' : $key,
        "payload" => $value
      ));
    }

    $bot_resp = array ();
    $bot_resp['text'] = $title;
    $bot_resp['quick_replies'] = $qrs;
    return $bot_resp;
  }

  function fill_slot(&$slots, $word) {
    foreach ($slots as &$slot) {
      // fill the first empty slot
      if (empty($slot['value'])) {
        if (!empty($word)) {
          if (preg_match($slot['valid'], $word) == 1) {
            $slot['value'] = trim($word);
            break;
          } else {
            return "Sorry, your input is not valid. " . $slot['prompt'];
          }
        }
      }
    }
    unset($slot);

    foreach ($slots as $slot) {
      // return the prompt for the next empty slot
      if (empty($slot['value'])) {
        return $slot['prompt'];
      }
    }
    return "";
  }
?>
