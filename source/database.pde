

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
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
  PREFEntry get(String name) {
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].name.equals(name)) return lines[i];
    }
    return null;
  }
  void set(PREFEntry entry) {
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
  Button generateButton(int x, int y, int width, int height, int num, Runnable onPress) {
    String text = "F" + num;
    if (!name.equals("")) text += ": " + name;
    Button button = new Button(x, y, width, height, text, !flip, onPress);
    button.enabled = used;
    return button;
  }
}

class LocEntry {//add controll functions and speed steps
  int address;
  String name;
  String owner;
  FunctionDefinition[] functions;
  LocEntry(PREF file) {
    if (file == null) {
      address = 3;
      name = "error while loading.";
      owner = "";
      functions = new FunctionDefinition[29];
      for (int i = 0; i < functions.length; i++) functions[i] = new FunctionDefinition();
      println("missing locomotive file!");
    }
    else
    {
      address = -1;
      PREFEntry ARAW = file.get("address");
      if (ARAW != null && ARAW.elements.length == 1) address = int(ARAW.elements[0]);
      if (address == -1) address = 3;
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
          functions[i] = new FunctionDefinition(FRAW.elements[2].substring(1, FRAW.elements[2].length() - 1), boolean(FRAW.elements[1]));
          functions[i].used = boolean(FRAW.elements[0]);
        }
      }
    }
  }
  LocEntry(int S_address, String S_name, String S_owner, FunctionDefinition[] S_functions) {
    address = S_address;
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
  PREF createFile() {
    PREF file = new PREF();
    file.set(new PREFEntry("address", new String[]{address + ""}));
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
  PREFEntry createIndexEntry(int id) {
    return new PREFEntry("LOC_" + id, new String[]{"" + address, "\"" + owner + "\"", "\"" + name + "\""});
  }
}