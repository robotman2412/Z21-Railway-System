package z21railway;

import hypermedia.net.UDP;
import java.awt.Dimension;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

import jutils.gui.ScrollBar;
import jutils.guiv2.style.*;
import processing.awt.PSurfaceAWT.SmoothCanvas;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

import jutils.guiv2.*;

public class Z21 extends PApplet {
	UDP com;
	String z21Address = "192.168.178.111";
	String connectionStatus = "NO_CONNECTION";
	Style guiStyle;
	Button z21Connect;
	Button z21Disconnect;
	Button locRailwayToggle;
	Button z21GoStopToggle;
	Button z21StopAll;
	Button lockedButton;
	TextInput ipInput;
	ScrollBar locLibScroll;
	Text connectionText;
	Text serialNumber;
	Text xBusVersion;
	Text firmwareversion;
	Text operationStatus;
	Text powerMode;
	Text mainTrackStatus;
	Text programTrackStatus;
	Text z21Temp;
	Text current;
	Text warning;
	Timer timer;
	byte tractState;
	float z21FirmwareVersion;
	boolean warn = false;
	boolean inLocLib = true;
	boolean isStart = false;
	boolean isLocked = false;
	boolean inMenu = false;
	PREFReader prefReader = new PREFReader();
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
	static final String STAT_NO_CONNECTION = "NO_CONNECTION";
	static final String STAT_CONNECTING = "CONNECTING";
	static final String STAT_CONNECTED = "CONNECTED";
	static final String STAT_CONNECT_FAILED = "CONNECT_FAILED";
	static final int ON = 1;
	static final int OFF = 0;
	static final int FLIP = 2;
	static final boolean FOREWARD = true;
	static final boolean BACKWARD = false;
	final TextStyle STYLE_TEXT_WARN = new TextStyle(-65536, 24, 3);
	final ButtonStyle STYLE_BUTTON_STOP = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(-65536, 127, 1), new Style(-43691, -10440705, 3), new Style(-3407872, -10131823, 1), new Style(255, 200, 1));
	final ButtonStyle STYLE_BUTTON_GO = new ButtonStyle(new TextStyle(255), new TextStyle(200), new Style(-16711936, 127, 1), new Style(-11141291, -10440705, 3), new Style(-16724992, -10131823, 1), new Style(255, 200, 1));
	final ButtonStyle STYLE_BUTTON_LOCKED = new ButtonStyle(new TextStyle(0), new TextStyle(0), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1), new Style(255, 127, 1));
	int sketchWidth;
	int sketchHeight;
	PApplet ref;

	public Z21() {
		ref = this;
	}

	public void setup() {
		((SmoothCanvas)getSurface().getNative()).getFrame().setMinimumSize(new Dimension(760, 510));
		surface.setResizable(true);
		surface.setTitle("Z21 Railway System | Alpha build 28");
		icon = loadImage("icon.png");
		if (icon != null) {
			surface.setIcon(icon);
		}

		dataRequests = new DataRequest[0];
		guiStyle = new Style(255, 127, 1);
		z21Connect = new Button(this, 5, 5, 69, 19, "connect", false, this::connect);
		z21Disconnect = new Button(this, 80, 5, 69, 19, "disconnect", false, () -> {
			stopControl();
			disconnect();
		});
		locRailwayToggle = new Button(this, 5, 35, 79, 19, "railway", false, this::locrailway);
		z21GoStopToggle = new Button(this, 90, 35, 59, 19, "go/stop", false, STYLE_BUTTON_STOP, this::gostop);
		z21StopAll = new Button(this, 0, 65, 69, 19, "stop all", false, STYLE_BUTTON_STOP, this::Z21_STOP_ALL);
		lockedButton = new Button(this, 0, 0, 330, 19, "your z21 start is locked! click here to get unlock code.", false, STYLE_BUTTON_LOCKED, () -> link("http://www.roco.cc/en/product/238834-unlock-0-0-0-0-0-004001/products.html"));
		connectionText = new Text(this, 160, 20, "No connection.");
		serialNumber = new Text(this, 260, 20, "Serial adress: ------");
		xBusVersion = new Text(this, 400, 20, "X-Bus version: ----");
		firmwareversion = new Text(this, 530, 20, "Firmware: ----");
		operationStatus = new Text(this, 160, 50, "Operation: ------");
		powerMode = new Text(this, 360, 50, "Power: -------");
		mainTrackStatus = new Text(this, 475, 50, "Main: -----");
		programTrackStatus = new Text(this, 565, 50, "Prog: -----");
		z21Temp = new Text(this, 655, 50, "Temp: ----");
		warning = new Text(this, (int)(width / 2), (int)(height / 2), "", STYLE_TEXT_WARN);
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				checkRailwayStatus();
			}
		}, 1000L, 250L);
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				backup();
			}
		}, 1000L, 60000L);
		ipInput = new TextInput(this, 0, 5, 120, 19, TextInputType.STRING, "IP adress");
		locLibScroll = new ScrollBar(this, true, 0, 65, 100, 10);
		PREF lastSession = prefReader.load("lastSession.pref");
		if (lastSession != null) {
			PREFEntry lastIP = lastSession.get("IP");
			if (lastIP != null && lastIP.elements.length == 1) {
				ipInput.text = lastIP.elements[0];
				timer.schedule(new TimerTask() {
					public void run() {
						connect();
					}
				}, 200L);
			}
		}

		locIndex = prefReader.load("locIndex.pref");
		if (locIndex == null) {
			locIndex = new PREF();
			locIndex.set(new PREFEntry("num", new String[]{"0"}));
		}

		decodeLocIndex();
		locSelectStyle = new Style(255, 127, 1);
		locSelectAdress = new Text(this, 5, 80, "Adress");
		locSelectName = new Text(this, 55, 80, "Name");
		locSelectOwner = new Text(this, 200, 80, "Owner");
		addLoc = new Button(this, 0, 90, 69, 19, "add loc...", false, () -> openMenu(new AddLocomotive()));
		speed_low = loadImage("speed_low.png");
		speed_high = loadImage("speed_high.png");
		direction = loadImage("direction.png");
		controler = new ControlScreen();
		runSketch(new String[]{"ControlScreen"}, controler);
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
			ipInput.render();
		} else {
			menu.keyPress();
		}

		if (keyCode == 114) {
			backup();
		}

	}

	public void mousePressed() {
		if (!inMenu) {
			ipInput.mousePressed();
		} else {
			menu.mousePress();
		}

	}

	public void warn(String message) {
		++numWarnings;
		serr("WARNING: " + message);
		if (numWarnings > 255) {
			serr("FATAL: too many warnings!");
			super.exit();
		}

	}

	public void serr(String message) {
		message = message + '\n';
		System.err.print(message);
		String[] split = split(message, '\n');
		if (logf[logf.length - 1] == null) {
			logf[logf.length - 1] = "[" + tmmins() + "][System.err] ";
		}

		int lind = logf.length - 1;
		logf = expand(logf, logf.length + split.length);

		for(int i = 0; i < split.length; ++i) {
			if (split[i] != null && !split[i].equals("")) {
				if (i > 0) {
					logf[lind + i] = "                       " + split[i];
				} else {
					String[] var10000 = logf;
					var10000[lind] = var10000[lind] + split[i];
				}
			}
		}

	}

	public static void print(String message) {
		System.out.print(message);
		String[] split = split(message, '\n');
		if (logf[logf.length - 1] == null) {
			logf[logf.length - 1] = "[" + tmmins() + "][System.out] ";
		}

		int lind = logf.length - 1;
		logf = expand(logf, logf.length + split.length);

		for(int i = 0; i < split.length; ++i) {
			if (split[i] != null && !split[i].equals("")) {
				if (i > 0) {
					logf[lind + i] = "                       " + split[i];
				} else {
					String[] var10000 = logf;
					var10000[lind] = var10000[lind] + split[i];
				}
			}
		}

	}

	public static void println(String message) {
		print(message + '\n');
	}

	public static String tmmins() {
		String hour = String.valueOf(hour());
		String second = String.valueOf(second());
		String minute = String.valueOf(minute());
		if (hour() <= 9) {
			hour = "0" + hour;
		}

		if (second() <= 9) {
			second = "0" + second;
		}

		if (minute() <= 9) {
			minute = "0" + minute;
		}

		return hour + ":" + minute + ":" + second;
	}

	public void doSpeed(int x, int y, int speed, PGraphics p) {
		p.image(speed_low, (float)x, (float)y);
		PImage overlay = speed_high.get(0, speed_low.height - speed, 25, speed * (speed_low.height / 128));
		p.image(overlay, (float)(x + (speed_low.width - speed_high.width)), (float)(y + (speed_low.height - speed)));
	}

	public void doLocSelect() {
		locSelectAdress.render();
		locSelectName.render();
		locSelectOwner.render();
		line(0, 89, (float)(width - 79), 89);
		line(50, 60, 50, (float)(89 + locomotives.length * 30));
		line(195, 60, 195, (float)(89 + locomotives.length * 30));
		line((float)(width - 214), 90, (float)(width - 214), (float)(89 + locomotives.length * 30));

		for(int i = 0; i < locomotives.length; ++i) {
			locSelectEditButtons[i].enabled = !inMenu;
			locSelectControlButtons[i].enabled = connectionStatus.equals("CONNECTED") && !inMenu;
			locSelectEditButtons[i].x = width - 134;
			locSelectControlButtons[i].x = width - 209;
			locSelectEditButtons[i].render();
			locSelectControlButtons[i].render();
			locSelectAdresses[i].render();
			locSelectNames[i].render();
			locSelectOwners[i].render();
			locSelectStyle._set(g);
			line(0, (float)(119 + i * 30), (float)(width - 79), (float)(119 + i * 30));
		}

	}

	public void decodeLocIndex() {
		int locs = 0;
		PREFEntry LRAW = locIndex.get("num");
		if (LRAW != null && LRAW.elements.length == 1) {
			locs = PApplet.parseInt(LRAW.elements[0]);
		}

		locomotives = new LocEntry[locs];

		int i;
		PREFEntry entry;
		for(i = 0; i < locomotives.length; ++i) {
			entry = locIndex.get(String.valueOf(i));
			if (entry != null && entry.elements.length == 1) {
				locomotives[i] = new LocEntry(prefReader.load(entry.elements[0].substring(1, entry.elements[0].length() - 1)));
			} else {
				locomotives[i] = new LocEntry(3, "Loc. data missing.", "", null);
			}
		}

		locSelectEditButtons = new Button[locomotives.length];
		locSelectControlButtons = new Button[locomotives.length];
		locSelectAdresses = new Text[locomotives.length];
		locSelectNames = new Text[locomotives.length];
		locSelectOwners = new Text[locomotives.length];

		for(i = 0; i < locomotives.length; ++i) {
			entry = locIndex.get(String.valueOf(i));

			final String file;
			try {
				file = entry.elements[0].substring(1, entry.elements[0].length() - 1);
			} catch (NullPointerException var9) {
				continue;
			}

			final LocEntry loc = locomotives[i];
			final int mI = i;
			locSelectEditButtons[i] = new Button(this, 0, 95 + i * 30, 49, 19, "edit", false, () -> openMenu(new EditLocomotive(mI, file)));
			locSelectControlButtons[i] = new Button(this, 0, 95 + i * 30, 69, 19, "controll", false, () -> control(loc));
			locSelectAdresses[i] = new Text(this, 5, (110 + i * 30), String.valueOf(locomotives[i].adress));
			locSelectNames[i] = new Text(this, 55, (110 + i * 30), locomotives[i].name);
			locSelectOwners[i] = new Text(this, 200, (110 + i * 30), locomotives[i].owner);
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
		guiStyle._set(g);
		line(0, 29, (float)(width - 1), 29);
		line(0, 59, (float)(width - 1), 59);
		line(154, 0, 154, 58);
		if (inLocLib) {
			line((float)(width - 79), 60, (float)(width - 79), (float)(height - 1));
			doLocSelect();
		} else {
			rect((float)(width - 79), 59, 80, 55);
			(new TextStyle(0, 12, 3))._text(g, "This function is not done yet.", (float)(width / 2), (float)(height / 2));
		}

		z21Connect.enabled = connectionStatus.equals("NO_CONNECTION") || connectionStatus.equals("CONNECT_FAILED") && !inMenu;
		z21Disconnect.enabled = connectionStatus.equals("CONNECTED") && !inMenu;
		locRailwayToggle.enabled = !inMenu;
		z21GoStopToggle.enabled = connectionStatus.equals("CONNECTED") && (!isStart || !isLocked) && !inMenu;
		z21StopAll.enabled = connectionStatus.equals("CONNECTED") && (!isStart || !isLocked) && !inMenu;
		ipInput.enabled = connectionStatus.equals("NO_CONNECTION") || connectionStatus.equals("CONNECT_FAILED") && !inMenu;
		z21Connect.render();
		z21Disconnect.render();
		locRailwayToggle.render();
		ipInput.render();
		z21GoStopToggle.render();
		z21StopAll.render();
		addLoc.render();
		connectionText.render();
		serialNumber.render();
		xBusVersion.render();
		firmwareversion.render();
		operationStatus.render();
		powerMode.render();
		mainTrackStatus.render();
		z21Temp.render();
		locLibScroll.display();
		if (!isStart) {
			programTrackStatus.render();
		}

		if (isStart && isLocked) {
			lockedButton.render();
		}

		if (warn) {
			rectMode(3);
			fill(255);
			textSize(warning.style.size);
			rect((float)(width / 2), (float)(height / 2), textWidth(warning.text), (textAscent() + textDescent()) * (float)split(warning.text, '\n').length + textAscent());
			rectMode(0);
			warning.render();
		}

	}

	public void checkSize() {
		sketchWidth = width;
		sketchHeight = height;
		z21StopAll.x = width - 74;
		lockedButton.x = width / 2 - 165;
		lockedButton.y = height / 2 - 10;
		ipInput.x = width - 125;
		textSize(warning.style.size);
		warning.x = (width / 2);
		String text = warning.text;
		warning.y = PApplet.parseInt((height / 2) - ((textAscent() + textDescent()) * split(text, '\n').length + textAscent()) / 2 + textAscent());
		textSize(12);
		locLibScroll.x = width - 94;
		locLibScroll.length = height - 70;
		locLibScroll.max = (locomotives.length + 1) * 30 - (height - 60);
		addLoc.x = width - 74;
	}

	public void checkRailwayStatus() {
		if (connectionStatus.equals("CONNECTED") && (!isStart || !isLocked)) {
			Z21_GET_STATUS_DATA();
			connectionText.text = "Connected.";
			if (tractState == 0) {
				operationStatus.text = "Operation: normal";
			}

			if (tractState == 1) {
				operationStatus.text = "Operation: EMERGENCY STOP!";
			}

			if (tractState == 2) {
				operationStatus.text = "Operation: TRACK POWER OFF!";
			}

			if (tractState == 4) {
				operationStatus.text = "Operation: SHORT CUIRCET!";
			}

			if (tractState == 6) {
				operationStatus.text = "Operation: SHORT CUIRCET!";
			}

			if (tractState == 32) {
				operationStatus.text = "Operation: normal, programming";
			}

			if ((tractState & 7) > 0) {
				operationStatus.style.col = -65536;
			} else if ((tractState & 32) > 0) {
				operationStatus.style.col = -16711936;
			} else {
				operationStatus.style.col = 0;
			}

			setgostop();
		}

		if (connectionStatus.equals("CONNECTING")) {
			connectionText.text = "Connecting...";
		}

		if (connectionStatus.equals("NO_CONNECTION")) {
			connectionText.text = "No connection.";
		}

		if (connectionStatus.equals("CONNECT_FAILED")) {
			connectionText.text = "Connect failed.";
		}

	}

	public void connect() {
		if (!ipInput.text.equals("")) {
			z21Address = ipInput.text;
		} else {
			z21Address = "192.168.178.111";
		}

		connectionStatus = "CONNECTING";
		com = new UDP(this, 21105);
		com.listen(true);
		Z21_GET_CODE();
		timer.schedule(new TimerTask() {
			public void run() {
				if (connectionStatus.equals("CONNECTING")) {
					connectionStatus = "CONNECT_FAILED";
					com.close();
					com = null;
				}

			}
		}, 3000L);
	}

	public void disconnect() {
		if (com != null) {
			Z21_LOGOUT();
			connectionStatus = "NO_CONNECTION";
			com.close();
			com = null;
			serialNumber.text = "Serial adress: ------";
			xBusVersion.text = "X-Bus version: ----";
			firmwareversion.text = "Firmware: ----";
			operationStatus.text = "operation: ------";
			powerMode.text = "Power: -------";
			mainTrackStatus.text = "Main: -----";
			programTrackStatus.text = "Prog: -----";
			z21Temp.text = "Temp: ----";
			z21GoStopToggle.text = "go/stop";
			warn = false;
			isStart = false;
		} else {
			println("Didn't have to disconnect!");
		}

	}

	public void setgostop() {
		if ((tractState & 7) > 0) {
			z21GoStopToggle.style = STYLE_BUTTON_GO;
			z21GoStopToggle.text = "GO";
		} else {
			z21GoStopToggle.style = STYLE_BUTTON_STOP;
			z21GoStopToggle.text = "STOP";
		}

	}

	public void gostop() {
		if ((tractState & 7) > 0) {
			Z21_SET_TRACK_POWER(true);
		} else {
			Z21_SET_TRACK_POWER(false);
		}

	}

	public void locrailway() {
		if (inLocLib) {
			locRailwayToggle.text = "locomotives";
			inLocLib = false;
		} else {
			locRailwayToggle.text = "railway";
			inLocLib = true;
		}

	}

	public boolean send(byte[] head, byte[] data) {
		if (com == null) {
			println("we are not connected! message cannot be sent!");
			connectionStatus = "NO_CONNECTION";
			return false;
		} else if (!connectionStatus.equals("NO_CONNECTION") && !connectionStatus.equals("CONNECT_FAILED")) {
			if (head.length == 1) {
				serr("head is 1 byte too short! bad practice, but no problem.");
				head = new byte[]{head[0], 0};
			}

			if (head.length == 0) {
				warn("head does not exist! message cannot be sent!");
				return false;
			} else {
				String s;
				if (head.length > 2) {
					s = "";
					if (head.length > 3) {
						s = "s";
					}

					warn("head is " + (head.length - 2) + " byte" + s + " too long! message cannot be sent!");
					return false;
				} else {
					if (data.length > 251) {
						s = "";
						if (data.length > 252) {
							s = "s";
						}

						warn("data is " + (data.length - 251) + " byte" + s + " too long! (out of max. 256) message cannot be sent!");
					}

					s = "";
					byte[] ndat = new byte[data.length + 4];
					ndat[0] = PApplet.parseByte(4 + data.length & 255);
					ndat[1] = 0;
					ndat[2] = head[0];
					ndat[3] = head[1];

					int i;
					for(i = 0; i < data.length; ++i) {
						ndat[i + 4] = data[i];
					}

					for(i = 0; i < ndat.length; ++i) {
						if (i > 0) {
							s = s + " ";
						}

						s = s + "0x" + hex(ndat[i]);
					}

					if (com.send(ndat, z21Address, 21105) && log) {
						println("sent packet to " + z21Address + ":21105:\n" + s);
					}

					return true;
				}
			}
		} else {
			println("we are not connected! message cannot be sent!");
			return false;
		}
	}

	public void receive(byte[] data, String host, int port) {
		String text = host + ":" + port + " sent packet:\n";

		for(int i = 0; i < data.length; ++i) {
			if (i > 0) {
				text = text + " ";
			}

			text = text + "0x" + hex(data[i]);
		}

		if (log) {
			println(text);
		}

		if (connectionStatus.equals("CONNECTING")) {
			timer.schedule(new TimerTask() {
				public void run() {
					if (!isStart || !isLocked) {
						Z21_GET_SERIAL_NUM();
						Z21_GET_XBUS_VER();
						delay(20);
						Z21_GET_FIRMWARE_VER();
						Z21_SET_BROADCAST_FLAGS(257);
					}

				}
			}, 100L);
		}

		connectionStatus = "CONNECTED";
		process(data);
	}

	public void process(byte[] data) {
		if (data.length >= 4) {
			byte[] len = new byte[]{data[0], data[1]};
			byte[] head = new byte[]{data[2], data[3]};
			byte[] ndat = new byte[data.length - 4];

			for(int i = 0; i < data.length - 4; ++i) {
				ndat[i] = data[i + 4];
			}

			data = ndat;
			if (PROCESS_GET_SERIAL_NUM(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_XBUS_VER(len, head, ndat)) {
				return;
			}

			if (PROCESS_STATUS_BROADCAST(len, head, ndat)) {
				return;
			}

			if (PROCESS_UNKNOWN_COMMAND(len, head, ndat)) {
				return;
			}

			if (PROCESS_STATUS_CHANGE(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_FIRMWARE_VER(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_BROADCAST_FLAGS(len, head, ndat)) {
				return;
			}

			if (PROCESS_STATUS_DATA_CHANGE(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_HARDWARE_VER(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_CODE(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_LOC_MODE(len, head, ndat)) {
				return;
			}

			if (PROCESS_GET_TURNOUT_MODE(len, head, ndat)) {
				return;
			}

			if (PROCESS_LOC_INFO(len, head, ndat)) {
				return;
			}

			String text = "";

			for(int i = 0; i < data.length; ++i) {
				if (i > 0) {
					text = text + " ";
				}

				text = text + "0x" + hex(data[i]);
			}

			serr("WARNING: Z21 reply is of an unknown type! packet:\n" + text);
		} else {
			serr("WARNING: message is incorrectly formatted!");
		}

	}

	public void backup() {
		PREF session = new PREF(new PREFEntry[0]);
		if (!ipInput.text.equals("")) {
			session.set(new PREFEntry("IP", new String[]{ipInput.text}));
		}

		prefReader.save((String)"data/lastSession.pref", session);
		prefReader.save((String)"data/locIndex.pref", locIndex);
		saveStrings("logs/latest.log", logf);
		saveStrings("logs/" + logfname, logf);
		println("automatic backup made.");
	}

	public void exit() {
		if (key != 27) {
			disconnect();
			PREF session = new PREF(new PREFEntry[0]);
			if (!ipInput.text.equals("")) {
				session.set(new PREFEntry("IP", new String[]{ipInput.text}));
			}

			prefReader.save((String)"data/lastSession.pref", session);
			prefReader.save((String)"data/locIndex.pref", locIndex);
			super.exit();
		}

	}

	public void Z21_GET_SERIAL_NUM() {
		send(new byte[]{16, 0}, new byte[0]);
	}

	public boolean PROCESS_GET_SERIAL_NUM(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 8 && len[1] == 0 && head[0] == 16 && head[1] == 0 && data.length == 4) {
			long serial = ByteBuffer.wrap(new byte[]{data[0], data[1], data[2], data[3], 0, 0, 0, 0}).order(ByteOrder.LITTLE_ENDIAN).getLong();
			if (log) {
				println("serial number is: " + serial);
			}

			serialNumber.text = "Serial adress: " + serial;
			return true;
		} else {
			return false;
		}
	}

	public void Z21_LOGOUT() {
		if (connectionStatus.equals("CONNECTED")) {
			send(new byte[]{48, 0}, new byte[0]);
			if (log) {
				println("successfully logged out.");
			}
		} else if (log) {
			println("didn't have to log out!");
		}

	}

	public void Z21_GET_XBUS_VER() {
		send(new byte[]{64, 0}, new byte[]{33, 33, 0});
	}

	public boolean PROCESS_GET_XBUS_VER(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 9 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == 5) {
			if (data[0] == 99 && data[1] == 33 && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
				int verfull = (data[2] & 240) >> 4;

				float versign;
				for(versign = (float)PApplet.parseInt((float)(data[2] & 15)); versign >= 1; versign /= 10) {
				}

				float version = PApplet.parseFloat(verfull) + versign;
				if (log) {
					println("X-Bus version is: V" + version);
				}

				xBusVersion.text = "X-Bus version: V" + version;
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void Z21_GET_STATUS() {
		send(new byte[]{64, 0}, new byte[]{33, 36, 5});
	}

	public void Z21_SET_TRACK_POWER(boolean power) {
		byte add = 0;
		if (power) {
			add = 1;
		}

		send(new byte[]{64, 0}, new byte[]{33, PApplet.parseByte(128 | add), PApplet.parseByte(160 | add)});
	}

	public boolean PROCESS_STATUS_BROADCAST(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 7 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == 3 && data[0] == PApplet.parseByte(97) && data[2] == (data[0] ^ data[1])) {
			tractState = PApplet.parseByte(tractState | data[1]);
			String[] states = new String[16];
			states[1] = "track power off.";
			states[2] = "track power on.";
			states[4] = "programming mode on.";
			states[8] = "SHORT CUIRCET!";
			states[9] = "SHORT CUIRCET!";
			if (log) {
				println("track state broadcast: " + states[data[1]]);
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean PROCESS_UNKNOWN_COMMAND(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 9 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == 3 && data[0] == 97 && data[1] == 130 && data[2] == 227) {
			System.err.println("WARNING: Z21 reports unknown command!");
			return true;
		} else {
			return false;
		}
	}

	public boolean PROCESS_STATUS_CHANGE(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 8 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == 4 && data[0] == 98 && data[1] == 34 && data[3] == (data[0] ^ data[1] ^ data[2])) {
			String text = "";
			if (data[2] == 0) {
				text = "normal operation.";
			}

			if ((data[2] & 1) > 0) {
				text = "emergency stop ";
			}

			if ((data[2] & 2) > 0) {
				text = "track power off ";
			}

			if ((data[2] & 4) > 0) {
				text = "short cuircet ";
			}

			if ((data[2] & 32) > 0) {
				text = "in programming mode";
			}

			if (log) {
				println("sys. status (track only): " + text);
			}

			tractState = data[2];
			return true;
		} else {
			return false;
		}
	}

	public void Z21_STOP_ALL() {
		send(new byte[]{64, 0}, new byte[]{PApplet.parseByte(128), PApplet.parseByte(128)});
	}

	public void Z21_GET_FIRMWARE_VER() {
		send(new byte[]{64, 0}, new byte[]{PApplet.parseByte(241), 10, PApplet.parseByte(251)});
	}

	public boolean PROCESS_GET_FIRMWARE_VER(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 9 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == 5 && data[0] == PApplet.parseByte(243) && data[1] == 10 && data[4] == (data[0] ^ data[1] ^ data[2] ^ data[3])) {
			z21FirmwareVersion = PApplet.parseFloat(hex(data[2]) + "." + hex(data[3]));
			if (log) {
				println("firmware version is V" + z21FirmwareVersion);
			}

			firmwareversion.text = "Firmware: V" + z21FirmwareVersion;
			return true;
		} else {
			return false;
		}
	}

	public void Z21_SET_BROADCAST_FLAGS(int flags) {
		send(new byte[]{80, 0}, new byte[]{PApplet.parseByte((flags & -16777216) >> 24), PApplet.parseByte((flags & 16711680) >> 16), PApplet.parseByte((flags & '\uff00') >> 8), PApplet.parseByte(flags & 255)});
	}

	public void Z21_GET_BROADCAST_FLAGS() {
		send(new byte[]{81, 0}, new byte[0]);
	}

	public boolean PROCESS_GET_BROADCAST_FLAGS(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 8 && len[1] == 0 && head[0] == 81 && head[1] == 0 && data.length == 4) {
			if (log) {
				println("broadcast flags are: 0x" + hex(data[0]) + hex(data[1]) + hex(data[2]) + hex(data[3]));
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean PROCESS_STATUS_DATA_CHANGE(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 20 && len[1] == 0 && head[0] == PApplet.parseByte(132) && head[1] == 0 && data.length == 16) {
			String track = "";
			if (data[12] == 0) {
				track = "normal operation.";
			}

			if ((data[12] & 1) > 0) {
				track = "emergency stop ";
			}

			if ((data[12] & 2) > 0) {
				track = "track power off ";
			}

			if ((data[12] & 4) > 0) {
				track = "short cuircet ";
			}

			if ((data[12] & 32) > 0) {
				track = "in programming mode";
			}

			String system = "";
			if ((data[13] & 1) > 0) {
				system = "OVERTEMPERATURE";
				warning.text = "WARNING: YOUR Z21 IS OVERHEAT!\nUNPLUG YOUR Z21 IMMEDIATELY TO AVOID DAMAGE!\nyou may plug it in after it has had time to cool down.";
				warn = true;
				Z21_SET_TRACK_POWER(false);
			}

			if ((data[13] & 14) > 0) {
				if (system.equals("")) {
					system = "POWER SHORTAGE!";
				} else {
					system = system + " AND POWER SHORTAGE!";
				}

				Z21_SET_TRACK_POWER(false);
				operationStatus.text = "Operation: POWER SHORTAGE!";
				operationStatus.style.col = -65536;
			} else if (system.equals("")) {
				system = system + "OK";
			} else {
				system = system + "!";
			}

			short main = ByteBuffer.wrap(new byte[]{data[0], data[1]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
			short prog = ByteBuffer.wrap(new byte[]{data[2], data[2]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
			short mainFiltered = ByteBuffer.wrap(new byte[]{data[4], data[5]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
			short temp = ByteBuffer.wrap(new byte[]{data[6], data[7]}).order(ByteOrder.LITTLE_ENDIAN).getShort();
			int supply = ByteBuffer.wrap(new byte[]{data[8], data[9], 0, 0}).order(ByteOrder.LITTLE_ENDIAN).getInt();
			int internal = ByteBuffer.wrap(new byte[]{data[10], data[11], 0, 0}).order(ByteOrder.LITTLE_ENDIAN).getInt();
			tractState = data[12];
			if (log) {
				println("sys. status (full):\ncurrent on main: " + main + "mA\n" + "current on programming: " + prog + "mA\n" + "filtered current on main: " + mainFiltered + "mA\n" + "internal temperature: " + temp + "°C\n" + "supply voltage: " + supply + "mV\n" + "internal voltage: " + internal + "mV\n" + "track status: " + track + "\n" + "system status: " + system);
			}

			powerMode.text = "Power: " + supply + "mV";
			mainTrackStatus.text = "Main: " + main + "mA";
			programTrackStatus.text = "Prog: " + prog + "mA";
			z21Temp.text = "Temp: " + temp + "°C";
			return true;
		} else {
			return false;
		}
	}

	public void Z21_GET_STATUS_DATA() {
		send(new byte[]{PApplet.parseByte(133), 0}, new byte[0]);
	}

	public void Z21_GET_HARDWARE_VER() {
		send(new byte[]{26, 0}, new byte[0]);
	}

	public boolean PROCESS_GET_HARDWARE_VER(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 12 && len[1] == 0 && head[0] == 26 && head[1] == 0 && data.length == 8) {
			String[] versions = new String[]{"black Z21 (2012)", "black Z21 (2013)", "smartrail (2012)", "white z21 (2013)", "z21 start (2016)"};
			if (log) {
				println("hardware version is: " + versions[PApplet.parseInt(data[3])]);
			}

			return true;
		} else {
			return false;
		}
	}

	public void Z21_GET_CODE() {
		send(new byte[]{24, 0}, new byte[0]);
	}

	public boolean PROCESS_GET_CODE(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 5 && len[1] == 0 && head[0] == 24 && head[1] == 0 && data.length == 1) {
			if (data[0] == 0) {
				isStart = false;
				isLocked = false;
				if (log) {
					println("Z21");
				}
			}

			if (data[0] == 1) {
				isStart = true;
				isLocked = true;
				if (log) {
					println("z21 start, locked.");
				}
			}

			if (data[0] == 2) {
				isStart = true;
				isLocked = false;
				if (log) {
					println("z21 start, unlocked");
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public void Z21_GET_LOC_MODE(int adress) {
		send(new byte[]{96, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255)});
	}

	public boolean PROCESS_GET_LOC_MODE(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 7 && len[1] == 0 && head[0] == 96 && head[1] == 0 && data.length == 3) {
			int adress = data[0] << 8 | data[1];
			if (data[2] > 0) {
				if (log) {
					println("locomotive " + adress + " is in MM mode.");
				}

				return true;
			} else {
				if (log) {
					println("locomotive " + adress + " is in DCC mode.");
				}

				return false;
			}
		} else {
			return false;
		}
	}

	public void Z21_SET_LOC_MODE(int adress, boolean mode) {
		if (mode) {
			send(new byte[]{97, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255), 1});
		} else {
			send(new byte[]{97, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255), 0});
		}

	}

	public void Z21_GET_TURNOUT_MODE(int adress) {
		send(new byte[]{96, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255)});
	}

	public boolean PROCESS_GET_TURNOUT_MODE(byte[] len, byte[] head, byte[] data) {
		if (len[0] == 7 && len[1] == 0 && head[0] == 96 && head[1] == 0 && data.length == 3) {
			int adress = data[0] << 8 | data[1];
			if (data[2] > 0) {
				if (log) {
					println("locomotive " + adress + " is in MM mode.");
				}

				return true;
			} else {
				if (log) {
					println("locomotive " + adress + " is in DCC mode.");
				}

				return false;
			}
		} else {
			return false;
		}
	}

	public void Z21_SET_TURNOUT_MODE(int adress, boolean mode) {
		if (mode) {
			send(new byte[]{97, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255), 1});
		} else {
			send(new byte[]{97, 0}, new byte[]{PApplet.parseByte((adress & '\uff00') >> 8), PApplet.parseByte(adress & 255), 0});
		}

	}

	public void Z21_GET_LOC_INFO(int adress) {
		byte[] message = new byte[]{PApplet.parseByte(227), PApplet.parseByte(240), PApplet.parseByte((adress & 16128) >> 8 | 192), PApplet.parseByte(adress & 255), 0};
		message[4] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3]);
		send(new byte[]{64, 0}, message);
	}

	public void Z21_GET_LOC_INFO(int adress, final AdvRunnable done) {
		byte[] message = new byte[]{PApplet.parseByte(227), PApplet.parseByte(240), PApplet.parseByte(adress >> 8 & 255 | 192), PApplet.parseByte(adress & 255), 0};
		message[4] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3]);
		send(new byte[]{64, 0}, message);
		addDataRequest(new DataRequest("Z21_RAILWAY_SYSTEM", "Z21_GET_LOC_INFO", "GET_LOC_INFO") {
			public Object complete(Object lst) {
				done.run(lst);
				return null;
			}
		});
	}

	public void Z21_SET_LOC_DRIVE(int adress, boolean direction, int speed) {
		byte[] message = new byte[]{PApplet.parseByte(228), 19, PApplet.parseByte((adress & 16128) >> 8 | 192), PApplet.parseByte(adress & 255), PApplet.parseByte(PApplet.parseByte(speed & 127) | PApplet.parseByte(direction)), 0};
		message[5] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
		send(new byte[]{64, 0}, message);
	}

	public void Z21_SET_LOC_FUNCTION(int adress, int function, int mode) {
		byte[] message = new byte[]{PApplet.parseByte(228), PApplet.parseByte(248), PApplet.parseByte((adress & 16128) >> 8 | 192), PApplet.parseByte(adress & 255), PApplet.parseByte(PApplet.parseByte(mode & 3) << 6 | PApplet.parseByte(function & 207)), 0};
		message[5] = PApplet.parseByte(message[0] ^ message[1] ^ message[2] ^ message[3] ^ message[4]);
		send(new byte[]{64, 0}, message);
		if (log) {
			println("set function " + function + " to " + mode + " for adress " + adress);
		}

	}

	public boolean PROCESS_LOC_INFO(byte[] len, byte[] head, byte[] data) {
		if (len[0] >= 14 && len[1] == 0 && head[0] == 64 && head[1] == 0 && data.length == len[0] - 4 && data[0] == PApplet.parseByte(239)) {
			final int adress = ((PApplet.parseInt(data[1]) & 63) << 8) + PApplet.parseInt(data[2]);
			final boolean[] function = new boolean[29];
			boolean[] DB4 = bit(data[5]);
			boolean[] DB5 = bit(data[6]);
			boolean[] DB6 = bit(data[7]);
			boolean[] DB7 = bit(data[7]);
			final boolean occupied = PApplet.parseBoolean(data[3] & 8);
			final byte speedSteps = PApplet.parseByte(data[3] & 7);
			final boolean direction = PApplet.parseBoolean(data[4] & 128);
			final byte speed = PApplet.parseByte(data[4] & 127);
			final boolean doubleHead = DB4[1];
			final boolean smartSearch = DB4[2];
			function[0] = DB4[3];
			function[1] = DB4[7];
			function[2] = DB4[6];
			function[3] = DB4[5];
			function[4] = DB4[4];

			for(int i = 0; i < 8; ++i) {
				function[5 + i] = DB5[i];
				function[13 + i] = DB6[i];
				function[21 + i] = DB7[i];
			}

			parseDataRequests(new AdvRunnable() {
				public Object run(Object req) {
					DataRequest request = (DataRequest)req;
					if (request.requestType.equals("GET_LOC_INFO") && (Integer)request.requestData == adress) {
						request.complete(new LocStatus(adress, occupied, speedSteps, direction, speed, doubleHead, smartSearch, function));
						return true;
					} else {
						return false;
					}
				}
			});
			return true;
		} else {
			return false;
		}
	}

	public AdvRunnable advRunnable(Runnable S_simple) {
		return new AdvRunnable() {
			Runnable simple = S_simple;

			public Object run(Object o) {
				simple.run();
				return null;
			}
		};
	}

	public void addDataRequest(DataRequest request) {
		for(int i = 0; i < dataRequests.length; ++i) {
			if (dataRequests[i] == null) {
				dataRequests[i] = request;
				return;
			}
		}

		dataRequests = (DataRequest[])expand(dataRequests, dataRequests.length + 1);
		dataRequests[dataRequests.length - 1] = request;
	}

	public void parseDataRequests(AdvRunnable parser) {
		for(int i = 0; i < dataRequests.length; ++i) {
			if (dataRequests[i] != null && (Boolean)parser.run(dataRequests[i])) {
				dataRequests[i] = null;
			}
		}

	}

	public void filterDataRequests() {
		DataRequest[] dr_new = new DataRequest[0];

		for(int i = 0; i < dataRequests.length; ++i) {
			if (dataRequests[i] != null) {
				dr_new = (DataRequest[])expand(dr_new, dr_new.length + 1);
				dr_new[dr_new.length - 1] = dataRequests[i];
			}
		}

		dataRequests = dr_new;
	}

	public boolean[] bit(byte in) {
		return new boolean[]{PApplet.parseBoolean(in & 1), PApplet.parseBoolean(in & 2), PApplet.parseBoolean(in & 4), PApplet.parseBoolean(in & 8), PApplet.parseBoolean(in & 16), PApplet.parseBoolean(in & 32), PApplet.parseBoolean(in & 64), PApplet.parseBoolean(in & 128)};
	}

	public static void main(String[] passedArgs) {
		String[] appletArgs = new String[]{Z21.class.getName()};
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}

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
		Text locAddress;
		Text locName;
		Text locOwner;
		Text functionsText;
		Text functionDemoText;
		Text functionToggleText;
		boolean exited;
		Style style;
		FunctionDefinition[] functions;
		Dropdown func;

		AddLocomotive() {
		}

		public void init() {
			cancel = new Button(ref, 0, 0, 49, 20, "CANCEL", false, () -> exited = true);
			add = new Button(ref, 0, 0, 49, 20, "ADD", false, this::addLoc);
			functionDemo = new Button(ref, 0, 0, 74, 20, "", false, this::tryFunction);
			adress = new TextInput(ref, 0, 0, 49, 20, TextInputType.UINT, "adress");
			name = new TextInput(ref, 0, 0, 139, 20, TextInputType.STRING, "name");
			owner = new TextInput(ref, 0, 0, 190, 20, TextInputType.STRING, "owner");
			locOwner = new Text(ref, 0, 0, "Owner's full name:");
			functionName = new TextInput(ref, 0, 0, 54, 20, TextInputType.STRING, "fn name");
			functionName.text = "unused";
			functionName.cursorPos = 6;
			functionUsed = new Checkbox(0, 0, 20, 20);
			functionToggle = new Checkbox(0, 0, 20, 20);
			locAddress = new Text(ref, 0, 0, "Adress");
			locName = new Text(ref, 0, 0, "Name:");
			functionsText = new Text(ref, 0, 0, "Functions:");
			functionDemoText = new Text(ref, 0, 0, "Try it:");
			functionToggleText = new Text(ref, 0, 0, "toggle:");
			exited = false;
			style = new Style(255, 127, 1);
			functions = new FunctionDefinition[29];
			DropdownElement[] funcSel = new DropdownElement[28];

			for(int i = 0; i < 29; ++i) {
				functions[i] = new FunctionDefinition();
				if (i > 0) {
					funcSel[i - 1] = new DropdownElement(String.valueOf(i), "F" + i);
				}
			}

			functions[0] = new FunctionDefinition("lights", true);
			func = new Dropdown(ref, 0, 0, 50, 20, funcSel, new SidedRunnable() {
				public void pre() {
					int functionIndex = func.selectedIndex + 1;
					functions[functionIndex].used = functionUsed.value;
					functions[functionIndex].name = functionName.text;
					functions[functionIndex].flip = functionToggle.value;
					Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), functionIndex, 0);
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
			style._set(ref.g);
			rect((float)(width / 2 - 200), (float)(height / 2 - 125), 400, 250);
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
			functionDemoText.render();
			adress.render();
			locAddress.render();
			name.render();
			locName.render();
			owner.render();
			locOwner.render();
			functionsText.render();
			functionUsed.display();
			functionToggle.display();
			functionToggleText.render();
			functionName.render();
			func.render();
		}

		public void mousePress() {
			adress.mousePressed();
			name.mousePressed();
			owner.mousePressed();
			functionName.mousePressed();
			func.mousePressed();
			functionUsed.render();
			functionToggle.render();
		}

		public void keyPress() {
			adress.keyPressed();
			name.keyPressed();
			owner.keyPressed();
			functionName.keyPressed();
		}

		public void tryFunction() {
			if (functionToggle.value) {
				Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, 2);
			} else if (functionDemo.pressed()) {
				Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, 1);
			} else {
				Z21_SET_LOC_FUNCTION(PApplet.parseInt(adress.text), func.selectedIndex + 1, 0);
			}

		}

		public void calcPosition() {
			cancel.x = width / 2 - 195;
			cancel.y = height / 2 + 100;
			add.x = width / 2 + 145;
			add.y = height / 2 + 100;
			adress.x = width / 2 - 195;
			adress.y = height / 2 - 100;
			locAddress.x = (width / 2 - 195);
			locAddress.y = (height / 2 - 105);
			func.x = width / 2 - 170;
			func.y = height / 2 - 60;
			functionUsed.x = width / 2 - 195;
			functionUsed.y = height / 2 - 60;
			functionName.x = width / 2 - 115;
			functionName.y = height / 2 - 60;
			functionDemo.x = width / 2 - 55;
			functionDemo.y = height / 2 - 60;
			functionDemoText.x = (width / 2 - 55);
			functionDemoText.y = (height / 2 - 65);
			functionToggle.x = width / 2 + 24;
			functionToggle.y = height / 2 - 60;
			functionToggleText.x = (width / 2 + 24);
			functionToggleText.y = (height / 2 - 65);
			functionsText.x = (width / 2 - 195);
			functionsText.y = (height / 2 - 65);
			name.x = width / 2 - 140;
			name.y = height / 2 - 100;
			locName.x = (width / 2 - 140);
			locName.y = (height / 2 - 105);
			owner.x = width / 2 + 4;
			owner.y = height / 2 - 100;
			locOwner.x = (width / 2 + 4);
			locOwner.y = (height / 2 - 105);
		}

		public void addLoc() {
			int locAdress = PApplet.parseInt(adress.text);
			String locName = name.text;
			String locOwner = owner.text;
			int functionIndex = func.selectedIndex + 1;
			functions[functionIndex].used = functionUsed.value;
			functions[functionIndex].name = functionName.text;
			functions[functionIndex].flip = functionToggle.value;
			Z21_SET_LOC_FUNCTION(locAdress, functionIndex, 0);
			int locID = 0;
			PREFEntry num = locIndex.get("num");
			if (num != null && num.elements.length == 1) {
				locID = PApplet.parseInt(num.elements[0]);
			}

			locIndex.set(new PREFEntry("num", new String[]{String.valueOf(locID + 1)}));
			LocEntry loc = new LocEntry(locAdress, locName, locOwner, functions);
			prefReader.save((String)("data/locs/LOC_" + locID + ".pref"), loc.createFile());
			locomotives = (LocEntry[])expand(locomotives, locomotives.length + 1);
			locomotives[locomotives.length - 1] = loc;
			locIndex.set(new PREFEntry(String.valueOf(locID), new String[]{"\"data/locs/LOC_" + locID + ".pref\""}));
			exited = true;
			decodeLocIndex();
			backup();
		}

		public boolean exited() {
			return exited;
		}
	}

	public class ControlScreen extends PApplet {
		Button[] functions;
		int speed;
		int speedSteps;
		boolean direction;

		public ControlScreen() {
		}

		public void settings() {
			size(250, 380);
		}

		public void setup() {
			setVisible(false);
			if (icon != null) {
				surface.setIcon(icon);
			}

		}

		public void draw() {
			background(255);
			if (functions != null) {
				for(int i = 0; i < functions.length; ++i) {
					if (functions[i] != null) {
						functions[i].render();
					}
				}
			}

			doSpeed(160, 5, speed, g);
			if (!connectionStatus.equals("CONNECTED")) {
				exit();
			}

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
						LocStatus status = (LocStatus)locst;
						speed = status.speed;
						return null;
					}
				});
			} else {
				noLoop();
			}

		}

		public void makeGUI() {
			functions = new Button[29];
			final int adress = controling.adress;

			int i;
			FunctionDefinition func;
			for(i = 0; i < 15; ++i) {
				func = controling.functions[i];
				final int mI = i;
				final FunctionDefinition mFunc = func;
				functions[i] = new Button(this, 5, i * 25 + 5, 74, 20, "F" + i + ": " + func.name, !func.flip, new AdvRunnable() {
					public Object run(Object btn) {
						Button button = (Button)btn;
						if (mFunc.flip) {
							Z21_SET_LOC_FUNCTION(adress, mI, 2);
						} else {
							if (!button.wasPressed) {
								Z21_SET_LOC_FUNCTION(adress, mI, 1);
							}

							if (button.wasPressed) {
								Z21_SET_LOC_FUNCTION(adress, mI, 0);
							}
						}

						return null;
					}
				});
				functions[i].enabled = func.used;
			}

			for(i = 15; i < 29; ++i) {
				func = controling.functions[i];
				final FunctionDefinition mFunc = func;
				final int mI = i;
				functions[i] = new Button(this, 85, (i - 15) * 25 + 5, 74, 20, "F" + i + ": " + func.name, !func.flip, new AdvRunnable() {
					public Object run(Object btn) {
						Button button = (Button)btn;
						if (mFunc.flip) {
							Z21_SET_LOC_FUNCTION(adress, mI, 2);
						} else {
							if (!button.wasPressed) {
								Z21_SET_LOC_FUNCTION(adress, mI, 1);
							}

							if (button.wasPressed) {
								Z21_SET_LOC_FUNCTION(adress, mI, 0);
							}
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

	public abstract class DataRequest {
		String requester;
		String requestType;
		Object requestData;

		DataRequest(String S_requester, String S_requestType, Object S_requestData) {
			requester = S_requester;
			requestType = S_requestType;
			requestData = S_requestData;
		}

		public abstract Object complete(Object var1);
	}

	class EditLocomotive extends AddLocomotive implements MenuScreen {
		int index;
		String locFile;

		EditLocomotive(int S_index, String S_locFile) {
			super();
			index = S_index;
			locFile = S_locFile;
		}

		public void init() {
			super.init();
			add = new Button(ref, 0, 0, 49, 20, "SAVE", false, this::saveLoc);
			functions = locomotives[index].functions;
			adress.text = String.valueOf(locomotives[index].adress);
			name.text = locomotives[index].name;
			owner.text = locomotives[index].owner;
			adress.cursorPos = adress.text.length();
			name.cursorPos = name.text.length();
			owner.cursorPos = owner.text.length();
			if (func.onChange != null) {
				func.onChange.run();
			}
		}

		public void saveLoc() {
			int locAdress = PApplet.parseInt(adress.text);
			String locName = name.text;
			String locOwner = owner.text;
			int functionIndex = func.selectedIndex + 1;
			functions[functionIndex].used = functionUsed.value;
			functions[functionIndex].name = functionName.text;
			functions[functionIndex].flip = functionToggle.value;
			Z21_SET_LOC_FUNCTION(locAdress, functionIndex, 0);
			println(locAdress);
			LocEntry loc = new LocEntry(locAdress, locName, locOwner, functions);
			prefReader.save((String)locFile, loc.createFile());
			locomotives[index] = loc;
			exited = true;
			decodeLocIndex();
			backup();
			println(locAdress);
		}
	}

	interface FileReader {
		Object load(String var1);

		Object load(File var1);

		void save(String var1, Object var2);

		void save(File var1, Object var2);

		String[] buffer(Object var1);
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
			if (!name.equals("")) {
				text = text + ": " + name;
			}

			Button button = new Button(ref, x, y, width, height, text, !flip, onPress);
			button.enabled = used;
			return button;
		}
	}

	class LocEntry {
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

				for(int ix = 0; ix < functions.length; ++ix) {
					functions[ix] = new FunctionDefinition();
				}

				println("missing locomotive file!");
			} else {
				adress = 0;
				PREFEntry ARAW = file.get("adress");
				if (ARAW != null && ARAW.elements.length == 1) {
					adress = PApplet.parseInt(ARAW.elements[0]);
				}

				if (adress == 0) {
					adress = 3;
				}

				name = "";
				PREFEntry NRAW = file.get("name");
				if (NRAW != null && NRAW.elements.length == 1 && NRAW.elements[0].length() >= 3) {
					name = NRAW.elements[0].substring(1, NRAW.elements[0].length() - 1);
				}

				owner = "";
				PREFEntry ORAW = file.get("owner");
				if (ORAW != null && ORAW.elements.length == 1 && ORAW.elements[0].length() >= 3) {
					owner = ORAW.elements[0].substring(1, ORAW.elements[0].length() - 1);
				}

				functions = new FunctionDefinition[29];

				for(int i = 0; i < 29; ++i) {
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

				for(int i = 0; i < 29; ++i) {
					functions[i] = new FunctionDefinition("", false);
				}

				functions[0] = new FunctionDefinition("lights", true);
			}

		}

		public PREF createFile() {
			PREF file = new PREF();
			file.set(new PREFEntry("adress", new String[]{String.valueOf(adress)}));
			file.set(new PREFEntry("name", new String[]{"\"" + name + "\""}));
			file.set(new PREFEntry("owner", new String[]{"\"" + owner + "\""}));
			int i;
			if (functions.length == 29) {
				for(i = 0; i < 29; ++i) {
					file.set(new PREFEntry("F" + i, new String[]{String.valueOf(functions[i].used), String.valueOf(functions[i].flip), "\"" + functions[i].name + "\""}));
				}
			} else {
				for(i = 0; i < 29; ++i) {
					file.set(new PREFEntry("F" + i, new String[]{"true", "false", "\"\""}));
				}
			}

			return file;
		}

		public PREFEntry createIndexEntry(int id) {
			return new PREFEntry("LOC_" + id, new String[]{"" + adress, "\"" + owner + "\"", "\"" + name + "\""});
		}
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

	interface MenuScreen {
		void init();

		void render();

		void mousePress();

		void keyPress();

		boolean exited();
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
			for(int i = 0; i < lines.length; ++i) {
				if (lines[i].name.equals(name)) {
					return lines[i];
				}
			}

			return null;
		}

		public void set(PREFEntry entry) {
			for(int i = 0; i < lines.length; ++i) {
				if (lines[i].name.equals(entry.name)) {
					lines[i] = entry;
					return;
				}
			}

			lines = (PREFEntry[])expand(lines, lines.length + 1);
			lines[lines.length - 1] = entry;
		}
	}

	class PREFEntry {
		String name;
		String[] elements;

		PREFEntry(String S_name, String[] S_elements) {
			name = S_name;
			elements = S_elements;
		}
	}

	class PREFReader implements FileReader {
		PREFReader() {
		}

		public PREF load(String path) {
			String[] file = loadStrings(path);
			if (file == null) {
				return null;
			} else {
				PREFEntry[] lines = new PREFEntry[file.length];

				for(int i = 0; i < file.length; ++i) {
					char level = 32;
					String decoded = "";

					for(int p = 0; p < file[i].length(); ++p) {
						if (level == 32 && file[i].charAt(p) == '\'') {
							level = 39;
						}

						if (level == 32 && file[i].charAt(p) == '"') {
							level = 34;
						}

						if (level == 39 && file[i].charAt(p) == '\'') {
							level = 32;
						}

						if (level == 34 && file[i].charAt(p) == '"') {
							level = 32;
						}

						if (level != 32 && file[i].charAt(p) == ':') {
							decoded = decoded + "\n";
						} else {
							decoded = decoded + file[i].charAt(p);
						}
					}

					String[] split = split(decoded, ':');

					for(int px = 0; px < split.length; ++px) {
						split[px] = split[px].replace('\n', ':');
					}

					String name = split[0];
					String[] elements = new String[split.length - 1];

					for(int pxx = 0; pxx < split.length - 1; ++pxx) {
						elements[pxx] = split[pxx + 1];
					}

					lines[i] = new PREFEntry(name, elements);
				}

				return new PREF(lines);
			}
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
			PREF file = (PREF)data;
			if (data == null) {
				return new String[0];
			} else {
				String[] buffer = new String[file.lines.length];

				for(int i = 0; i < file.lines.length; ++i) {
					buffer[i] = file.lines[i].name;

					for(int p = 0; p < file.lines[i].elements.length; ++p) {
						buffer[i] = buffer[i] + ":" + file.lines[i].elements[p];
					}
				}

				return buffer;
			}
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
					style.box._set(ref.g);
					rect((float)x, (float)y, (float)width, (float)height);
					style.check._set(ref.g);
					line((float)x, (float)(y + height / 2), (float)(x + width / 2), (float)(y + height));
					line((float)(x + width / 2), (float)(y + height), (float)(x + width), (float)y);
				} else {
					style.box._set(ref.g);
					rect((float)x, (float)y, (float)width, (float)height);
					style.cross._set(ref.g);
					line((float)x, (float)y, (float)(x + width), (float)(y + height));
					line((float)(x + width), (float)y, (float)x, (float)(y + height));
				}
			} else {
				style.disabled._set(ref.g);
				rect((float)x, (float)y, (float)width, (float)height);
				if (value) {
					line((float)x, (float)(y + height / 2), (float)(x + width / 2), (float)(y + height));
					line((float)(x + width / 2), (float)(y + height), (float)(x + width), (float)y);
				} else {
					line((float)x, (float)y, (float)(x + width), (float)(y + height));
					line((float)(x + width), (float)y, (float)x, (float)(y + height));
				}
			}

		}

		public void render() {
			if (enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
				value = !value;
			}

		}
	}

	class CheckboxStyle {
		Style check;
		Style cross;
		Style box;
		Style disabled;

		CheckboxStyle() {
			check = new Style(255, -16711936, 1);
			cross = new Style(255, -65536, 1);
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

}
