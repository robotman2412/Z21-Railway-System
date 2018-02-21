

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
 */


static final String STAT_NO_CONNECTION = "NO_CONNECTION";
static final String STAT_CONNECTING = "CONNECTING";
static final String STAT_CONNECTED = "CONNECTED";
static final String STAT_CONNECT_FAILED = "CONNECT_FAILED";

static final int ON = 1;
static final int OFF = 0;
static final int FLIP = 2;

static final boolean FOREWARD = true;
static final boolean BACKWARD = false;

final TextStyle STYLE_TEXT_WARN = new TextStyle(#ff0000, 24, CENTER);

final ButtonStyle STYLE_BUTTON_STOP = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(#ff0000, 127, 1), new Style(#ff5555, #60afff, 3), new Style(#cc0000, #656691, 1), new Style(255, 200, 1));
final ButtonStyle STYLE_BUTTON_GO = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(#00ff00, 127, 1), new Style(#55ff55, #60afff, 3), new Style(#00cc00, #656691, 1), new Style(255, 200, 1));
final ButtonStyle STYLE_BUTTON_LOCKED = new ButtonStyle(new TextStyle(0), new TextStyle(0), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1));