import hypermedia.net.*;
import java.util.*;
import java.nio.*;
import processing.net.*;
import processing.awt.PSurfaceAWT.SmoothCanvas;
import javax.swing.JFrame;
import java.awt.Dimension;


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
 */


/*

TODO list:

- make the speed stuffs do stuffs
- re-make the speed texture
- clean up code

*/


UDP com;
String Z21_ADRESS = "192.168.178.111";
String CONNECT_STATUS = STAT_NO_CONNECTION;
Style GUIStyle;
Button Z21_connect;
Button Z21_disconnect;
Button LocRailway;
Button Z21_gostop;
Button Z21_stopAll;
Button Locked;
TextInput IP;
ScrollBar locLibScroll;
Text Connection;
Text SerialNum;
Text XBusVersion;
Text Firmware;
Text Operation;
Text Power;
Text Main;
Text Prog;
Text Temperature;
Text Current;
Text Warning;
Timer timer;
byte TRACK_STATE;
float Z21_FIRMWARE_VER;
boolean warn = false;
boolean inLocLib = true;
boolean start = false;
boolean locked = false; //ONLY for start models
boolean inMenu = false;
PREFReader PREF = new PREFReader();
PREF locIndex;
MenuScreen menu = null;
LocEntry[] locomotives;
Style locSelectStyle;
Button[] locSelectControlButtons;
Button[] locSelectEditButtons;
Text[] locSelectAdresses;
Text[] locSelectNames;
Text[] locSelectOwners;
Text locSelectAdress;
Text locSelectName;
Text locSelectOwner;
Button addLoc;
LocEntry controling;
PImage speed_low;
PImage speed_high;
PImage direction;
PImage icon;
ControlScreen controler;
boolean log = false;
DataRequest[] dataRequests;
final int maxWarnings = 255;
int numWarnings = 0;
static String[] logf = new String[1];
final String logfname = "log_" + day() + "-" + month() + "-" + year() + "_" + hour() + "-" + minute() + ".log";

void setup() {
  size(750, 500, P2D);
  ((SmoothCanvas) getSurface().getNative()).getFrame().setMinimumSize(new Dimension(760, 510));
  surface.setResizable(true);
  surface.setTitle("Z21 Railway System | PRE-ALPHA build 28");
  icon = loadImage("icon.png");
  if (icon != null) surface.setIcon(icon);
  dataRequests = new DataRequest[0];
  GUIStyle = new Style(255, 127, 1);
  Z21_connect = new Button(5, 5, 69, 19, "connect", false, new Runnable(){
    public void run() {
      connect();
    }
  });
  Z21_disconnect = new Button(80, 5, 69, 19, "disconnect", false, new Runnable(){
    public void run() {
      stopControl();
      disconnect();
    }
  });
  LocRailway = new Button(5, 35, 79, 19, "railway", false, new Runnable(){
    public void run() {
      locrailway();
    }
  });
  Z21_gostop = new Button(90, 35, 59, 19, "go/stop", false, STYLE_BUTTON_STOP, new Runnable() {
    public void run() {
      gostop();
    }
  });
  Z21_stopAll = new Button(0, 65, 69, 19, "stop all", false, STYLE_BUTTON_STOP, new Runnable() {
    public void run() {
      Z21_STOP_ALL();
    }
  });
  Locked = new Button(0, 0, 330, 19, "your z21 start is locked! click here to get unlock code.", false, STYLE_BUTTON_LOCKED, new Runnable() {
    public void run(){
      link("http://www.roco.cc/en/product/238834-unlock-0-0-0-0-0-004001/products.html");
    }
  });
  Connection = new Text(160, 20, "No connection.");
  SerialNum = new Text(260, 20, "Serial adress: ------");
  XBusVersion = new Text(400, 20, "X-Bus version: ----");
  Firmware = new Text(530, 20, "Firmware: ----");
  Operation = new Text(160, 50, "Operation: ------");
  Power = new Text(360, 50, "Power: -------");
  Main = new Text(475, 50, "Main: -----");
  Prog = new Text(565, 50, "Prog: -----");
  Temperature = new Text(655, 50, "Temp: ----");
  Warning = new Text(width / 2, height / 2, "", STYLE_TEXT_WARN);
  timer = new Timer();
  timer.scheduleAtFixedRate(new TimerTask() {
    public void run() {
      checkRailwayStatus();
    }
  }, 1000, 250);
  timer.scheduleAtFixedRate(new TimerTask() {
    public void run() {
      backup();
    }
  }, 1000, 60000);
  IP = new TextInput(0, 5, 120, 19, false, false, "IP adress");
  locLibScroll = new ScrollBar(true, 0, 65, 100, 10);
  PREF lastSession = PREF.load("lastSession.pref");
  if (lastSession != null) {
    PREFEntry lastIP = lastSession.get("IP");
    if (lastIP != null && lastIP.elements.length == 1) {
      IP.text = lastIP.elements[0];
      timer.schedule(new TimerTask(){public void run(){connect();}}, 200);
    }
  }
  locIndex = PREF.load("locIndex.pref");
  if (locIndex == null) {
    locIndex = new PREF();
    locIndex.set(new PREFEntry("num", new String[]{"0"}));
  }
  decodeLocIndex();
  locSelectStyle = new Style(255, 127, 1);
  locSelectAdress = new Text(5, 80, "Adress");
  locSelectName = new Text(55, 80, "Name");
  locSelectOwner = new Text(200, 80, "Owner");
  addLoc = new Button(0, 90, 69, 19, "add loc...", false, new Runnable() {
    public void run() {
      openMenu(new AddLocomotive());
    }
  });
  speed_low = loadImage("speed_low.png");
  speed_high = loadImage("speed_high.png");
  direction = loadImage("direction.png");
  controler = new ControlScreen();
  runSketch(new String[] {"ControlScreen"}, controler);
}

void draw() {
  background(255);
  doGUI();
  if (inMenu) {
    menu.render();
    if (menu.exited()) {
      inMenu = false;
      menu = null;
    }
  }
  checkSize();
}

void mouseWheel(MouseEvent event) {
  int scrolled = event.getCount();
  locLibScroll.scroll(scrolled);
}

void keyPressed() {
  if (!inMenu) {
    IP.render();
  }
  else menu.keyPress();
  if (keyCode == 114) backup();
}

void mousePressed() {
  if (!inMenu) {
    IP.select();
  }
  else menu.mousePress();
}

void warn(String message) {
  numWarnings ++;
  serr("WARNING: " + message);
  if (numWarnings > maxWarnings) {
    serr("FATAL: too many warnings!");
    super.exit();
  }
}

void serr(String message) {
  message += '\n';
  System.err.print(message);
  String[] split = split(message, '\n');
  if (logf[logf.length - 1] == null) logf[logf.length - 1] = "[" + tmmins() + "][System.err] ";
  int lind = logf.length - 1;
  logf = expand(logf, logf.length + split.length);
  for (int i = 0; i < split.length; i++) if (split[i] != null && !split[i].equals(null)) {
    if (i > 0) logf[lind + i] = "                       " + split[i];
    else logf[lind] += split[i];
  }
}

public static void print(String message) {
  System.out.print(message);
  String[] split = split(message, '\n');
  if (logf[logf.length - 1] == null) logf[logf.length - 1] = "[" + tmmins() + "][System.out] ";
  int lind = logf.length - 1;
  logf = expand(logf, logf.length + split.length);
  for (int i = 0; i < split.length; i++) if (split[i] != null && !split[i].equals(null)) {
    if (i > 0) logf[lind + i] = "                       " + split[i];
    else logf[lind] += split[i];
  }
}

public static void println(String message) {
  print(message + '\n');
}

public static String tmmins() {
  String hour = hour() + "";
  String second = second() + "";
  String minute = minute() + "";
  if (hour() <= 9) hour = "0" + hour;
  if (second() <= 9) second = "0" + second;
  if (minute() <= 9) minute = "0" + minute;
  return hour + ":" + minute + ":" + second;
}

void doSpeed(int x, int y, int speed, PGraphics p) {
  p.image(speed_low, x, y);
  PImage overlay = speed_high.get(0, speed_low.height - speed, 25, speed * (speed_low.height / 128));
  p.image(overlay, x + (speed_low.width - speed_high.width), y + (speed_low.height - speed));
}

void doLocSelect() {//add the rest of the GUI bits
  locSelectAdress.display();
  locSelectName.display();
  locSelectOwner.display();
  line(0, 89, width - 79, 89);
  line(50, 60, 50, 89 + locomotives.length * 30);
  line(195, 60, 195, 89 + locomotives.length * 30);
  line(width - 214, 90, width - 214, 89 + locomotives.length * 30);
  for (int i = 0; i < locomotives.length; i++) {
    locSelectEditButtons[i].enabled = !inMenu;
    locSelectControlButtons[i].enabled = CONNECT_STATUS.equals(STAT_CONNECTED) && !inMenu;
    locSelectEditButtons[i].x = width - 134;
    locSelectControlButtons[i].x = width - 209;
    locSelectEditButtons[i].render();
    locSelectControlButtons[i].render();
    locSelectAdresses[i].display();
    locSelectNames[i].display();
    locSelectOwners[i].display();
    locSelectStyle._set();
    line(0, 119 + i * 30, width - 79, 119 + i * 30);
  }
}

void decodeLocIndex() {
  int locs = 0;
  PREFEntry LRAW = locIndex.get("num");
  if (LRAW != null && LRAW.elements.length == 1) locs = int(LRAW.elements[0]);
  locomotives = new LocEntry[locs];
  for (int i = 0; i < locomotives.length; i++) {
    PREFEntry loc = locIndex.get(i + "");
    if (loc != null && loc.elements.length == 1) {
      locomotives[i] = new LocEntry(PREF.load(loc.elements[0].substring(1, loc.elements[0].length() - 1)));
    }
    else
    {
      locomotives[i] = new LocEntry(3, "Loc. data missing.", "", null);
    }
  }
  locSelectEditButtons = new Button[locomotives.length];
  locSelectControlButtons = new Button[locomotives.length];
  locSelectAdresses = new Text[locomotives.length];
  locSelectNames = new Text[locomotives.length];
  locSelectOwners = new Text[locomotives.length];
  for (int i = 0; i < locomotives.length; i++) {
    PREFEntry entry = locIndex.get(i + "");
    String file;
    try {
      file = entry.elements[0].substring(1, entry.elements[0].length() - 1);
    }
    catch(NullPointerException e) {
      file = "";
    }
    final String finFile = file;
    final int index = i;
    final LocEntry loc = locomotives[i];
    locSelectEditButtons[i] = new Button(0, 95 + i * 30, 49, 19, "edit", false, new Runnable() {
      public void run() {
        openMenu(new EditLocomotive(index, finFile));
      }
    });
    locSelectControlButtons[i] = new Button(0, 95 + i * 30, 69, 19, "controll", false, new Runnable() {
      public void run() {
        control(loc);
      }
    });
    locSelectAdresses[i] = new Text(5, 110 + i * 30, locomotives[i].adress + "");
    locSelectNames[i] = new Text(55, 110 + i * 30, locomotives[i].name);
    locSelectOwners[i] = new Text(200, 110 + i * 30, locomotives[i].owner);
  }
}

void control(LocEntry loc) {
  controling = loc;
  controler.setVisible(true);
}

void stopControl() {
  controling = null;
  controler.setVisible(false);
}

void openMenu(MenuScreen S_menu) {
  menu = S_menu;
  menu.init();
  inMenu = true;
}

void doGUI() {
  GUIStyle._set();
  line(0, 29, width - 1, 29);
  line(0, 59, width - 1, 59);
  line(154, 0, 154, 58);
  if (inLocLib) {
    line(width - 79, 60, width - 79, height - 1);
    doLocSelect();
  }
  else
  {
    rect(width - 79, 59, 80, 55);
    new TextStyle(0, 12, CENTER)._text("This function is not done yet.", width / 2, height / 2);
  }
  Z21_connect.enabled = CONNECT_STATUS.equals(STAT_NO_CONNECTION) || CONNECT_STATUS.equals(STAT_CONNECT_FAILED) && !inMenu;
  Z21_disconnect.enabled = CONNECT_STATUS.equals(STAT_CONNECTED) && !inMenu;
  LocRailway.enabled = !inMenu;
  Z21_gostop.enabled = CONNECT_STATUS.equals(STAT_CONNECTED) && !(start && locked) && !inMenu;
  Z21_stopAll.enabled = CONNECT_STATUS.equals(STAT_CONNECTED) && !(start && locked) && !inMenu;
  IP.enabled = CONNECT_STATUS.equals(STAT_NO_CONNECTION) || CONNECT_STATUS.equals(STAT_CONNECT_FAILED) && !inMenu;
  Z21_connect.render();
  Z21_disconnect.render();
  LocRailway.render();
  IP.display();
  Z21_gostop.render();
  Z21_stopAll.render();
  addLoc.render();
  Connection.display();
  SerialNum.display();
  XBusVersion.display();
  Firmware.display();
  Operation.display();
  Power.display();
  Main.display();
  Temperature.display();
  locLibScroll.display();
  if (!start) Prog.display();
  if (start && locked) Locked.render();
  if (warn) {
    rectMode(CENTER);
    fill(255);
    textSize(Warning.style.size);
    rect(width / 2, height / 2, textWidth(Warning.text), (textAscent() + textDescent()) * split(Warning.text, '\n').length + textAscent());
    rectMode(CORNER);
    Warning.display();
  }
}

void checkSize() {
  sketchWidth = width;
  sketchHeight = height;
  //if (width < 750) surface.setSize(750, height);
  //if (height < 500) surface.setSize(width, 500);
  Z21_stopAll.x = width - 74;
  Locked.x = width / 2 - 165;
  Locked.y = height / 2 - 10;
  IP.x = width - 125;
  textSize(Warning.style.size);
  Warning.x = width / 2;
  String text = Warning.text;
  Warning.y = int(height / 2 - ((textAscent() + textDescent()) * split(text, '\n').length + textAscent()) / 2 + textAscent());
  textSize(12);
  locLibScroll.x = width - 94;
  locLibScroll.length = height - 70;
  locLibScroll.max = (locomotives.length + 1) * 30 - (height - 60);
  addLoc.x = width - 74;
}

void checkRailwayStatus() {
  if (CONNECT_STATUS.equals(STAT_CONNECTED)) {
    if (!(start && locked)) {
      Z21_GET_STATUS_DATA();
      Connection.text = "Connected.";
      if (TRACK_STATE == 0x00) Operation.text = "Operation: normal";
      if (TRACK_STATE == 0x01) Operation.text = "Operation: EMERGENCY STOP!";
      if (TRACK_STATE == 0x02) Operation.text = "Operation: TRACK POWER OFF!";
      if (TRACK_STATE == 0x04) Operation.text = "Operation: SHORT CUIRCET!";
      if (TRACK_STATE == 0x06) Operation.text = "Operation: SHORT CUIRCET!";
      if (TRACK_STATE == 0x20) Operation.text = "Operation: normal, programming";
      if ((TRACK_STATE & 0x07) > 0) Operation.style.col = #ff0000;
      else if ((TRACK_STATE & 0x20) > 0) Operation.style.col = #00ff00;
      else Operation.style.col = 0;
      setgostop();
    }
  }
  if (CONNECT_STATUS.equals(STAT_CONNECTING)) Connection.text = "Connecting...";
  if (CONNECT_STATUS.equals(STAT_NO_CONNECTION)) Connection.text = "No connection.";
  if (CONNECT_STATUS.equals(STAT_CONNECT_FAILED)) Connection.text = "Connect failed.";
}

void connect() {
  if (!IP.text.equals("")) Z21_ADRESS = IP.text;
  else Z21_ADRESS = "192.168.178.111";
  CONNECT_STATUS = STAT_CONNECTING;
  com = new UDP(this, 21105);
  com.listen(true);
  Z21_GET_CODE();
  timer.schedule(new TimerTask() {
    public void run() {
      if (CONNECT_STATUS.equals(STAT_CONNECTING)) {
        CONNECT_STATUS = STAT_CONNECT_FAILED;
        com.close();
        com = null;
      }
    }
  }, 3000);
}

void disconnect() {
  if (com != null) {
    Z21_LOGOUT();
    CONNECT_STATUS = STAT_NO_CONNECTION;
    com.close();
    com = null;
    SerialNum.text = "Serial adress: ------";
    XBusVersion.text = "X-Bus version: ----";
    Firmware.text = "Firmware: ----";
    Operation.text = "operation: ------";
    Power.text = "Power: -------";
    Main.text = "Main: -----";
    Prog.text = "Prog: -----";
    Temperature.text = "Temp: ----";
    Z21_gostop.text = "go/stop";
    warn = false;
    start = false;
  }
  else println("Didn't have to disconnect!");
}

void setgostop() {
  if ((TRACK_STATE & 7) > 0) {
    Z21_gostop.style = STYLE_BUTTON_GO;
    Z21_gostop.text = "GO";
  }
  else
  {
    Z21_gostop.style = STYLE_BUTTON_STOP;
    Z21_gostop.text = "STOP";
  }
}

void gostop() {
  if ((TRACK_STATE & 7) > 0) {
    Z21_SET_TRACK_POWER(true);
  }
  else
  {
    Z21_SET_TRACK_POWER(false);
  }
}

void locrailway() {
  if (inLocLib) {
    LocRailway.text = "locomotives";
    inLocLib = false;
  }
  else
  {
    LocRailway.text = "railway";
    inLocLib = true;
  }
}

boolean send(byte[] head, byte[] data) {
  if (com == null) {
    println("we are not connected! message cannot be sent!");
    CONNECT_STATUS = STAT_NO_CONNECTION;
    return false;
  }
  if (CONNECT_STATUS.equals(STAT_NO_CONNECTION) || CONNECT_STATUS.equals(STAT_CONNECT_FAILED)) {
    println("we are not connected! message cannot be sent!");
    return false;
  }
  if (head.length == 1) {
    serr("head is 1 byte too short! bad practice, but no problem.");
    head = new byte[]{head[0], 0x00};
  }
  if (head.length == 0) {
    warn("head does not exist! message cannot be sent!");
    return false;
  }
  if (head.length > 2) {
    String n = "";
    if (head.length > 3) n = "s";
    warn("head is " + (head.length - 2) + " byte" + n + " too long! message cannot be sent!");
    return false;
  }
  if (data.length > 251) {
    String n = "";
    if (data.length > 252) n = "s";
    warn("data is " + (data.length - 251) + " byte" + n + " too long! (out of max. 256) message cannot be sent!");
  }
  String s = "";
  byte[] ndat = new byte[data.length + 4];
  ndat[0] = byte(4 + data.length & 0xff);
  ndat[1] = 0x00;
  ndat[2] = head[0];
  ndat[3] = head[1];
  for (int i = 0; i < data.length; i++) {
    ndat[i + 4] = data[i];
  }
  for (int i = 0; i < ndat.length; i++) {
    if (i > 0) s += " ";
    s += "0x" + hex(ndat[i]);
  }
  if (com.send(ndat, Z21_ADRESS, 21105) && log) println("sent packet to " + Z21_ADRESS + ":21105:\n" + s);
  return true;
}

void receive(byte[] data, String host, int port) {
  String text = host + ":" + port + " sent packet:\n";
  for (int i = 0; i < data.length; i++) {
    if (i > 0) text += " ";
    text += "0x" + hex(data[i]);
  }
  if (log) println(text);
  if (CONNECT_STATUS.equals(STAT_CONNECTING)) {
    timer.schedule(new TimerTask(){
      public void run() {
        if (!(start && locked)) {
          Z21_GET_SERIAL_NUM();
          Z21_GET_XBUS_VER();
          delay(20);
          Z21_GET_FIRMWARE_VER();
          Z21_SET_BROADCAST_FLAGS(0x00000101);
        }
      }
    }, 100);
  }
  CONNECT_STATUS = STAT_CONNECTED;
  process(data);
}

void process(byte[] data) {
  if (data.length >= 4) {
    byte[] len = new byte[]{data[0], data[1]};
    byte[] head = new byte[]{data[2], data[3]};
    byte[] ndat = new byte[data.length - 4];
    for (int i = 0; i < data.length - 4; i++) {
      ndat[i] = data[i + 4];
    }
    data = ndat;
    if (PROCESS_GET_SERIAL_NUM(len, head, data)) return;//2.1
    if (PROCESS_GET_XBUS_VER(len, head, data)) return;//2.3
    if (PROCESS_STATUS_BROADCAST(len, head, data)) return;//2.7 > 2.10
    if (PROCESS_UNKNOWN_COMMAND(len, head, data)) return;//2.11
    if (PROCESS_STATUS_CHANGE(len, head, data)) return;//2.12
    if (PROCESS_GET_FIRMWARE_VER(len, head, data)) return;//2.15
    if (PROCESS_GET_BROADCAST_FLAGS(len, head, data)) return;//2.17
    if (PROCESS_STATUS_DATA_CHANGE(len, head, data)) return;//2.18
    if (PROCESS_GET_HARDWARE_VER(len, head, data)) return;//2.20
    if (PROCESS_GET_CODE(len, head, data)) return;//2.21
    if (PROCESS_GET_LOC_MODE(len, head, data)) return;//3.1
    if (PROCESS_GET_TURNOUT_MODE(len, head, data)) return;//3.3
    if (PROCESS_LOC_INFO(len, head, data)) return;//4.4
    String text = "";
    for (int i = 0; i < data.length; i++) {
      if (i > 0) text += " ";
      text += "0x" + hex(data[i]);
    }
    serr("WARNING: Z21 reply is of an unknown type! packet:\n" + text);
  }
  else
  {
    serr("WARNING: message is incorrectly formatted!");
  }
}

void backup() {
  PREF session = new PREF(new PREFEntry[0]);
  if (!IP.text.equals("")) session.set(new PREFEntry("IP", new String[]{IP.text}));
  PREF.save("data/lastSession.pref", session);
  PREF.save("data/locIndex.pref", locIndex);
  saveStrings("logs/latest.log", logf);
  saveStrings("logs/" + logfname, logf);
  println("automatic backup made.");
}

void exit() {
  if (key != ESC) {
    disconnect();
    PREF session = new PREF(new PREFEntry[0]);
    if (!IP.text.equals("")) session.set(new PREFEntry("IP", new String[]{IP.text}));
    PREF.save("data/lastSession.pref", session);
    PREF.save("data/locIndex.pref", locIndex);
    super.exit();
  }
}