<?php
  $app_id = "SKYPE-APPID";
  $app_secret = "SKYPE-SECRET";

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

  $body = file_get_contents('php://input');
  error_log ($body);
  $entry = json_decode($body, true);

  if (!empty($token)) {
    if ($entry['type'] != 'message' && $entry['type'] != 'message/text') {
      continue;
    }
    $content = $entry['text'];
    $cid = $entry['conversation']['id'];

    $arr = array(
      "type" => "message/text",
      "text" => $content
    );
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
  }
?>
