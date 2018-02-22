

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
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
  TextInput address;
  TextInput name;
  TextInput owner;
  TextInput functionName;
  Checkbox functionUsed;
  Checkbox functionToggle;
  Text Address;
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
    address = new TextInput(0, 0, 49, 20, false, true, "address");
    name = new TextInput(0, 0, 139, 20, false, false, "name");
    owner = new TextInput(0, 0, 190, 20, false, false, "owner");
    Owner = new Text(0, 0, "owner\'s full name:");
    functionName = new TextInput(0, 0, 54, 20, false, false, "fn name");
    functionName.text = "unused";
    functionName.cursorPos = 6;
    functionUsed = new Checkbox(0, 0, 20, 20);
    functionToggle = new Checkbox(0, 0, 20, 20);
    Address = new Text(0, 0, "Address");
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
        Z21_SET_LOC_FUNCTION(int(address.text), functionIndex, OFF);
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
    address.enabled = !func.open;
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
    address.display();
    Address.display();
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
    address.select();
    name.select();
    owner.select();
    functionName.select();
    func.clicked();
    functionUsed.render();
    functionToggle.render();
  }
  public void keyPress() {
    address.render();
    name.render();
    owner.render();
    functionName.render();
  }
  void tryFunction() {
    if (functionToggle.value) Z21_SET_LOC_FUNCTION(int(address.text), func.selectedIndex + 1, FLIP);
    else if (functionDemo.pressed()) Z21_SET_LOC_FUNCTION(int(address.text), func.selectedIndex + 1, ON);
    else Z21_SET_LOC_FUNCTION(int(address.text), func.selectedIndex + 1, OFF);
  }
  void calcPosition() {
    cancel.x = width / 2 - 195;
    cancel.y = height / 2 + 100;
    add.x = width / 2 + 145;
    add.y = height / 2 + 100;
    address.x = width / 2 - 195;
    address.y = height / 2 - 100;
    Address.x = width / 2 - 195;
    Address.y = height / 2 - 105;
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
  void addLoc() {
    int locAddress = int(address.text);
    String locName = name.text.replace("\"", "\\\"").replace("\'", "\\\'").replace(":", "\t");
    String locOwner = owner.text.replace("\"", "\\\"").replace("\'", "\\\'").replace(":", "\t");
    int functionIndex = func.selectedIndex + 1;
    functions[functionIndex].used = functionUsed.value;
    functions[functionIndex].name = functionName.text;
    functions[functionIndex].flip = functionToggle.value;
    Z21_SET_LOC_FUNCTION(locAddress, functionIndex, OFF);
    int locID = 0;
    PREFEntry num = locIndex.get("num");
    if (num != null && num.elements.length == 1) locID = int(num.elements[0]);
    locIndex.set(new PREFEntry("num", new String[]{(locID + 1) + ""}));
    LocEntry loc = new LocEntry(locAddress, locName, locOwner, functions);
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
    address.text = locomotives[index].address + "";
    name.text = locomotives[index].name;
    owner.text = locomotives[index].owner;
    address.cursorPos = address.text.length();
    name.cursorPos = name.text.length();
    owner.cursorPos = owner.text.length();
    func.onChange.post();
  }
  void saveLoc() {
    int locAddress = int(address.text);
    String locName = name.text.replace("\"", "\\\"").replace("\'", "\\\'").replace(":", "\t");
    String locOwner = owner.text.replace("\"", "\\\"").replace("\'", "\\\'").replace(":", "\t");
    int functionIndex = func.selectedIndex + 1;
    functions[functionIndex].used = functionUsed.value;
    functions[functionIndex].name = functionName.text;
    functions[functionIndex].flip = functionToggle.value;
    Z21_SET_LOC_FUNCTION(locAddress, functionIndex, OFF);
    LocEntry loc = new LocEntry(locAddress, locName, locOwner, functions);
    PREF.save(locFile, loc.createFile());
    locomotives[index] = loc;
    exited = true;
    decodeLocIndex();
    backup();
  }
}