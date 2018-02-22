

/**
 * @author Julian Scheffers
**/

/*
 * Z21 Railway System, Copyright (©) Julian Scheffers, all rights reserved.
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
          boolean c = true;
          if (level == ' ' && file[i].charAt(p) == '\\') {
            level = '\\';
            c = false;
          }
          if (level == ' ' && file[i].charAt(p) == '\'') {
            level = '\'';
          }
          if (level == ' ' && file[i].charAt(p) == '\"') {
            level = '\"';
          }
          if (level == '\\' && file[i].charAt(p) == '\\') {
            level = ' ';
            c = false;
          }
          if (level == '\'' && file[i].charAt(p) == '\'') {
            level = ' ';
          }
          if (level == '\"' && file[i].charAt(p) == '\"') {
            level = ' ';
          }
          if (level != ' ' && file[i].charAt(p) == ':') decoded += "\n";
          else if (c) decoded += file[i].charAt(p);
        }
        String[] split = split(decoded, ':');
        for (int p = 0; p < split.length; p++) {
          split[p] = split[p].replace('\n', ':').replace('\t', ':');
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