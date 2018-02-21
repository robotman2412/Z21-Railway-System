import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import hypermedia.net.*; 
import java.util.*; 
import java.nio.*; 
import processing.net.*; 
import processing.awt.PSurfaceAWT.SmoothCanvas; 
import javax.swing.JFrame; 
import java.awt.Dimension; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Z21 extends PApplet {










/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
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

public void setup() {
  
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

public void draw() {
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

public void mouseWheel(MouseEvent event) {
  int scrolled = event.getCount();
  locLibScroll.scroll(scrolled);
}

public void keyPressed() {
  if (!inMenu) {
    IP.render();
  }
  else menu.keyPress();
  if (keyCode == 114) backup();
}

public void mousePressed() {
  if (!inMenu) {
    IP.select();
  }
  else menu.mousePress();
}

public void warn(String message) {
  numWarnings ++;
  serr("WARNING: " + message);
  if (numWarnings > maxWarnings) {
    serr("FATAL: too many warnings!");
    super.exit();
  }
}

public void serr(String message) {
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

public void doSpeed(int x, int y, int speed, PGraphics p) {
  p.image(speed_low, x, y);
  PImage overlay = speed_high.get(0, speed_low.height - speed, 25, speed * (speed_low.height / 128));
  p.image(overlay, x + (speed_low.width - speed_high.width), y + (speed_low.height - speed));
}

public void doLocSelect() {//add the rest of the GUI bits
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

public void decodeLocIndex() {
  int locs = 0;
  PREFEntry LRAW = locIndex.get("num");
  if (LRAW != null && LRAW.elements.length == 1) locs = PApplet.parseInt(LRAW.elements[0]);
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

public void control(LocEntry loc) {
  controling = loc;
  controler.setVisible(true);
}

public void stopControl() {
  controling = null;
  controler.setVisible(false);
}

public void openMenu(MenuScreen S_menu) {
  menu = S_menu;
  menu.init();
  inMenu = true;
}

public void doGUI() {
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

public void checkSize() {
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
  Warning.y = PApplet.parseInt(height / 2 - ((textAscent() + textDescent()) * split(text, '\n').length + textAscent()) / 2 + textAscent());
  textSize(12);
  locLibScroll.x = width - 94;
  locLibScroll.length = height - 70;
  locLibScroll.max = (locomotives.length + 1) * 30 - (height - 60);
  addLoc.x = width - 74;
}

public void checkRailwayStatus() {
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
      if ((TRACK_STATE & 0x07) > 0) Operation.style.col = 0xffff0000;
      else if ((TRACK_STATE & 0x20) > 0) Operation.style.col = 0xff00ff00;
      else Operation.style.col = 0;
      setgostop();
    }
  }
  if (CONNECT_STATUS.equals(STAT_CONNECTING)) Connection.text = "Connecting...";
  if (CONNECT_STATUS.equals(STAT_NO_CONNECTION)) Connection.text = "No connection.";
  if (CONNECT_STATUS.equals(STAT_CONNECT_FAILED)) Connection.text = "Connect failed.";
}

public void connect() {
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

public void disconnect() {
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

public void setgostop() {
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

public void gostop() {
  if ((TRACK_STATE & 7) > 0) {
    Z21_SET_TRACK_POWER(true);
  }
  else
  {
    Z21_SET_TRACK_POWER(false);
  }
}

public void locrailway() {
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

public boolean send(byte[] head, byte[] data) {
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
  ndat[0] = PApplet.parseByte(4 + data.length & 0xff);
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

public void receive(byte[] data, String host, int port) {
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

public void process(byte[] data) {
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

public void backup() {
  PREF session = new PREF(new PREFEntry[0]);
  if (!IP.text.equals("")) session.set(new PREFEntry("IP", new String[]{IP.text}));
  PREF.save("data/lastSession.pref", session);
  PREF.save("data/locIndex.pref", locIndex);
  saveStrings("logs/latest.log", logf);
  saveStrings("logs/" + logfname, logf);
  println("automatic backup made.");
}

public void exit() {
  if (key != ESC) {
    disconnect();
    PREF session = new PREF(new PREFEntry[0]);
    if (!IP.text.equals("")) session.set(new PREFEntry("IP", new String[]{IP.text}));
    PREF.save("data/lastSession.pref", session);
    PREF.save("data/locIndex.pref", locIndex);
    super.exit();
  }
}


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


class Style {
  int fill;
  int stroke;
  float strokeWeight;
  Style(int S_fill, int S_stroke, float S_strokeWeight) {
    fill = S_fill;
    stroke = S_stroke;
    strokeWeight = S_strokeWeight;
  }
  public PGraphics _set(PGraphics graphics) {
    graphics.fill(fill);
    graphics.stroke(stroke);
    graphics.strokeWeight(strokeWeight);
    return graphics;
  }
  public void _set() {
    fill(fill);
    stroke(stroke);
    strokeWeight(strokeWeight);
  }
  public void _rect(float x, float y, float width, float height) {
    fill(fill);
    stroke(stroke);
    strokeWeight(strokeWeight);
    rect(x, y, width, height);
  }
  public void _ellipse(float x, float y, float width, float height) {
    fill(fill);
    stroke(stroke);
    strokeWeight(strokeWeight);
    ellipse(x, y, width, height);
  }
  public void _line(float bx, float by, float ex, float ey) {
    fill(fill);
    stroke(stroke);
    strokeWeight(strokeWeight);
    line(bx, by, ex, ey);
  }
  public void _rect(float x, float y, float width, float height, PGraphics graphics) {
    graphics.fill(fill);
    graphics.stroke(stroke);
    graphics.strokeWeight(strokeWeight);
    graphics.rect(x, y, width, height);
  }
  public void _ellipse(float x, float y, float width, float height, PGraphics graphics) {
    graphics.fill(fill);
    graphics.stroke(stroke);
    graphics.strokeWeight(strokeWeight);
    graphics.ellipse(x, y, width, height);
  }
  public void _line(float bx, float by, float ex, float ey, PGraphics graphics) {
    graphics.fill(fill);
    graphics.stroke(stroke);
    graphics.strokeWeight(strokeWeight);
    graphics.line(bx, by, ex, ey);
  }
}

class TextStyle {
  int col;
  float size;
  int align;
  TextStyle(int S_col, float S_size, int S_align) {
    col = S_col;
    size = S_size;
    align = S_align;
  }
  TextStyle(int S_col) {
    col = S_col;
    size = 12;
    align = CORNER;
  }
  public void _set() {
    fill(col);
    textSize(size);
    textAlign(align);
  }
  public void _set(PGraphics graphics) {
    graphics.fill(col);
    graphics.textSize(size);
    graphics.textAlign(align);
  }
  public void _text(String text, float x, float y) {
    fill(col);
    textSize(size);
    textAlign(align);
    text(text, x, y);
  }
  public void _text(String text, float x, float y, PGraphics graphics) {
    graphics.fill(col);
    graphics.textSize(size);
    graphics.textAlign(align);
    graphics.text(text, x, y);
  }
}

class Text {
  float x;
  float y;
  String text;
  TextStyle style;
  Text(float S_x, float S_y, String S_text, TextStyle S_style) {
    x = S_x;
    y = S_y;
    text = S_text;
    style = S_style;
  }
  Text(float S_x, float S_y, String S_text) {
    x = S_x;
    y = S_y;
    text = S_text;
    style = new TextStyle(0);
  }
  public void display() {
    style._text(text, x, y);
  }
  public void display(PGraphics p) {
    style._text(text, x, y, p);
  }
}

class CheckboxStyle {
  Style check;
  Style cross;
  Style box;
  Style disabled;
  CheckboxStyle() {
    check = new Style(255, 0xff00ff00, 1);
    cross = new Style(255, 0xffff0000, 1);
    box = new Style(255, 127, 1);
    disabled = new Style(255, 192, 1);
  }
  CheckboxStyle(Style S_check, Style S_cross, Style S_box, Style S_disabled) {
    check = S_check;
    cross = S_cross;
    box = S_box;
    disabled = S_disabled;
  }
}

class Checkbox {
  int x;
  int y;
  int width;
  int height;
  boolean value;
  boolean enabled;
  CheckboxStyle style;
  Runnable onChange;
  Checkbox(int S_x, int S_y, int S_width, int S_height) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    value = false;
    enabled = true;
    style = new CheckboxStyle();
    onChange = null;
  }
  Checkbox(int S_x, int S_y, int S_width, int S_height, CheckboxStyle S_style) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    value = false;
    enabled = true;
    style = S_style;
    onChange = null;
  }
  Checkbox(int S_x, int S_y, int S_width, int S_height, Runnable S_onChange) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    value = false;
    enabled = true;
    style = new CheckboxStyle();
    onChange = S_onChange;
  }
  Checkbox(int S_x, int S_y, int S_width, int S_height, CheckboxStyle S_style, Runnable S_onChange) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    value = false;
    enabled = true;
    style = S_style;
    onChange = S_onChange;
  }
  public void display() {
    if (enabled) {
      if (value) {
        style.box._set();
        rect(x, y, width, height);
        style.check._set();
        line(x, y + height / 2, x + width / 2, y + height);
        line(x + width / 2, y + height, x + width, y);
      }
      else
      {
        style.box._set();
        rect(x, y, width, height);
        style.cross._set();
        line(x, y, x + width, y + height);
        line(x + width, y, x, y + height);
      }
    }
    else
    {
      style.disabled._set();
      rect(x, y, width, height);
      if (value) {
        line(x, y + height / 2, x + width / 2, y + height);
        line(x + width / 2, y + height, x + width, y);
      }
      else
      {
        line(x, y, x + width, y + height);
        line(x + width, y, x, y + height);
      }
    }
  }
  //do render in mousePressed
  public void render() {
    if (enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) value = !value;
  }
}

class DropdownStyle {
  TextStyle text;
  TextStyle textDisabled;
  Style normal;
  Style disabled;
  int selected;
  int hover;
  DropdownStyle() {
    text = new TextStyle(0);
    textDisabled = new TextStyle(192);
    normal = new Style(255, 127, 1);
    disabled = new Style(255, 192, 1);
    selected = 192;
    hover = 0xff60afff;
  }
  DropdownStyle(TextStyle S_text, TextStyle S_textDisabled, Style S_normal, Style S_disabled) {
    text = S_text;
    textDisabled = S_textDisabled;
    normal = S_normal;
    disabled = S_disabled;
  }
}

class DropdownElement {
  String id;
  String value;
  DropdownElement(String S_id, String S_value) {
    id = S_id;
    value = S_value;
  }
}

class Dropdown {
  int x;
  int y;
  int width;
  int height;
  int selectedIndex;
  boolean open;
  boolean enabled;
  DropdownElement[] elements;
  DropdownStyle style;
  SidedRunnable onChange;
  Dropdown(int S_x, int S_y, int S_width, int S_height, DropdownElement[] S_elements) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    selectedIndex = 0;
    open = false;
    enabled = true;
    elements = S_elements;
    style = new DropdownStyle();
    onChange = null;
  }
  Dropdown(int S_x, int S_y, int S_width, int S_height, DropdownElement[] S_elements, DropdownStyle S_style) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    selectedIndex = 0;
    open = false;
    enabled = true;
    elements = S_elements;
    style = S_style;
    onChange = null;
  }
  Dropdown(int S_x, int S_y, int S_width, int S_height, DropdownElement[] S_elements, SidedRunnable S_onChange) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    selectedIndex = 0;
    open = false;
    enabled = true;
    elements = S_elements;
    style = new DropdownStyle();
    onChange = S_onChange;
  }
  Dropdown(int S_x, int S_y, int S_width, int S_height, DropdownElement[] S_elements, DropdownStyle S_style, SidedRunnable S_onChange) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    selectedIndex = 0;
    open = false;
    enabled = true;
    elements = S_elements;
    style = S_style;
    onChange = S_onChange;
  }
  public void render() {
    if (open && enabled) {
      style.normal._set();
      rect(x, y, width, height);
      line(x + width - height + height / 4, y + (height / 4) * 3, x + width - height / 2, y + height / 4);
      line(x + width - height / 4, y + (height / 4) * 3, x + width - height / 2, y + height / 4);
      int optionHeight = elements.length * (height / 3 * 2) + 1;
      int optiony = y;
      if (optiony + optionHeight > sketchHeight) optiony -= optiony + optionHeight - sketchHeight + 1;
      rect(x, optiony, width - height, optionHeight);
      for (int i = 0; i < elements.length; i++) {
        if (mouseX >= x && mouseX < x + width - height && mouseY >= optiony + (height / 3 * 2) * i && mouseY < optiony + (height / 3 * 2) * (i + 1)) {
          noStroke();
          fill(style.hover);
          rect(x + 1, optiony + (height / 3 * 2) * i + 1, width - height - 1, height / 3 * 2);
          if (mousePressed) {
            if (onChange != null) onChange.pre();
            selectedIndex = i;
            open = false;
            if (onChange != null) onChange.post();
          }
        }
        else if (selectedIndex == i) {
          noStroke();
          fill(style.selected);
          rect(x + 1, optiony + (height / 3 * 2) * i + 1, width - height - 1, height / 3 * 2);
        }
        style.text._text(elements[i].value, x + 5, optiony + (height / 3 * 2) * (i + 1));
      }
    }
    else
    {
      if (enabled) style.normal._set();
      else style.disabled._set();
      rect(x, y, width, height);
      line(x + width - height, y, x + width - height, y + height);
      line(x + width - height + height / 4, y + height / 4, x + width - height / 2, y + (height / 4) * 3);
      line(x + width - height / 4, y + height / 4, x + width - height / 2, y + (height / 4) * 3);
      if (enabled) style.text._set();
      else style.textDisabled._set();
      text(elements[selectedIndex].value, x + 5, y + height / 4 * 3);
    }
  }
  //do clicked in mousePressed
  public void clicked() {
    if (enabled) {
      if (open) {
      int optionHeight = elements.length * (height / 3 * 2) + 1;
      int optiony = y;
      if (optiony + optionHeight > sketchHeight) optiony -= optiony + optionHeight - sketchHeight + 1;
        if (mouseX < x || mouseX > x + width - height || mouseY < optiony || mouseY > optiony + optionHeight) open = false;
      }
      else if (!open && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height && elements.length >= 1) open = true;
    }
  }
}

class TextInputStyle {
  TextStyle text;
  TextStyle textExample;
  TextStyle textDisabled;
  Style normal;
  Style selected;
  Style disabled;
  TextInputStyle() {
    text = new TextStyle(0);
    textExample = new TextStyle(127);
    textDisabled = new TextStyle(192);
    normal = new Style(255, 127, 1);
    selected = new Style(255, 0xff60afff, 3);
    disabled = new Style(255, 192, 1);
  }
  TextInputStyle(TextStyle S_text, TextStyle S_textExample, TextStyle S_textDisabled, Style S_normal, Style S_selected, Style S_disabled) {
    text = S_text;
    textExample = S_textExample;
    textDisabled = S_textDisabled;
    normal = S_normal;
    selected = S_selected;
    disabled = S_disabled;
  }
}

class TextInput {
  TextInputStyle style;
  int x;
  int y;
  int width;
  int height;
  int cursorPos;
  boolean password;
  boolean numeric;
  boolean selected;
  boolean enabled;
  String text;
  String example;
  int lastMillis;
  int barTimer;
  Runnable onEnter;
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = "";
    onEnter = null;
    style = new TextInputStyle();
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, TextInputStyle S_style) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = "";
    onEnter = null;
    style = S_style;
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, String S_example) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = S_example;
    onEnter = null;
    style = new TextInputStyle();
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, String S_example, TextInputStyle S_style) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = S_example;
    onEnter = null;
    style = S_style;
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, Runnable S_onEnter) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = "";
    onEnter = S_onEnter;
    style = new TextInputStyle();
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, TextInputStyle S_style, Runnable S_onEnter) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = "";
    onEnter = S_onEnter;
    style = S_style;
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, String S_example, Runnable S_onEnter) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = S_example;
    onEnter = S_onEnter;
    style = new TextInputStyle();
    barTimer = 0;
    lastMillis = millis();
  }
  TextInput(int S_x, int S_y, int S_width, int S_height, boolean S_password, boolean S_numeric, String S_example, TextInputStyle S_style, Runnable S_onEnter) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    password = S_password;
    numeric = S_numeric;
    selected = false;
    enabled = true;
    text = "";
    example = S_example;
    onEnter = S_onEnter;
    style = S_style;
    barTimer = 0;
    lastMillis = millis();
  }
  public void display() {
    if (enabled && selected) style.selected._set();
    else if (enabled) style.normal._set();
    else style.disabled._set();
    rect(x, y, width, height);
    if (enabled && text.equals("") && !example.equals("")) style.textExample._set();
    else if (enabled) style.text._set();
    else style.textDisabled._set();
    if (text.equals("") && !example.equals("")) text(example, x + 3, y + height - 3);
    else text(text, x + 3, y + height - 3);
    String left = "";
    for (int i = 0; i < cursorPos; i++) {
      left += text.charAt(i);
    }
    strokeWeight(1);
    barTimer += millis() - lastMillis;
    lastMillis = millis();
    if (barTimer >= 1000) {
      barTimer -= 1000;
    }
    if (barTimer < 500 && selected) stroke(0, 255);
    else noStroke();
    line(x + 3 + textWidth(left), y + 2, x + 3 + textWidth(left), y + height - 2);
  }
  public boolean mouseOver() {
    return mouseX >= x && mouseX <= x + width - 1 && mouseY >= y && mouseY <= y + height - 1;
  }
  //do select in mousePressed
  public void select() {
    selected = mouseOver() && enabled;
  }
  //do render in keyPressed
  public void render() {
    if (enabled && selected) {
      style.text._set();
      char pressed = key;
      int code = keyCode;
      if (pressed == ENTER || pressed == RETURN) {
        if (onEnter != null) onEnter.run();
      }
      else if (pressed == BACKSPACE) {
        String pre = "";
        String post = "";
        boolean isPost = false;
        for (int i = 0; i < text.length(); i++) {
          if (i == cursorPos) isPost = true;
          if (isPost) post += text.charAt(i);
          else if (i < cursorPos - 1) pre += text.charAt(i);
        }
        text = pre + post;
        if (cursorPos > 0) cursorPos --;
      }
      else if (pressed == '\f' || pressed == TAB);
      else if (pressed == CODED) {
        if (code == LEFT) {
          if (cursorPos > 0) cursorPos --;
        }
        else if (code == RIGHT) {
          if (cursorPos < text.length()) cursorPos ++;
        }
      }
      else if (textWidth(text + pressed) <= width - 6 && !(numeric && "0123456789".indexOf(pressed) < 0)) {
        String pre = "";
        String post = "";
        boolean isPost = false;
        for (int i = 0; i < text.length(); i++) {
          if (i == cursorPos) isPost = true;
          if (isPost) post += text.charAt(i);
          else pre += text.charAt(i);
        }
        text = pre + pressed + post;
        cursorPos ++;
      }
      barTimer = 0;
      lastMillis = millis();
    }
  }
}

class ButtonStyle {
  TextStyle text;
  TextStyle textDisabled;
  Style normal;
  Style hover;
  Style pressed;
  Style disabled;
  ButtonStyle() {
    text = new TextStyle(0, 12, CENTER);
    textDisabled = new TextStyle(200, 12, CENTER);
    normal = new Style(255, 127, 1);
    hover = new Style(0xff91c8ff, 0xff60afff, 3);
    pressed = new Style(0xff3c3fe8, 0xff656691, 1);
    disabled = new Style(255, 200, 1);
  }
  ButtonStyle(TextStyle S_text, TextStyle S_textDisabled, Style S_normal, Style S_hover, Style S_pressed, Style S_disabled) {
    text = S_text;
    textDisabled = S_textDisabled;
    normal = S_normal;
    hover = S_hover;
    pressed = S_pressed;
    disabled = S_disabled;
    //make proper alignment stuffs (_textInRect()?)
    text.align = CENTER;
    textDisabled.align = CENTER;
  }
}

class Button {
  int x;
  int y;
  int width;
  int height;
  String text;
  boolean enabled;
  boolean wasPressed;
  boolean dEdge;
  ButtonStyle style;
  AdvRunnable onPress;
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, ButtonStyle S_style) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    text = S_text;
    enabled = true;
    style = S_style;
    onPress = null;
    wasPressed = false;
    dEdge = S_dEdge;
  }
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge) {
    x = S_x;
    y = S_y;
    if (S_width > ceil(textWidth(S_text))) width = S_width;
    else width = ceil(textWidth(S_text));
    height = S_height;
    text = S_text;
    enabled = true;
    style = new ButtonStyle();
    onPress = null;
    wasPressed = false;
    dEdge = S_dEdge;
  }
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, ButtonStyle S_style, final Runnable S_onPress) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    text = S_text;
    enabled = true;
    style = S_style;
    onPress = advRunnable(S_onPress);
    wasPressed = false;
    dEdge = S_dEdge;
  }
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, Runnable S_onPress) {
    x = S_x;
    y = S_y;
    if (S_width > ceil(textWidth(S_text))) width = S_width;
    else width = ceil(textWidth(S_text));
    height = S_height;
    text = S_text;
    enabled = true;
    style = new ButtonStyle();
    onPress = advRunnable(S_onPress);
    wasPressed = false;
    dEdge = S_dEdge;
  }
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, ButtonStyle S_style, AdvRunnable S_onPress) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    text = S_text;
    enabled = true;
    style = S_style;
    onPress = S_onPress;
    wasPressed = false;
    dEdge = S_dEdge;
  }
  Button(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, AdvRunnable S_onPress) {
    x = S_x;
    y = S_y;
    if (S_width > ceil(textWidth(S_text))) width = S_width;
    else width = ceil(textWidth(S_text));
    height = S_height;
    text = S_text;
    enabled = true;
    style = new ButtonStyle();
    onPress = S_onPress;
    wasPressed = false;
    dEdge = S_dEdge;
  }
  public void render() {
    if (enabled) {
      if (pressed()) {
        style.pressed._rect(x, y, width, height);
        if (onPress != null && !wasPressed) onPress.run(this);
      }
      else if (mouseOver()) {
        style.hover._rect(x, y, width, height);
      }
      else
      {
        style.normal._rect(x, y, width, height);
      }
      if (!pressed() && wasPressed && dEdge && onPress != null) onPress.run(this);
      style.text._text(text, x + width / 2, y + height - 3);
      wasPressed = pressed();
    }
    else
    {
      style.disabled._rect(x, y, width, height);
      style.textDisabled._text(text, x + width / 2,  y + height - 3);
    }
  }
  public void render(PApplet app, int mouseX, int mouseY, boolean mousePressed) {
    if (enabled) {
      if (pressed(mouseX, mouseY, mousePressed)) {
        style.pressed._rect(x, y, width, height, app.g);
        if (onPress != null && !wasPressed) onPress.run(this);
      }
      else if (mouseOver(mouseX, mouseY)) {
        style.hover._rect(x, y, width, height, app.g);
      }
      else
      {
        style.normal._rect(x, y, width, height, app.g);
      }
      if (!pressed(mouseX, mouseY, mousePressed) && wasPressed && dEdge && onPress != null) onPress.run(this);
      style.text._text(text, x + width / 2, y + height - 3, app.g);
      wasPressed = pressed(mouseX, mouseY, mousePressed);
    }
    else
    {
      style.disabled._rect(x, y, width, height, app.g);
      style.textDisabled._text(text, x + width / 2,  y + height - 3, app.g);
    }
  }
  public void setEnabled(boolean S_enabled) {
    enabled = S_enabled;
  }
  public boolean isEnabled() {
    return enabled;
  }
  public boolean pressed() {
    return mousePressed && enabled && mouseOver();
  }
  public boolean mouseOver() {
    return mouseX >= x && mouseX <= x + width - 1 && mouseY >= y && mouseY <= y + height - 1;
  }
  public boolean pressed(int mouseX, int mouseY, boolean mousePressed) {
    return mousePressed && enabled && mouseOver(mouseX, mouseY);
  }
  public boolean mouseOver(int mouseX, int mouseY) {
    return mouseX >= x && mouseX <= x + width - 1 && mouseY >= y && mouseY <= y + height - 1;
  }
}

class TextureButtonStyle {
  PImage normal;
  PImage hover;
  PImage pressed;
  PImage disabled;
  TextureButtonStyle(PImage S_normal, PImage S_hover, PImage S_pressed, PImage S_disabled) {
    normal = S_normal;
    hover = S_hover;
    pressed = S_pressed;
    disabled = S_disabled;
  }
}

class TextureButton {
  int x;
  int y;
  int width;
  int height;
  String text;
  boolean enabled;
  boolean wasPressed;
  boolean dEdge;
  TextureButtonStyle style;
  AdvRunnable onPress;
  TextureButton(int S_x, int S_y, int S_width, int S_height, String S_text, boolean S_dEdge, TextureButtonStyle S_style, AdvRunnable S_onPress) {
    x = S_x;
    y = S_y;
    width = S_width;
    height = S_height;
    text = S_text;
    enabled = true;
    style = S_style;
    onPress = S_onPress;
    wasPressed = false;
    dEdge = S_dEdge;
  }
  public void render() {
    if (enabled) {
      if (pressed()) {
        image(style.pressed, x, y, width, height);
        if (onPress != null && !wasPressed) onPress.run(this);
      }
      else if (mouseOver()) {
        image(style.hover, x, y, width, height);
      }
      else
      {
        image(style.normal, x, y, width, height);
      }
      if (!pressed() && wasPressed && dEdge && onPress != null) onPress.run(this);
      wasPressed = pressed();
    }
    else
    {
      image(style.disabled, x, y, width, height);
    }
  }
  public void setEnabled(boolean S_enabled) {
    enabled = S_enabled;
  }
  public boolean isEnabled() {
    return enabled;
  }
  public boolean pressed() {
    return mousePressed && enabled && mouseOver();
  }
  public boolean mouseOver() {
    return mouseX >= x && mouseX <= x + width - 1 && mouseY >= y && mouseY <= y + height - 1;
  }
}

class ScrollBar {
  int x;
  int y;
  int length;
  int max;
  int value;
  boolean vertical;
  ScrollBar(boolean S_vertical, int S_x, int S_y, int S_length, int S_max) {
    x = S_x;
    y = S_y;
    length = S_length;
    vertical = S_vertical;
    max = S_max;
  }
  public void display() {
    if (max > 0) {
      int barLength = length / max;
      if (barLength < 10) barLength = 10;
      float pixelPerValue = (length - barLength) / max;
      float pixelOffset = pixelPerValue * value;
      if (vertical) {
        noStroke();
        fill(127);
        ellipse(x + 5, y + 5 + pixelOffset, 10, 10);
        ellipse(x + 5, y + 5 + pixelOffset + barLength - 10, 10, 10);
        rect(x, y + 5 + pixelOffset, 10, barLength - 10);
      }
      else
      {
        
      }
    }
  }
  public void scroll(int scroll) {
    value += scroll;
    if (value < 0) value = 0;
    if (value > max) value = max;
  }
}


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


//2.1
public void Z21_GET_SERIAL_NUM() {
  send(new byte[]{0x10, 0x00}, new byte[0]);
}

//2.1
public boolean PROCESS_GET_SERIAL_NUM(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x08 && len[1] == 0x00 && head[0] == 0x10 && head[1] == 0x00 && data.length == 4) {
    long serial = ByteBuffer.wrap(new byte[]{data[0], data[1], data[2], data[3], 0x00, 0x00, 0x00, 0x00}).order(ByteOrder.LITTLE_ENDIAN).getLong();
    if (log) println("serial number is: " + serial);
    SerialNum.text = "Serial adress: " + serial;
    return true;
  }
  else return false;
}

//2.2
public void Z21_LOGOUT() {
  if (CONNECT_STATUS.equals(STAT_CONNECTED)) {
    send(new byte[]{0x30, 0x00}, new byte[0]);
    if (log) println("successfully logged out.");
  }
  else if (log) println("didn't have to log out!");
}

//2.3
public void Z21_GET_XBUS_VER() {
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, 0x21, 0x00});
}

//2.3
public boolean PROCESS_GET_XBUS_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 5) {
    if (data[0] == 0x63 && data[1] == 0x21 && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
      int verfull = (data[2] & 0xf0) >> 4;
      float versign = PApplet.parseInt(data[2] & 0x0f);
      while (versign >= 1) versign /= 10;
      float version = PApplet.parseFloat(verfull) + versign;
      if (log) println("X-Bus version is: V" + version);
      XBusVersion.text = "X-Bus version: V" + version;
      return true;
    }
    else return false;
  }
  else return false;
}

//2.4
public void Z21_GET_STATUS() {
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, 0x24, 0x05});
}

//2.5 & 2.6
public void Z21_SET_TRACK_POWER(boolean power) {
  byte add = 0x00;
  if (power) add = 0x01;
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, PApplet.parseByte(0x80 | add), PApplet.parseByte(0xa0 | add)});
}

//2.7 > 2.10
public boolean PROCESS_STATUS_BROADCAST(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 3 && data[0] == PApplet.parseByte(0x61) && data[2] == (data[0] ^ data[1])) {
    TRACK_STATE = PApplet.parseByte(TRACK_STATE | data[1]);
    String[] states = new String[16];
    states[0x01] = "track power off.";
    states[0x02] = "track power on.";
    states[0x04] = "programming mode on.";
    states[0x08] = "SHORT CUIRCET!";
    states[0x09] = "SHORT CUIRCET!";
    if (log) println("track state broadcast: " + states[data[1]]);
    return true;
  }
  else return false;
}

//2.11
public boolean PROCESS_UNKNOWN_COMMAND(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 3 && data[0] == 0x61 && data[1] == 0x82 && data[2] == 0xe3) {
    System.err.println("WARNING: Z21 reports unknown command!");
    return true;
  }
  else return false;
}

//2.12
public boolean PROCESS_STATUS_CHANGE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x08 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 4 && data[0] == 0x62 && data[1] == 0x22 && data[3] == (data[0] ^ data[1] ^ data[2])) {
    String text = "";
    if (data[2] == 0x00) text = "normal operation.";
    if ((data[2] & 0x01) > 0) text = "emergency stop ";
    if ((data[2] & 0x02) > 0) text = "track power off ";
    if ((data[2] & 0x04) > 0) text = "short cuircet ";
    if ((data[2] & 0x20) > 0) text = "in programming mode";
    if (log) println("sys. status (track only): " + text);
    TRACK_STATE = data[2];
    return true;
  }
  else return false;
}

//2.13
public void Z21_STOP_ALL() {
  send(new byte[]{0x40, 0x00}, new byte[]{PApplet.parseByte(0x80), PApplet.parseByte(0x80)});
}

//2.15
public void Z21_GET_FIRMWARE_VER() {
  send(new byte[]{0x40, 0x00}, new byte[]{PApplet.parseByte(0xf1), 0x0a, PApplet.parseByte(0xfb)});
}

//2.15
public boolean PROCESS_GET_FIRMWARE_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 5 && data[0] == PApplet.parseByte(0xf3) && data[1] == 0x0a && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
    Z21_FIRMWARE_VER = PApplet.parseFloat(hex(data[2]) + "." + hex(data[3]));
    if (log) println("firmware version is V" + Z21_FIRMWARE_VER);
    Firmware.text = "Firmware: V" + Z21_FIRMWARE_VER;
    return true;
  }
  else return false;
}

//2.16
public void Z21_SET_BROADCAST_FLAGS(int flags) {
  send(new byte[]{0x50, 0x00}, new byte[]{PApplet.parseByte((flags & 0xff000000) >> 24), PApplet.parseByte((flags & 0x00ff0000) >> 16), PApplet.parseByte((flags & 0x0000ff00) >> 8), PApplet.parseByte(flags & 0x000000ff)});
}

//2.17
public void Z21_GET_BROADCAST_FLAGS() {
  send(new byte[]{0x51, 0x00}, new byte[]{});
}

//2.17
public boolean PROCESS_GET_BROADCAST_FLAGS(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x08 && len[1] == 0x00 && head[0] == 0x51 && head[1] == 0x00 && data.length == 4) {
    if (log) println("broadcast flags are: 0x" + hex(data[0]) + hex(data[1]) + hex(data[2]) + hex(data[3]));
    return true;
  }
  else return false;
}

//2.18
public boolean PROCESS_STATUS_DATA_CHANGE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x14 && len[1] == 0x00 && head[0] == PApplet.parseByte(0x84) && head[1] == 0x00 && data.length == 16) {
    String track = "";
    if (data[12] == 0x00) track = "normal operation.";
    if ((data[12] & 0x01) > 0) track = "emergency stop ";
    if ((data[12] & 0x02) > 0) track = "track power off ";
    if ((data[12] & 0x04) > 0) track = "short cuircet ";
    if ((data[12] & 0x20) > 0) track = "in programming mode";
    String system = "";
    if ((data[13] & 0x01) > 0) {
      system = "OVERTEMPERATURE";
      Warning.text = "WARNING: YOUR Z21 IS OVERHEAT!\nUNPLUG YOUR Z21 IMMEDIATELY TO AVOID DAMAGE!\nyou may plug it in after it has had time to cool down.";
      warn = true;
      Z21_SET_TRACK_POWER(false);
    }
    if ((data[13] & 14) > 0) {
      if (system.equals("")) system = "POWER SHORTAGE!";
      else system += " AND POWER SHORTAGE!";
      Z21_SET_TRACK_POWER(false);
      Operation.text = "Operation: POWER SHORTAGE!";
      Operation.style.col = 0xffff0000;
    }
    else if (system.equals("")) system += "OK";
    else system += "!";
    short main = ByteBuffer.wrap(new byte[]{data[0], data[1]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
    short prog = ByteBuffer.wrap(new byte[]{data[2], data[2]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
    short mainFiltered = ByteBuffer.wrap(new byte[]{data[4], data[5]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
    short temp = ByteBuffer.wrap(new byte[]{data[6], data[7]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
    int supply = ByteBuffer.wrap(new byte[]{data[8], data[9], 0x00, 0x00}).order(ByteOrder.LITTLE_ENDIAN).getInt();
    int internal = ByteBuffer.wrap(new byte[]{data[10], data[11], 0x00, 0x00}).order(ByteOrder.LITTLE_ENDIAN).getInt();
    TRACK_STATE = data[12];
    if (log) println("sys. status (full):\n"
                   + "current on main: " + main + "mA\n"
                   + "current on programming: " + prog + "mA\n"
                   + "filtered current on main: " + mainFiltered + "mA\n"
                   + "internal temperature: " + temp + "\u00b0C\n"
                   + "supply voltage: " + supply + "mV\n"
                   + "internal voltage: " + internal + "mV\n"
                   + "track status: " + track + "\n"
                   + "system status: " + system);
    Power.text = "Power: " + supply + "mV";
    Main.text = "Main: " + main + "mA";
    Prog.text = "Prog: " + prog + "mA";
    Temperature.text = "Temp: " + temp + "\u00b0C";
    return true;
  }
  else return false;
}

//2.19
public void Z21_GET_STATUS_DATA() {
  send(new byte[]{PApplet.parseByte(0x85), 0x00}, new byte[]{});
}

//2.20
public void Z21_GET_HARDWARE_VER() {
  send(new byte[]{0x1a, 0x00}, new byte[]{});
}

//2.20
public boolean PROCESS_GET_HARDWARE_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x0c && len[1] == 0x00 && head[0] == 0x1a && head[1] == 0x00 && data.length == 8) {
    String[] versions = new String[5];
    versions[0] = "black Z21 (2012)";
    versions[1] = "black Z21 (2013)";
    versions[2] = "smartrail (2012)";
    versions[3] = "white z21 (2013)";
    versions[4] = "z21 start (2016)";
    if (log) println("hardware version is: " + versions[PApplet.parseInt(data[3])]);
    //unreliable, firmware changed and the format with it.
    return true;
  }
  else return false;
}

//2.21
public void Z21_GET_CODE() {
  send(new byte[]{0x18, 0x00}, new byte[]{});
}

//2.21
public boolean PROCESS_GET_CODE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x05 && len[1] == 0x00 && head[0] == 0x18 && head[1] == 0x00 && data.length == 1) {
    if (data[0] == 0x00) {
      start = false;
      locked = false;
      if (log) println("Z21");
    }
    if (data[0] == 0x01) {
      start = true;
      locked = true;
      if (log) println("z21 start, locked.");
    }
    if (data[0] == 0x02) {
      start = true;
      locked = false;
      if (log) println("z21 start, unlocked");
    }
    return true;
  }
  else return false;
}

//3.1
public void Z21_GET_LOC_MODE(int adress) {
  send(new byte[]{0x60, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff)});
}

//3.1
public boolean PROCESS_GET_LOC_MODE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x60 && head[1] == 0x00 && data.length == 3) {
    int adress = (data[0] << 8) | data[1];
    if (data[2] > 0) {
      if (log) println("locomotive " + adress + " is in MM mode.");
      return true;
    }
    else
    {
      if (log) println("locomotive " + adress + " is in DCC mode.");
      return false;
    }
  }
  else return false;
}

//3.2
public void Z21_SET_LOC_MODE(int adress, boolean mode) {
  if (mode) send(new byte[]{0x61, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff), 0x01});
  else send(new byte[]{0x61, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff), 0x00});
}

//3.3
public void Z21_GET_TURNOUT_MODE(int adress) {
  send(new byte[]{0x60, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff)});
}

//3.3
public boolean PROCESS_GET_TURNOUT_MODE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x60 && head[1] == 0x00 && data.length == 3) {
    int adress = (data[0] << 8) | data[1];
    if (data[2] > 0) {
      if (log) println("locomotive " + adress + " is in MM mode.");
      return true;
    }
    else
    {
      if (log) println("locomotive " + adress + " is in DCC mode.");
      return false;
    }
  }
  else return false;
}

//3.4
public void Z21_SET_TURNOUT_MODE(int adress, boolean mode) {
  if (mode) send(new byte[]{0x61, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff), 0x01});
  else send(new byte[]{0x61, 0x00}, new byte[]{PApplet.parseByte((adress & 0x0000ff00) >> 8), PApplet.parseByte(adress & 0x000000ff), 0x00});
}

//4.1
public void Z21_GET_LOC_INFO(int adress) {
  byte[] message = new byte[]{PApplet.parseByte(0xe3), PApplet.parseByte(0xf0), PApplet.parseByte(((adress & 0x00003f00) >> 8) | 0xc0), PApplet.parseByte(adress & 0x000000ff), 0x00};
  message[4] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3]);
  send(new byte[]{0x40, 0x00}, message);
}

//4.1
public void Z21_GET_LOC_INFO(int adress, final AdvRunnable done) {
  byte[] message = new byte[]{PApplet.parseByte(0xe3), PApplet.parseByte(0xf0), PApplet.parseByte(((adress >> 8) & 0x000000ff) | 0xc0), PApplet.parseByte(adress & 0x000000ff), 0x00};
  message[4] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3]);
  send(new byte[]{0x40, 0x00}, message);
  addDataRequest(new DataRequest("Z21_GET_LOC_INFO", "GET_LOC_INFO", adress) {
    public Object complete(Object lst) {
      done.run(lst);
      return null;
    }
  });
}

//4.2
public void Z21_SET_LOC_DRIVE(int adress, boolean direction, int speed) {
  byte[] message = new byte[]{PApplet.parseByte(0xe4), 0x13, PApplet.parseByte(((adress & 0x00003f00) >> 8) | 0xc0), PApplet.parseByte(adress & 0x000000ff), PApplet.parseByte(PApplet.parseByte(speed & 0x0000007f) | PApplet.parseByte(direction)), 0x00};
  message[5] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
  send(new byte[]{0x40, 0x00}, message);
}

//4.3
public void Z21_SET_LOC_FUNCTION(int adress, int function, int mode) {
  byte[] message = new byte[]{PApplet.parseByte(0xe4), PApplet.parseByte(0xf8), PApplet.parseByte(((adress & 0x00003f00) >> 8) | 0xc0), PApplet.parseByte(adress & 0x000000ff), PApplet.parseByte((PApplet.parseByte(mode & 0x00000003) << 6) | PApplet.parseByte(function & 0x000000cf)), 0x00};
  message[5] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
  send(new byte[]{0x40, 0x00}, message);
  if (log) println("set function " + function + " to " + mode + " for adress " + adress);
}

//4.4
public boolean PROCESS_LOC_INFO(byte[] len, byte[] head, byte[] data) {
  if (len[0] >= 0x0e && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == len[0] - 4 && data[0] == PApplet.parseByte(0xef)) {
    final int adress = ((PApplet.parseInt(data[1]) & 0x3F) << 8) + PApplet.parseInt(data[2]);
    boolean[] function = new boolean[29];
    boolean[] DB4 = bit(data[5]);
    boolean[] DB5 = bit(data[6]);
    boolean[] DB6 = bit(data[7]);
    boolean[] DB7 = bit(data[7]);
    final boolean occupied = PApplet.parseBoolean(data[3] & 0x08);
    final byte speedSteps = PApplet.parseByte(data[3] & 0x07);
    final boolean direction = PApplet.parseBoolean(data[4] & 0x80);
    final byte speed = PApplet.parseByte(data[4] & 0x7f);
    final boolean doubleHead = DB4[1];
    final boolean smartSearch = DB4[2];
    function[0] = DB4[3];
    function[1] = DB4[7];
    function[2] = DB4[6];
    function[3] = DB4[5];
    function[4] = DB4[4];
    for (int i = 0; i < 8; i++) {
      function[5 + i] = DB5[i];
      function[13 + i] = DB6[i];
      function[21 + i] = DB7[i];
    }
    final boolean[] functions = function;
    parseDataRequests(new AdvRunnable() {
      public Object run(Object req) {
        DataRequest request = (DataRequest) req;
        if (request.requestType.equals("GET_LOC_INFO") && (int) request.requestData == adress) {
          request.complete(new LocStatus(adress, occupied, speedSteps, direction, speed, doubleHead, smartSearch, functions));
          return true;
        }
        else return false;
      }
    });
    return true;
  }
  else return false;
}

/*
void Z21_TEMP() {
  send(new byte[]{0x??, 0x??}, new byte[]{???});
}

boolean PROCESS_TEMP(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x?? && len[1] == 0x?? && head[0] == 0x?? && head[1] == 0x?? && data.length == ?) {
    //do stuff
    return true;
  }
  else
  {
    println("packet is not ?!\nignoring.");//TesTer only
    return false;
  }
  //uncomment with removing TesTer:
  //else return false;
}
*/


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
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

final TextStyle STYLE_TEXT_WARN = new TextStyle(0xffff0000, 24, CENTER);

final ButtonStyle STYLE_BUTTON_STOP = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(0xffff0000, 127, 1), new Style(0xffff5555, 0xff60afff, 3), new Style(0xffcc0000, 0xff656691, 1), new Style(255, 200, 1));
final ButtonStyle STYLE_BUTTON_GO = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(0xff00ff00, 127, 1), new Style(0xff55ff55, 0xff60afff, 3), new Style(0xff00cc00, 0xff656691, 1), new Style(255, 200, 1));
final ButtonStyle STYLE_BUTTON_LOCKED = new ButtonStyle(new TextStyle(0), new TextStyle(0), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1));


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


class PREFEntry {
  String name;
  String[] elements;
  PREFEntry(String S_name, String[] S_elements) {
    name = S_name;
    elements = S_elements;
  }
}

class PREF {
  PREFEntry[] lines;
  PREF(PREFEntry[] S_lines) {
    lines = S_lines;
  }
  PREF() {
    lines = new PREFEntry[0];
  }
  public PREFEntry get(String name) {
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].name.equals(name)) return lines[i];
    }
    return null;
  }
  public void set(PREFEntry entry) {
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].name.equals(entry.name)) {
        lines[i] = entry;
        return;
      }
    }
    lines = (PREFEntry[]) expand(lines, lines.length + 1);
    lines[lines.length - 1] = entry;
  }
}

class FunctionDefinition {
  public String name;
  public boolean used;
  public boolean flip;
  FunctionDefinition() {
    name = "unused";
    used = false;
  }
  FunctionDefinition(String S_name, boolean S_flip) {
    name = S_name;
    used = true;
    flip = S_flip;
  }
  public Button generateButton(int x, int y, int width, int height, int num, Runnable onPress) {
    String text = "F" + num;
    if (!name.equals("")) text += ": " + name;
    Button button = new Button(x, y, width, height, text, !flip, onPress);
    button.enabled = used;
    return button;
  }
}

class LocEntry {//add controll functions and speed steps
  int adress;
  String name;
  String owner;
  FunctionDefinition[] functions;
  LocEntry(PREF file) {
    if (file == null) {
      adress = 3;
      name = "error while loading.";
      owner = "";
      functions = new FunctionDefinition[29];
      for (int i = 0; i < functions.length; i++) functions[i] = new FunctionDefinition();
      println("missing locomotive file!");
    }
    else
    {
      adress = 0;
      PREFEntry ARAW = file.get("adress");
      if (ARAW != null && ARAW.elements.length == 1) adress = PApplet.parseInt(ARAW.elements[0]);
      if (adress == 0) adress = 3;
      name = "";
      PREFEntry NRAW = file.get("name");
      if (NRAW != null && NRAW.elements.length == 1 && NRAW.elements[0].length() >= 3) name = NRAW.elements[0].substring(1, NRAW.elements[0].length() - 1);
      owner = "";
      PREFEntry ORAW = file.get("owner");
      if (ORAW != null && ORAW.elements.length == 1 && ORAW.elements[0].length() >= 3) owner = ORAW.elements[0].substring(1, ORAW.elements[0].length() - 1);
      functions = new FunctionDefinition[29];
      for (int i = 0; i < 29; i++) {
        functions[i] = new FunctionDefinition("", false);
        PREFEntry FRAW = file.get("F" + i);
        if (FRAW != null && FRAW.elements.length == 3) {
          functions[i] = new FunctionDefinition(FRAW.elements[2].substring(1, FRAW.elements[2].length() - 1), PApplet.parseBoolean(FRAW.elements[1]));
          functions[i].used = PApplet.parseBoolean(FRAW.elements[0]);
        }
      }
    }
  }
  LocEntry(int S_adress, String S_name, String S_owner, FunctionDefinition[] S_functions) {
    adress = S_adress;
    name = S_name;
    owner = S_owner;
    functions = S_functions;
    if (functions == null) {
      functions = new FunctionDefinition[29];
      for (int i = 0; i < 29; i++) {
        functions[i] = new FunctionDefinition("", false);
      }
      functions[0] = new FunctionDefinition("lights", true);
    }
  }
  public PREF createFile() {
    PREF file = new PREF();
    file.set(new PREFEntry("adress", new String[]{adress + ""}));
    file.set(new PREFEntry("name", new String[]{"\"" + name + "\""}));
    file.set(new PREFEntry("owner", new String[]{"\"" + owner + "\""}));
    if (functions.length == 29) for (int i = 0; i < 29; i++) {
      file.set(new PREFEntry("F" + i, new String[]{functions[i].used + "", functions[i].flip + "", "\"" + functions[i].name + "\""}));
    }
    else for (int i = 0; i < 29; i++) {
      file.set(new PREFEntry("F" + i, new String[]{"true", "false", "\"\""}));
    }
    return file;
  }
  public PREFEntry createIndexEntry(int id) {
    return new PREFEntry("LOC_" + id, new String[]{"" + adress, "\"" + owner + "\"", "\"" + name + "\""});
  }
}


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


interface FileReader {
  public Object load(String path);
  public Object load(File file);
  public void save(String path, Object data);
  public void save(File file, Object data);
  public String[] buffer(Object data);
}

class PREFReader implements FileReader {
  public PREF load(String path) {
    String[] file = loadStrings(path);
    if (file != null) {
      PREFEntry[] lines = new PREFEntry[file.length];
      for (int i = 0; i < file.length; i++) {
        char level = ' ';
        String decoded = "";
        for (int p = 0; p < file[i].length(); p++) {
          char prevLevel = level;
          if (level == ' ' && file[i].charAt(p) == '\'') level = '\'';
          if (level == ' ' && file[i].charAt(p) == '\"') level = '\"';
          if (level == '\'' && file[i].charAt(p) == '\'') level = ' ';
          if (level == '\"' && file[i].charAt(p) == '\"') level = ' ';
          if (level != ' ' && file[i].charAt(p) == ':') decoded += "\n";
          else decoded += file[i].charAt(p);
        }
        String[] split = split(decoded, ':');
        for (int p = 0; p < split.length; p++) {
          split[p] = split[p].replace('\n', ':');
        }
        String name = split[0];
        String[] elements = new String[split.length - 1];
        for (int p = 0; p < split.length - 1; p++) {
          elements[p] = split[p + 1];
        }
        lines[i] = new PREFEntry(name, elements);
      }
      return new PREF(lines);
    }
    return null;
  }
  public PREF load(File file) {
    return load(file.getAbsolutePath());
  }
  public void save(String path, Object data) {
    saveStrings(path, buffer(data));
  }
  public void save(File file, Object data) {
    save(file.getAbsolutePath(), data);
  }
  public String[] buffer(Object data) {
    PREF file = (PREF) data;
    if (data == null) {
      return new String[0];
    }
    else
    {
      String[] buffer = new String[file.lines.length];
      for (int i = 0; i < file.lines.length; i++) {
        buffer[i] = file.lines[i].name;
        for (int p = 0; p < file.lines[i].elements.length; p++) {
          buffer[i] += ":" + file.lines[i].elements[p];
        }
      }
      return buffer;
    }
  }
}


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


interface MenuScreen {
  public void init();
  public void render();
  public void mousePress();
  public void keyPress();
  public boolean exited();
}

class AddLocomotive implements MenuScreen {
  Button cancel;
  Button add;
  Button functionDemo;
  TextInput adress;
  TextInput name;
  TextInput owner;
  TextInput functionName;
  Checkbox functionUsed;
  Checkbox functionToggle;
  Text Adress;
  Text Name;
  Text Owner;
  Text Functions;
  Text FunctionDemo;
  Text FunctionToggle;
  boolean exited;
  Style style;
  FunctionDefinition[] functions;
  Dropdown func;
  public void init() {
    cancel = new Button(0, 0, 49, 20, "CANCEL", false, new Runnable(){public void run(){exited=true;}});
    add = new Button(0, 0, 49, 20, "ADD", false, new Runnable(){public void run(){addLoc();}});
    functionDemo = new Button(0, 0, 74, 20, "", false, new Runnable(){public void run(){tryFunction();}});
    adress = new TextInput(0, 0, 49, 20, false, true, "adress");
    name = new TextInput(0, 0, 139, 20, false, false, "name");
    owner = new TextInput(0, 0, 190, 20, false, false, "owner");
    Owner = new Text(0, 0, "owner\'s full name:");
    functionName = new TextInput(0, 0, 54, 20, false, false, "fn name");
    functionName.text = "unused";
    functionName.cursorPos = 6;
    functionUsed = new Checkbox(0, 0, 20, 20);
    functionToggle = new Checkbox(0, 0, 20, 20);
    Adress = new Text(0, 0, "Adress");
    Name = new Text(0, 0, "Name:");
    Functions = new Text(0, 0, "Functions:");
    FunctionDemo = new Text(0, 0, "Try it:");
    FunctionToggle = new Text(0, 0, "toggle:");
    exited = false;
    style = new Style(255, 127, 1);
    functions = new FunctionDefinition[29];
    DropdownElement[] funcSel = new DropdownElement[28];
    for (int i = 0; i < 29; i++) {
      functions[i] = new FunctionDefinition();
      if (i > 0) funcSel[i - 1] = new DropdownElement(i + "", "F" + i);
    }
    functions[0] = new FunctionDefinition("lights", true);
    func = new Dropdown(0, 0, 50, 20, funcSel, new SidedRunnable(){
      public void pre() {
        int functionIndex = func.selectedIndex + 1;
        functions[functionIndex].used = functionUsed.value;
        functions[functionIndex].name = functionName.text;
        functions[functionIndex].flip = functionToggle.value;
        Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), functionIndex, OFF);
      }
      public void post() {
        int functionIndex = func.selectedIndex + 1;
        functionUsed.value = functions[functionIndex].used;
        functionName.text = functions[functionIndex].name;
        functionName.cursorPos = functions[functionIndex].name.length();
        functionToggle.value = functions[functionIndex].flip;
      }
    });
  }
  public void render() {
    calcPosition();
    style._set();
    rect(width / 2 - 200, height / 2 - 125, 400, 250);
    cancel.enabled = !func.open;
    add.enabled = !func.open;
    adress.enabled = !func.open;
    name.enabled = !func.open;
    owner.enabled = !func.open;
    functionName.enabled = !func.open;
    functionUsed.enabled = !func.open;
    functionToggle.enabled = !func.open && functionUsed.value;
    functionDemo.enabled = !func.open && functionUsed.value;
    functionDemo.text = "F" + (func.selectedIndex + 1) + ": " + functionName.text;
    functionDemo.dEdge = !functionToggle.value;
    cancel.render();
    add.render();
    functionDemo.render();
    FunctionDemo.display();
    adress.display();
    Adress.display();
    name.display();
    Name.display();
    owner.display();
    Owner.display();
    Functions.display();
    functionUsed.display();
    functionToggle.display();
    FunctionToggle.display();
    functionName.display();
    func.render();
  }
  public void mousePress() {
    adress.select();
    name.select();
    owner.select();
    functionName.select();
    func.clicked();
    functionUsed.render();
    functionToggle.render();
  }
  public void keyPress() {
    adress.render();
    name.render();
    owner.render();
    functionName.render();
  }
  public void tryFunction() {
    if (functionToggle.value) Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, FLIP);
    else if (functionDemo.pressed()) Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, ON);
    else Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, OFF);
  }
  public void calcPosition() {
    cancel.x = width / 2 - 195;
    cancel.y = height / 2 + 100;
    add.x = width / 2 + 145;
    add.y = height / 2 + 100;
    adress.x = width / 2 - 195;
    adress.y = height / 2 - 100;
    Adress.x = width / 2 - 195;
    Adress.y = height / 2 - 105;
    func.x = width / 2 - 170;
    func.y = height / 2 - 60;
    functionUsed.x = width / 2 - 195;
    functionUsed.y = height / 2 - 60;
    functionName.x = width / 2 - 115;
    functionName.y = height / 2 - 60;
    functionDemo.x = width / 2 - 55;
    functionDemo.y = height / 2 - 60;
    FunctionDemo.x = width / 2 - 55;
    FunctionDemo.y = height / 2 - 65;
    functionToggle.x = width / 2 + 24;
    functionToggle.y = height / 2 - 60;
    FunctionToggle.x = width / 2 + 24;
    FunctionToggle.y = height / 2 - 65;
    Functions.x = width / 2 - 195;
    Functions.y = height / 2 - 65;
    name.x = width / 2 - 140;
    name.y = height / 2 - 100;
    Name.x = width / 2 - 140;
    Name.y = height / 2 - 105;
    owner.x = width / 2 + 4;
    owner.y = height / 2 - 100;
    Owner.x = width / 2 + 4;
    Owner.y = height / 2 - 105;
  }
  public void addLoc() {
    int locAdress = PApplet.parseInt(adress.text);
    String locName = name.text;
    String locOwner = owner.text;
    int functionIndex = func.selectedIndex + 1;
    functions[functionIndex].used = functionUsed.value;
    functions[functionIndex].name = functionName.text;
    functions[functionIndex].flip = functionToggle.value;
    Z21_SET_LOC_FUNCTION(locAdress, functionIndex, OFF);
    int locID = 0;
    PREFEntry num = locIndex.get("num");
    if (num != null && num.elements.length == 1) locID = PApplet.parseInt(num.elements[0]);
    locIndex.set(new PREFEntry("num", new String[]{(locID + 1) + ""}));
    LocEntry loc = new LocEntry(locAdress, locName, locOwner, functions);
    PREF.save("data/locs/LOC_" + locID + ".pref", loc.createFile());
    locomotives = (LocEntry[]) expand(locomotives, locomotives.length + 1);
    locomotives[locomotives.length - 1] = loc;
    locIndex.set(new PREFEntry(locID + "", new String[]{"\"data/locs/LOC_" + locID + ".pref\""}));
    exited = true;
    decodeLocIndex();
    backup();
  }
  public boolean exited() {
    return exited;
  }
}

class EditLocomotive extends AddLocomotive implements MenuScreen {
  int index;
  String locFile;
  EditLocomotive(int S_index, String S_locFile) {
    index = S_index;
    locFile = S_locFile;
  }
  @Override
  public void init() {
    super.init();
    add = new Button(0, 0, 49, 20, "SAVE", false, new Runnable(){public void run(){saveLoc();}});
    functions = locomotives[index].functions;
    adress.text = locomotives[index].adress + "";
    name.text = locomotives[index].name;
    owner.text = locomotives[index].owner;
    adress.cursorPos = adress.text.length();
    name.cursorPos = name.text.length();
    owner.cursorPos = owner.text.length();
    func.onChange.post();
  }
  public void saveLoc() {
    int locAdress = PApplet.parseInt(adress.text);
    String locName = name.text;
    String locOwner = owner.text;
    int functionIndex = func.selectedIndex + 1;
    functions[functionIndex].used = functionUsed.value;
    functions[functionIndex].name = functionName.text;
    functions[functionIndex].flip = functionToggle.value;
    Z21_SET_LOC_FUNCTION(locAdress, functionIndex, OFF);
    println(locAdress);
    LocEntry loc = new LocEntry(locAdress, locName, locOwner, functions);
    PREF.save(locFile, loc.createFile());
    locomotives[index] = loc;
    exited = true;
    decodeLocIndex();
    backup();
    println(locAdress);
  }
}


/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (\u00a9) Julian Scheffers, all rights reserved.
 */


int sketchWidth;
int sketchHeight;

interface SidedRunnable {
  public void pre();
  public void post();
}

interface AdvRunnable {
  public Object run(Object in);
}

public AdvRunnable advRunnable(final Runnable S_simple) {
  return new AdvRunnable(){
    Runnable simple = S_simple;
    public Object run(Object o) {
      simple.run();
      return null;
    }
  };
}

class LocStatus {
  int adress;
  boolean occupied;
  byte speedSteps;
  boolean direction;
  byte speed;
  boolean doubleHead;
  boolean smartSearch;
  boolean[] function;
  LocStatus(int S_adress, boolean S_occupied, byte S_speedSteps, boolean S_direction, byte S_speed, boolean S_doubleHead, boolean S_smartSearch, boolean[] S_function) {
    adress = S_adress;
    occupied = S_occupied;
    speedSteps = S_speedSteps;
    direction = S_direction;
    speed = S_speed;
    doubleHead = S_doubleHead;
    smartSearch = S_smartSearch;
    function = S_function;
  }
}

public abstract class DataRequest {
  String requester;
  String requestType;
  Object requestData;
  DataRequest(String S_requester, String S_requestType, Object S_requestData) {
    requester = S_requester;
    requestType = S_requestType;
    requestData = S_requestData;
  }
  public abstract Object complete(Object data);
}

public void addDataRequest(DataRequest request) {
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] == null) {
    dataRequests[i] = request;
    return;
  }
  dataRequests = (DataRequest[]) expand(dataRequests, dataRequests.length + 1);
  dataRequests[dataRequests.length - 1] = request;
}

public void parseDataRequests(AdvRunnable parser) {
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] != null) {
    if ((boolean) parser.run(dataRequests[i])) {
      dataRequests[i] = null;
    }
  }
}

public void filterDataRequests() {
  DataRequest[] dr_new = new DataRequest[0];
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] != null) {
    dr_new = (DataRequest[]) expand(dr_new, dr_new.length + 1);
    dr_new[dr_new.length - 1] = dataRequests[i];
  }
  dataRequests = dr_new;
}

public boolean[] bit(byte in) {
  return new boolean[]{PApplet.parseBoolean(in & 0x01), PApplet.parseBoolean(in & 0x02), PApplet.parseBoolean(in & 0x04), PApplet.parseBoolean(in & 0x08), PApplet.parseBoolean(in & 0x10), PApplet.parseBoolean(in & 0x20), PApplet.parseBoolean(in & 0x40), PApplet.parseBoolean(in & 0x80)};
}

public class ControlScreen extends PApplet {
  Button[] functions;
  int speed;
  int speedSteps;
  boolean direction;
  
  public void settings() {
    size(250, 380);
  }
  
  public void setup() {
    setVisible(false);
    if (icon != null) surface.setIcon(icon);
  }
  
  public void draw() {
    background(255);
    if (functions != null) for (int i = 0; i < functions.length; i++) if (functions[i] != null) functions[i].render(this, mouseX, mouseY, mousePressed);
    doSpeed(160, 5, speed, g);
    if (!CONNECT_STATUS.equals(STAT_CONNECTED)) exit();
  }
  
  public void setVisible(boolean b) {
    surface.setVisible(b);
    if (b) {
      loop();
      makeGUI();
      speed = 0;
      surface.setTitle("Control " + controling.name);
      Z21_GET_LOC_INFO(controling.adress, new AdvRunnable() {
        public Object run(Object locst) {
          LocStatus status = (LocStatus) locst;
          speed = status.speed;
          return null;
        }
      });
    }
    else noLoop();
  }
  
  public void makeGUI() {
    functions = new Button[29];
    final int adress = controling.adress;
    for (int i = 0; i < 15; i++) {
      final int index = i;
      final FunctionDefinition func = controling.functions[i];
      functions[i] = new Button(5, i * 25 + 5, 74, 20, "F" + i + ": " + func.name, !func.flip, new AdvRunnable() {
        public Object run(Object btn) {
          Button button = (Button) btn;
          if (func.flip) Z21_SET_LOC_FUNCTION(adress, index, FLIP);
          else
          {
            if (!button.wasPressed) Z21_SET_LOC_FUNCTION(adress, index, ON);
            if (button.wasPressed) Z21_SET_LOC_FUNCTION(adress, index, OFF);
          }
          return null;
        }
      });
      functions[i].enabled = func.used;
    }
    for (int i = 15; i < 29; i++) {
      final int index = i;
      final FunctionDefinition func = controling.functions[i];
      functions[i] = new Button(85, (i - 15) * 25 + 5, 74, 20, "F" + i + ": " + func.name, !func.flip, new AdvRunnable() {
        public Object run(Object btn) {
          Button button = (Button) btn;
          if (func.flip) Z21_SET_LOC_FUNCTION(adress, index, FLIP);
          else
          {
            if (!button.wasPressed) Z21_SET_LOC_FUNCTION(adress, index, ON);
            if (button.wasPressed) Z21_SET_LOC_FUNCTION(adress, index, OFF);
          }
          return null;
        }
      });
      functions[i].enabled = func.used;
    }
  }
  
  public void exit() {
    stopControl();
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Z21" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
