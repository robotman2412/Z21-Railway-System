

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
 */


//2.1
void Z21_GET_SERIAL_NUM() {
  send(new byte[]{0x10, 0x00}, new byte[0]);
}

//2.1
boolean PROCESS_GET_SERIAL_NUM(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x08 && len[1] == 0x00 && head[0] == 0x10 && head[1] == 0x00 && data.length == 4) {
    long serial = ByteBuffer.wrap(new byte[]{data[0], data[1], data[2], data[3], 0x00, 0x00, 0x00, 0x00}).order(ByteOrder.LITTLE_ENDIAN).getLong();
    if (log) println("serial number is: " + serial);
    SerialNum.text = "Serial address: " + serial;
    return true;
  }
  else return false;
}

//2.2
void Z21_LOGOUT() {
  if (CONNECT_STATUS.equals(STAT_CONNECTED)) {
    send(new byte[]{0x30, 0x00}, new byte[0]);
    if (log) println("successfully logged out.");
  }
  else if (log) println("didn't have to log out!");
}

//2.3
void Z21_GET_XBUS_VER() {
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, 0x21, 0x00});
}

//2.3
boolean PROCESS_GET_XBUS_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 5) {
    if (data[0] == 0x63 && data[1] == 0x21 && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
      int verfull = (data[2] & 0xf0) >> 4;
      float versign = int(data[2] & 0x0f);
      while (versign >= 1) versign /= 10;
      float version = float(verfull) + versign;
      if (log) println("X-Bus version is: V" + version);
      XBusVersion.text = "X-Bus version: V" + version;
      return true;
    }
    else return false;
  }
  else return false;
}

//2.4
void Z21_GET_STATUS() {
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, 0x24, 0x05});
}

//2.5 & 2.6
void Z21_SET_TRACK_POWER(boolean power) {
  byte add = 0x00;
  if (power) add = 0x01;
  send(new byte[]{0x40, 0x00}, new byte[]{0x21, byte(0x80 | add), byte(0xa0 | add)});
}

//2.7 > 2.10
boolean PROCESS_STATUS_BROADCAST(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 3 && data[0] == byte(0x61) && data[2] == (data[0] ^ data[1])) {
    TRACK_STATE = byte(TRACK_STATE | data[1]);
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
boolean PROCESS_UNKNOWN_COMMAND(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 3 && data[0] == 0x61 && data[1] == 0x82 && data[2] == 0xe3) {
    System.err.println("WARNING: Z21 reports unknown command!");
    return true;
  }
  else return false;
}

//2.12
boolean PROCESS_STATUS_CHANGE(byte[] len, byte[] head, byte[] data) {
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
void Z21_STOP_ALL() {
  send(new byte[]{0x40, 0x00}, new byte[]{byte(0x80), byte(0x80)});
}

//2.15
void Z21_GET_FIRMWARE_VER() {
  send(new byte[]{0x40, 0x00}, new byte[]{byte(0xf1), 0x0a, byte(0xfb)});
}

//2.15
boolean PROCESS_GET_FIRMWARE_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x09 && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == 5 && data[0] == byte(0xf3) && data[1] == 0x0a && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
    Z21_FIRMWARE_VER = float(hex(data[2]) + "." + hex(data[3]));
    if (log) println("firmware version is V" + Z21_FIRMWARE_VER);
    Firmware.text = "Firmware: V" + Z21_FIRMWARE_VER;
    return true;
  }
  else return false;
}

//2.16
void Z21_SET_BROADCAST_FLAGS(int flags) {
  send(new byte[]{0x50, 0x00}, new byte[]{byte((flags & 0xff000000) >> 24), byte((flags & 0x00ff0000) >> 16), byte((flags & 0x0000ff00) >> 8), byte(flags & 0x000000ff)});
}

//2.17
void Z21_GET_BROADCAST_FLAGS() {
  send(new byte[]{0x51, 0x00}, new byte[]{});
}

//2.17
boolean PROCESS_GET_BROADCAST_FLAGS(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x08 && len[1] == 0x00 && head[0] == 0x51 && head[1] == 0x00 && data.length == 4) {
    if (log) println("broadcast flags are: 0x" + hex(data[0]) + hex(data[1]) + hex(data[2]) + hex(data[3]));
    return true;
  }
  else return false;
}

//2.18
boolean PROCESS_STATUS_DATA_CHANGE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x14 && len[1] == 0x00 && head[0] == byte(0x84) && head[1] == 0x00 && data.length == 16) {
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
      Operation.style.col = #ff0000;
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
                   + "internal temperature: " + temp + "°C\n"
                   + "supply voltage: " + supply + "mV\n"
                   + "internal voltage: " + internal + "mV\n"
                   + "track status: " + track + "\n"
                   + "system status: " + system);
    Power.text = "Power: " + supply + "mV";
    Main.text = "Main: " + main + "mA";
    Prog.text = "Prog: " + prog + "mA";
    Temperature.text = "Temp: " + temp + "°C";
    return true;
  }
  else return false;
}

//2.19
void Z21_GET_STATUS_DATA() {
  send(new byte[]{byte(0x85), 0x00}, new byte[]{});
}

//2.20
void Z21_GET_HARDWARE_VER() {
  send(new byte[]{0x1a, 0x00}, new byte[]{});
}

//2.20
boolean PROCESS_GET_HARDWARE_VER(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x0c && len[1] == 0x00 && head[0] == 0x1a && head[1] == 0x00 && data.length == 8) {
    String[] versions = new String[5];
    versions[0] = "black Z21 (2012)";
    versions[1] = "black Z21 (2013)";
    versions[2] = "smartrail (2012)";
    versions[3] = "white z21 (2013)";
    versions[4] = "z21 start (2016)";
    if (log) println("hardware version is: " + versions[int(data[3])]);
    //unreliable, firmware changed and the format with it.
    return true;
  }
  else return false;
}

//2.21
void Z21_GET_CODE() {
  send(new byte[]{0x18, 0x00}, new byte[]{});
}

//2.21
boolean PROCESS_GET_CODE(byte[] len, byte[] head, byte[] data) {
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
void Z21_GET_LOC_MODE(int address) {
  send(new byte[]{0x60, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff)});
}

//3.1
boolean PROCESS_GET_LOC_MODE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x60 && head[1] == 0x00 && data.length == 3) {
    int address = (data[0] << 8) | data[1];
    if (data[2] > 0) {
      if (log) println("locomotive " + address + " is in MM mode.");
      return true;
    }
    else
    {
      if (log) println("locomotive " + address + " is in DCC mode.");
      return false;
    }
  }
  else return false;
}

//3.2
void Z21_SET_LOC_MODE(int address, boolean mode) {
  if (mode) send(new byte[]{0x61, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff), 0x01});
  else send(new byte[]{0x61, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff), 0x00});
}

//3.3
void Z21_GET_TURNOUT_MODE(int address) {
  send(new byte[]{0x60, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff)});
}

//3.3
boolean PROCESS_GET_TURNOUT_MODE(byte[] len, byte[] head, byte[] data) {
  if (len[0] == 0x07 && len[1] == 0x00 && head[0] == 0x60 && head[1] == 0x00 && data.length == 3) {
    int address = (data[0] << 8) | data[1];
    if (data[2] > 0) {
      if (log) println("locomotive " + address + " is in MM mode.");
      return true;
    }
    else
    {
      if (log) println("locomotive " + address + " is in DCC mode.");
      return false;
    }
  }
  else return false;
}

//3.4
void Z21_SET_TURNOUT_MODE(int address, boolean mode) {
  if (mode) send(new byte[]{0x61, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff), 0x01});
  else send(new byte[]{0x61, 0x00}, new byte[]{byte((address & 0x0000ff00) >> 8), byte(address & 0x000000ff), 0x00});
}

//4.1
void Z21_GET_LOC_INFO(int address) {
  byte[] message = new byte[]{byte(0xe3), byte(0xf0), byte(((address & 0x00003f00) >> 8) | 0xc0), byte(address & 0x000000ff), 0x00};
  message[4] = byte(message[0] ^ message[1] ^ message[2] ^ message[3]);
  send(new byte[]{0x40, 0x00}, message);
}

//4.1
void Z21_GET_LOC_INFO(int address, final AdvRunnable done) {
  byte[] message = new byte[]{byte(0xe3), byte(0xf0), byte(((address >> 8) & 0x000000ff) | 0xc0), byte(address & 0x000000ff), 0x00};
  message[4] = byte(message[0] ^ message[1] ^ message[2] ^ message[3]);
  send(new byte[]{0x40, 0x00}, message);
  addDataRequest(new DataRequest("Z21_GET_LOC_INFO", "GET_LOC_INFO", address) {
    public Object complete(Object lst) {
      done.run(lst);
      return null;
    }
  });
}

//4.2
void Z21_SET_LOC_DRIVE(int address, boolean direction, int speed) {
  byte[] message = new byte[]{byte(0xe4), 0x13, byte(((address & 0x00003f00) >> 8) | 0xc0), byte(address & 0x000000ff), byte(byte(speed & 0x0000007f) | byte(byte(direction) << 7)), 0x00};
  message[5] = byte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
  send(new byte[]{0x40, 0x00}, message);
}

//4.3
void Z21_SET_LOC_FUNCTION(int address, int function, int mode) {
  byte[] message = new byte[]{byte(0xe4), byte(0xf8), byte(((address & 0x00003f00) >> 8) | 0xc0), byte(address & 0x000000ff), byte((byte(mode & 0x00000003) << 6) | byte(function & 0x000000cf)), 0x00};
  message[5] = byte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
  send(new byte[]{0x40, 0x00}, message);
  if (log) println("set function " + function + " to " + mode + " for address " + address);
}

//4.4
boolean PROCESS_LOC_INFO(byte[] len, byte[] head, byte[] data) {
  if (len[0] >= 0x0e && len[1] == 0x00 && head[0] == 0x40 && head[1] == 0x00 && data.length == len[0] - 4 && data[0] == byte(0xef)) {
    final int address = ((int(data[1]) & 0x3F) << 8) + int(data[2]);
    boolean[] function = new boolean[29];
    boolean[] DB4 = bit(data[5]);
    boolean[] DB5 = bit(data[6]);
    boolean[] DB6 = bit(data[7]);
    boolean[] DB7 = bit(data[7]);
    final boolean occupied = boolean(data[3] & 0x08);
    final byte speedSteps = byte(data[3] & 0x07);
    final boolean direction = boolean(data[4] & 0x80);
    final byte speed = byte(data[4] & 0x7f);
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
        if (request.requestType.equals("GET_LOC_INFO") && (int) request.requestData == address) {
          request.complete(new LocStatus(address, occupied, speedSteps, direction, speed, doubleHead, smartSearch, functions));
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