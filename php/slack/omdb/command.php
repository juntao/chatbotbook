<?php
  require_once ('../framework/command.php');

  function converse ($human, &$context) {
    $json_str = file_get_contents("http://www.omdbapi.com/?t=" . urlencode($human) . "&r=json");
    $json = json_decode($json_str, true);
    if ($json['Response'] == "False") {
        return "No movie is found";
    }

    $arr = array ();
    // $arr['response_type'] = "in_channel";
    $arr['text'] = $json['Title'] . " [" . $json['Year'] . "] ";
    $arr['attachments'] = array ();
    array_push($arr['attachments'], array(
        "text"=>$json['Plot'],
        "fields"=>array(
            array("title"=>"Rating", "value"=>$json['imdbRating'], "short"=>true),
            array("title"=>"Votes",  "value"=>$json['imdbVotes'], "short"=>true)
        )
    ));
    array_push($arr['attachments'], array(
        "title"=>"Detail",
        "title_link"=>"http://www.imdb.com/title/" . $json['imdbID']
    ));
    // The IMDB image server blocks Slack for some reason:
    // https://github.com/matiassingers/slack-movie/issues/1
    // error_log("POSTER : " . $json['Poster']);
    // array_push($arr['attachments'], array(
    //     "image_url"=>$json['Poster']
    // ));
    return $arr;
  }
?>
