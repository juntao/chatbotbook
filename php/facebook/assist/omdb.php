<?php
  $page_access_token = "FB-PAGE-TOKEN";
  require_once ('../framework/base.php');

  function converse ($human, &$context) {
    $json_str = file_get_contents("http://www.omdbapi.com/?s=" . urlencode($human) . "&r=json");
    $json = json_decode($json_str, true);
    $selections = $json['Search'];
    if (empty($selections)) {
        return "No movie is found";
    }

    $num_of_buttons = 3;
    $titles = array ();
    $subtitles = array ();
    $image_urls = array ();
    $button_titles = array ();
    $button_payloads = array ();
    for($i=0; $i<count($selections); $i++) {
      $selected = $selections[$i];
      array_push($titles, $selected['Year']);
      array_push($subtitles, $selected['Title']);
      array_push($image_urls, $selected['Poster']);
      for ($j = 0; $j < $num_of_buttons; $j++) {
        array_push($button_titles, "Detail");
        array_push($button_payloads, "http://www.imdb.com/title/" . $selected['imdbID']);
        array_push($button_titles, "Showtimes");
        array_push($button_payloads, "http://www.imdb.com/showtimes/title/" . $selected['imdbID']);
        array_push($button_titles, "Trivia");
        array_push($button_payloads, "http://www.imdb.com/title/" . $selected['imdbID'] . "/trivia");
      }
    }
    return createCarousel ($titles, $subtitles, $image_urls, $num_of_buttons, $button_titles, $button_payloads);
    /*
    return array(
      "template_type" => "generic",
      "elements" => createElements($titles, $subtitles, $image_urls, $num_of_buttons, $button_titles, $button_payloads) 
    );
    */
  }

  function converse2 ($human, &$context) {
    // Search the OMDB
    if (is_null($context['selections'])) {
        $json_str = file_get_contents("http://www.omdbapi.com/?s=" . urlencode($human) . "&r=json");
        $json = json_decode($json_str, true);
        $selections = $json['Search'];
        if (empty($selections)) {
            return "No movie is found";
        }

        $context['selections'] = $selections;
        $replies = array ();
        for($i=0; $i<count($selections); $i++) {
            $replies[$selections[$i]['Title']] = strval($i);
        }
        return createQuickReplies (
          "Please select one of the movies below",
          $replies
        );
        /*
        $bot_resp = array ();
        $bot_resp['text'] = "Please select one of the movies below";
        $bot_resp['quick_replies'] = createQuickReplies ($replies);
        return $bot_resp;
        */

    } else {
        $selected = $context['selections'][intval($human)];
        $context['selections'] = NULL;

        $bot_resp = array ();
        array_push($bot_resp, array (
            "type" => "image", 
            "payload" => array ("url" => $selected['Poster'])
        ));
        array_push($bot_resp, createButtons (
          $selected['Title']." (".$selected['Year'].")",
          array (
            "Detail" => "http://www.imdb.com/title/" . $selected['imdbID'],
            "Showtimes" => "http://www.imdb.com/showtimes/title/" . $selected['imdbID'],
            "Trivia" => "http://www.imdb.com/title/" . $selected['imdbID'] . "/trivia"
          )
        ));
        return $bot_resp;
    }

  }
?>
