<?php
  $page_access_token = "FB-PAGE-TOKEN";
  require_once ('../framework/base.php');

  function converse ($human, &$context) {
    $human_cap = strtoupper($human);

    if (0 === strpos($human_cap, 'MOVIE')) {
      $context['mode'] = "movie";
      if ($human_cap == "MOVIE") {
        $human = "";
      } else {
        $human = trim(substr($human, 5));
      }
    }
    if (0 === strpos($human_cap, 'WEATHER')) {
      $context['mode'] = "weather";
      if ($human_cap == "WEATHER") {
        $human = "";
      } else {
        $human = trim(substr($human, 7));
      }
    }
    if ($human_cap == "STOP" || $human_cap == "END") {
      unset($context['mode']);
      return "Available commands are 'movie' and 'weather'";
    }

    if ($context['mode'] == "movie") {
      if (empty($human)) {
        return "Please type in the movie title.";
      }

      $json_str = file_get_contents("http://www.omdbapi.com/?s=" . urlencode($human) . "&r=json");
      $json = json_decode($json_str, true);
      $selections = $json['Search'];
      if (empty($selections)) {
        return "No movie is found. Please enter another title.";
      }

      unset($context['mode']);
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
        if (substr($selected['Poster'], 0, 4) === "http") {
          array_push($image_urls, $selected['Poster']);
        } else {
          array_push($image_urls, NULL);
        }
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
    }

    if ($context['mode'] == "weather") {
      if (empty($context['weather_slots'])) {
        $context['weather_slots'] = array (
          array ("prompt"=>"Please type 'now' for current conditions, and 'future' for 3 day forcast", "valid"=>"/^(now)|(future)$/i", "value"=>""),
          array ("prompt"=>"Please type in a city name", "valid"=>"/^[A-Za-z]+$/", "value"=>"")
        );
      }

      foreach (explode(' ', trim($human)) as $word) {
        // fill slot for word
        $resp = fill_slot($context['weather_slots'], $word);
      }

      // We have not filled all slots
      if (!empty($resp)) {
        return $resp;
      }
      
      // All slots are filled!

      if (strtolower($context['weather_slots'][0]['value']) == 'now') {
        $json_str = file_get_contents("http://api.openweathermap.org/data/2.5/weather?q=" . urlencode($context['weather_slots'][1]['value']) . "&units=imperial&appid=756395849259d92214b1ec2975aa667e");
        $json = json_decode($json_str, true);
        unset($context['weather_slots']);
        unset($context['mode']);

        return $json['name'] . ": " . $json['weather'][0]['description'] . " Temp: " . $json['main']['temp'] . "F" . " Humidity: " . $json['main']['humidity'] . "%";
      }

      if (strtolower($context['weather_slots'][0]['value']) == 'future') {
        $json_str = file_get_contents("http://api.openweathermap.org/data/2.5/forecast?q=" . urlencode($context['weather_slots'][1]['value']) . "&units=imperial&appid=756395849259d92214b1ec2975aa667e");
        $json = json_decode($json_str, true);
        unset($context['weather_slots']);
        unset($context['mode']);

        $num_of_buttons = 0;
        $titles = array ();
        $subtitles = array ();
        $image_urls = array ();
        $button_titles = array ();
        $button_payloads = array ();
        for($i=0; $i<count($json['list']); $i++) {
          array_push($titles, $json['list'][$i]['dt_txt']);
          array_push($subtitles, $json['list'][$i]['weather'][0]['description'] . " " . $json['list'][$i]['main']['temp'] . "F");
          array_push($image_urls, "http://openweathermap.org/img/w/" . $json['list'][$i]['weather'][0]['icon'] . ".png");
        }
        return createCarousel ($titles, $subtitles, $image_urls, $num_of_buttons, $button_titles, $button_payloads);
      }

      unset($context['weather_slots']);
      unset($context['mode']);
      return "Sorry, there is an error!";
    }

    unset($context['mode']);
    return "Available commands are 'movie' and 'weather'";
  }
?>
