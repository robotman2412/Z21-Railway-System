

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
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

AdvRunnable advRunnable(final Runnable S_simple) {
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

void addDataRequest(DataRequest request) {
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] == null) {
    dataRequests[i] = request;
    return;
  }
  dataRequests = (DataRequest[]) expand(dataRequests, dataRequests.length + 1);
  dataRequests[dataRequests.length - 1] = request;
}

void parseDataRequests(AdvRunnable parser) {
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] != null) {
    if ((boolean) parser.run(dataRequests[i])) {
      dataRequests[i] = null;
    }
  }
}

void filterDataRequests() {
  DataRequest[] dr_new = new DataRequest[0];
  for (int i = 0; i < dataRequests.length; i++) if (dataRequests[i] != null) {
    dr_new = (DataRequest[]) expand(dr_new, dr_new.length + 1);
    dr_new[dr_new.length - 1] = dataRequests[i];
  }
  dataRequests = dr_new;
}

boolean[] bit(byte in) {
  return new boolean[]{boolean(in & 0x01), boolean(in & 0x02), boolean(in & 0x04), boolean(in & 0x08), boolean(in & 0x10), boolean(in & 0x20), boolean(in & 0x40), boolean(in & 0x80)};
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