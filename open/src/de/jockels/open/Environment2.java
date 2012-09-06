package de.jockels.open;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;
import android.util.Pair;

/**
 * Hilfsklasse mit vier Funktionsblöcken, die auf die Problematik eingehen, dass viele moderne Android-Geräte technisch zwei
 * SD-Karten haben: Der "alte", den Android als "External Memory" oder unter "mnt/sdcard" anspricht, ist dabei fest eingebaut;
 * er heißt nur noch so, ist aber nicht wechselbar und auch nicht als SD-Karte implementiert. Der bei diesen Geräten zugängliche 
 * Slot für eine microSD-Karte wird nicht über diese External-Methoden angesprochen und liegt daher für die meisten Apps 
 * brach.
 *
 * 1) Die bei vielen modernen Geräten (v.a. Tablets) vorhandene zweite, externe SD-Karte Apps zugänglich machen. Dazu 
 * sind von den ganzen "External"-Methoden zum Zugriff auf die interne SD-Karte (in Environment und in Context) Äquivalente mit 
 * "Secondary" im Namen vorhanden, sowie ein Algorithmus, der versucht, den korrekte MountPoint für diese zweite Karte 
 * herauszufinden.
 * 
 * 2) Weil dieser Algorithmus versagen kann oder weil Anwender ihre Daten vielleicht noch wo anders speichern möchten, ist
 * auch eine Durchsuchfunktion für alle extern anbindbaren Geräte (diese Zweit-SD, aber auch USB-Geräte oder Kartenleser) 
 * vorhanden. Eine App  könnte damit eine Liste aller verfügbaren Speichermedien (inklusive der internen SD-Karte) anzeigen, 
 * zusammen mit deren Kapazität und freien Speicherplatz.
 * 
 * 3) Zwei Hilfsmethoden, die die beide ab API9 und 13 in Environment vorhandenen Funktionen zur Analyse des Geräts
 * auch unter API8 nutzbar machen, und die einige Besonderheiten von unseren Testgeräten berücksichtigen:
 *  
 * 	isExternalStorageRemovable()		true = dieser "/mnt/sdcard" ist wie bei alten Geräten eine echte microSD-Card
 * 																	false = ist fest eingebaut und daher meist keine echte physische SD-Karte
 * 
 * 	isExternalStorageEmulated()			false = wie bei alten Geräten, /data und /mnt/sdcard sind getrennte Partitionen
 * 																	true = ab Honeycomb hat Google das Problem mit einer zu kleinen /data-Partiton 
 *			erkannt, aber etwas unglücklich gelöst: Der gesamte interne Speicher ist für /data nutzbar, aber das ist so 
 *			implementiert, dass /data und /mnt/sdcard denselben Speicher nutzen. Das wiederum hat zur Folge, dass 
 *			/mnt/sdcard schon vom internen Speicher belegt ist und gerade nicth auf einer echten microSD-Karte liegen 
 *			kann. Die Konsequenz: Apps können nicht mehr auf diese echte microSD ausgelagert werden, und sie können
 *			die microSD auch nicht so ohne weiteres ansprechen, denn die ganzen dafür ansich ausgelegten Funktionen
 *			von Android mit "External" im Namen zeigen auf /mnt/sdcard...
 * 
 * 4) Ein paar kleine Hilfsfunktionen (Klasse Size) zum Auslesen der Speicherkapazitäten einer Partition, die ab API8 klappt, aber
 * ab API9 die File-Methoden dazu nutzt.
 * 
 * Diese Liste aller mountbaren Geräte (wozu diese Zweit-SDs zählt) lässt sich glücklicherweise bei allen bisher getesteten 
 * Geräten aus der Systemdatei /system/etc/vold.fstab auslesen, einer Konfigurationsdatei eines Linux-Dämons, der genau 
 * für das Einbinden dieser Geräte zuständig ist. Es mag Custom-ROMs geben, wo diese Methode nicht funktioniert.
 * 
 * Der MountPoint für die zweite SD-Karte stand bei allen bisher getesteten Geräten direkt an erster Stelle dieser Datei,
 * bei einigen nach /mnt/sdcard an zweiter Stelle. Andere Algorithmen zum Herausfinden 

 *  Varianten des SD-Pfads sind:
 *  	Asus Transformer	/Removable/MicroSD
 *  	HTC Velocity LTE		/mnt/sdcard/ext_sd
 *  	Huawei MediaPad	/mnt/external
 *  	Intel Orange				/mnt/sdcard2
 *  	LG Prada						/mnt/sdcard/_ExternalSD
 *  	Motorola Razr			/mnt/sdcard-ext
 *  	Motorola Xoom		/mnt/external1
 *  	Samsung Note			/mnt/sdcard/external_sd (und Pocket und Mini 2)
 *  	Samsung S3				/mnt/extSdCard
 *  
 *  Die MountPoints der USB-Geräte haben alle ein "usb" im Namen(.toLowerCase), aber bei einigen Geräten auch ein 
 *  "sd". Nicht alle SD-Mountpoints haben ein "sd" im Namen, manche auch nur "ext". Einige Hersteller hängen die
 *  Karte unter /mnt ein, andere in die interne Karte /mnt/sdcard (was dazu führt, dass einige Unterverzeichnisse
 *  von /mnt/sdcard größer sind als der gesamte Speicherbereich ;-), wieder andere in ein anderes Root-Verzeichnis.
 *  
 */

public class Environment2  {
	private static final String TAG = "Environment2";

	private static ArrayList<Device> mDeviceList = null;
	private static boolean mExternalEmulated = false;
	private static Device mPrimary = null;
	private static Device mSecondary = null;
	
	
	/**
	 * Miniklasse für Partitions-Größe, die auch mit alten Android-Versionen zusammen klappt und ab Gingerbread die File-Methoden benutzt
	 * first = free = getUsableSpace; secondary = size = getTotalSpace
	 */
	public static class Size extends Pair<Long,Long> {
		public Size(long free, long size) { super(free, size); }
		public long guessSize() {
			long g = 1024*1024;
			while (second>g) g *= 2;
			return g;
		}
	
		@SuppressLint("NewApi")
		public static Size getSpace(File f) {
			if (f!=null) try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					return new Size(f.getUsableSpace(), f.getTotalSpace());
				} else {
					StatFs fs = new StatFs(f.getAbsolutePath());
					return new Size((long)fs.getAvailableBlocks()*fs.getBlockSize(), (long)fs.getBlockCount()*fs.getBlockSize());
				}
			} catch (Exception e) { }
			return new Size((long)0, (long)0);
		}
	}

	
	/**
	 * die Beschreibung eines Devices
	 */
	public static class Device  {
		private Size mSize;
		private String mLabel, mMountPoint, mName;
		private boolean mRemovable, mAvailable, mWriteable;
		
		/**
		 * Constructor für einen der get...Directory()-Methoden (nicht ExternalStorage)
		 */
		private Device(File f) {
			mLabel = mMountPoint = mName = f.getAbsolutePath();
			mRemovable = false;
			if (mAvailable = f.isDirectory()) {
				mWriteable = f.canWrite();
				mSize = Size.getSpace(f);
			}
		}
		
		
		/**
		 * Constructor für Environment.getExternalStorageDirectory()
		 */
		@SuppressLint("NewApi")
		private Device() {
			File f = Environment.getExternalStorageDirectory();
			mLabel = mMountPoint = f.getAbsolutePath();

			String state = Environment.getExternalStorageState();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) 
	    		mRemovable = Environment.isExternalStorageRemovable(); // Gingerbread weiß es genau
			else
				mRemovable = false; // guess, wird ggf. später korrigiert
			
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				mAvailable = mWriteable = true;
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				mAvailable = true;
				mWriteable = false;
			} else {
				mAvailable = mWriteable = false;
				// nicht mRemovable=true! Unmounted kann auch heißen, dass sie per USB am PC hängt
			}
			if (mAvailable) mSize = Size.getSpace(f);
		}
		
		/**
		 * Constructor, der eine Zeile aus vold.fstab bekommt (dev_mount schon weggelesen)
		 * @param sp
		 */
		private Device(SimpleStringSplitter sp) {
			mRemovable = true;
    		mLabel = sp.next().trim();
    		mMountPoint = sp.next().trim();
    		File f = new File(mMountPoint);
    		mName = f.getName(); // letzter Teil des Pfads
			if (mAvailable = f.isDirectory()) {
				mSize = Size.getSpace(f); 
				mWriteable = f.canWrite();
				// Korrektur, falls in /mnt/sdcard gemountet (z.B. Samsung)
				if (mMountPoint.startsWith(mPrimary.mMountPoint) && mSize.equals(mPrimary.mSize)) 
					mAvailable = mWriteable = false;
			} else 
				mWriteable = false;
		}
		public final File getFile() { return new File(mMountPoint); }
		public final Size getSize() { return mSize; }
		public final String getLabel() { return mLabel; }
		public final String getMountPoint() { return mMountPoint; }
		public final String getName() { return mName; }
		public final boolean isRemovable() { return mRemovable; }
		public final boolean isAvailable() { return mAvailable; }
		public final boolean isWriteable() { return mWriteable; }
	}


	/**
	 * leerer Constructor
	 */
	public Environment2() {
		rescanDevices();
	}
	
	
	/**
	 * Hilfe für einen Broadcastreceiver: So muss der passende IntentFilter aussehen, damit der
	 * alle Änderungen mitbekommt.
	 * @return
	 */
	public static IntentFilter getRescanIntentFilter() {
		if (mDeviceList==null) rescanDevices();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL); // rausgenommen
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED); // klappt wieder
		filter.addAction(Intent.ACTION_MEDIA_REMOVED); // entnommen
		filter.addAction(Intent.ACTION_MEDIA_SHARED); // per USB am PC
		filter.addDataScheme("file"); // geht ohne das nicht, obwohl das in der Doku nicht so recht steht

		// die folgenden waren zumindest bei den bisher mit USB getesteten Geräten nicht notwendig, da diese
		// auch bei USB-Sticks und externen SD-Karten die ACTION_MEDIA-Intents abgefeuert haben
//		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
//		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		return filter;
	}
	
	
	/**
	 * BroadcastReceiver, der einen Rescan durchführt und (als Callback) das übergebene Runnable aufruft. Muss
	 * mit unregisterReceiver freigegebe werden; dafür ist der Aufrufer verantwortlich. 
	 * Das geht dann (z.B. in onCreate() ) so: 
	 * 	mRescanReceiver = Environment2.registerRescanBroadcastReceiver(this, new Runnable() {
	 * 		public void run() {
	 * 	}});
	 * und später (z.B. in onDestroy() ):
	 * 	unregisterReceiver(mRescanReceiver);
	 * @param context der Context, in dem registerReceiver aufgerufen wird
	 * @param r der Runnable, der bei jedem An- und Abmelden von Devices ausgeführt wird; kann auch null sein
	 * @return der BroadcastReceiver, der später unregisterReceiver übergeben werden muss 
	 */
	public static BroadcastReceiver registerRescanBroadcastReceiver(Context context, final Runnable r) {
		if (mDeviceList==null) rescanDevices();
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Storage: "+intent.getAction()+"-"+intent.getData());
				rescanDevices();
				if (r!=null) r.run();
			}
		};
		context.registerReceiver(br, getRescanIntentFilter());
		return br;
	}
	
	
	/**
	 * alle Devices neu scannen
	 */
	@SuppressLint("NewApi")
	public static void rescanDevices() {
		mDeviceList = new ArrayList<Device>(10);
		mPrimary = new Device();

		// vold.fstab lesen; TODO bei Misserfolg eine andere Methode
		if (!scanVold("vold.fstab")) scanVold("vold.conf");

    	// zeigen /mnt/sdcard und /data auf denselben Speicher?
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    		mExternalEmulated = Environment.isExternalStorageEmulated();
    	} else {
    		// vor Honeycom gab es den unified memory noch nicht
    		mExternalEmulated = false; 
    	}

		// Pfad zur zweiten SD-Karte suchen; bisher nur Methode 1 implementiert
		// Methode 1: einfach der erste Eintrag in vold.fstab, ggf. um ein /mnt/sdcard-Doppel bereinigt
		// Methode 2: das erste mit "sd", falls nicht vorhanden das erste mit "ext"
		// Methode 3: das erste verfügbare
		if (mDeviceList.size()==0) {
			mSecondary = null;
		} else {
			mSecondary = mDeviceList.get(0);
			// Hack
			if (mPrimary.mRemovable) Log.w(TAG, "isExternStorageRemovable overwrite (secondary sd found) auf false");
			mPrimary.mRemovable = false;
		}

		// jetzt noch Name setzen
		mPrimary.mName = mPrimary.mRemovable ? "SD-Card" : "intern";
	}
	

	/**
	 * /system/etc/vold.fstab auswerten
	 * @return true, wenn geklappt hat; false, wenn Datei nicht (vollständig) gelesen werden konnte
	 */
	private static boolean scanVold(String name) {
		String s, f;
		boolean prefixScan = true; // sdcard-Prefixes
		SimpleStringSplitter sp = new SimpleStringSplitter(' ');
    	try {
    		BufferedReader buf = new BufferedReader(new FileReader(Environment.getRootDirectory().getAbsolutePath()+"/etc/"+name), 2048);
    		s = buf.readLine();
    		while (s!=null) {
    			sp.setString(s.trim());
    			f = sp.next(); // dev_mount oder anderes
        		if ("dev_mount".equals(f)) {
        			Device d = new Device(sp);
        			
        			if (mPrimary.mMountPoint.equals(d.mMountPoint)) {
        				// ein wenig Spezialkrams über /mnt/sdcard herausfinden
        				
        				// wenn die Gingerbread-Funktion isExternalStorageRemovable nicht da ist, diesen Hinweis nutzen
        				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) 
        					mPrimary.mRemovable = true; // dann ist auch der Standard-Eintrag removable
        					// eigentlich reicht das hier nicht, denn die vold-Einträge für die primäre SD-Karte sind viel komplexer, 
        					// oft steht da was von non-removable. Doch diese ganzen proprietären Klamotten auszuwerden,
        					// wäre viel zu komplex. Ein gangbarer Kompromiss scheint zu sein, sich ab 2.3 einfach auf
        					// isExternalStorageRemovable zu verlassen, was schon oben in Device() gesetzt wird. Bei den
        					// bisher aufgetauchten Geräten mit 2.2 wiederum scheint der Hinweis in vold zu klappen.vccfg
        				
        				// z.B. Galaxy Note hängt "encryptable_nonremovable" an
        				while (sp.hasNext()) {
        					f = sp.next();
        					if (f.contains("nonremovable")) {
        						mPrimary.mRemovable = false;
        						Log.w(TAG, "isExternStorageRemovable overwrite ('nonremovable') auf false");
        					}
        				}
        				prefixScan = false;
        			} else 
        				// nur in Liste aufnehmen, falls nicht Dupe von /mnt/sdcard
        				mDeviceList.add(d);
        			
        		} else if (prefixScan) {
        			// Weitere Untersuchungen nur, wenn noch vor sdcard-Eintrag
        			// etwas unsauber, da es eigentlich in {} vorkommen muss, was ich hier nicht überprüfe
        			
        			if ("discard".equals(f)) {
        				// manche (Galaxy Note) schreiben "discard=disable" vor den sdcard-Eintrag.
        				sp.next(); // "="
        				f = sp.next();
        				if ("disable".equals(f)) {
        					mPrimary.mRemovable = false;
        					Log.w(TAG, "isExternStorageRemovable overwrite ('discard=disable') auf false");
        				} else if ("enable".equals(f)) {
        					// ha, denkste...  bisher habe ich den Eintrag nur bei zwei Handys gefunden, (Galaxy Note, Galaxy Mini 2), und
        					// da stimmte er *nicht*, sondern die Karten waren nicht herausnehmbar.
        					// mPrimary.mRemovable = true;
        					Log.w(TAG, "isExternStorageRemovable overwrite overwrite ('discard=enable'), bleibt auf "+mPrimary.mRemovable);
        				} else
        					Log.w(TAG, "disable-Eintrag unverständlich: "+f);
        			}
        			
        		}
    			s = buf.readLine();
    		}
    		buf.close();
    		return true;
    	} catch (Exception e) {
    		Log.e(TAG, "kann "+name+" nicht lesen: "+e.getMessage());
    		return false;
    	}
	}
	

	/**
	 * Alternative zu Environment.isExternalStorageEmulated(), die ab API8 funktioniert
	 * 
	 * @return true, falls /mnt/sdcard und /data auf den gleichen Speicherbereich zeigen; 
	 * 	false, falls /mnt/sdcard einen eigenen (ggf. trotzdem nicht entfernbaren) Speicherbereich beschreibt
	 */
	public static boolean isExternalStorageEmulated() {
		if (mDeviceList==null) rescanDevices();
		return mExternalEmulated; 
	}

	
	/**
	 * Alternative zu Environment.isExternalStorageRemovable(), die ab API8 funktioniert. Achtung: Die 
	 * Bedeutung ist eine subtil andere als beim Original-Aufruf. Hier geht es (eher zu Hardware-
	 * Diagnosezwecken) darum, ob /mnt/sdcard eine physische Karte ist, die der Nutzer herausnehmen
	 * kann. Der Original-Aufruf liefert true, wenn es sein kann, dass auf /mnt/sdcard nicht zugegriffen
	 * werden kann. Das unterscheidet sich in einem Fall: fest eingelötete Karten, die per USB an einen
	 * PC freigegeben werden können und während der Freigabe für Android nicht im Zugriff stehen.
	 * 
	 * @return true, falls /mnt/sdcard auf einer entnehmbaren physischen Speicherkarte liegt
	 * 	false, falls das ein fest verlöteter Speicher ist; das heißt nicht, dass immer auf den
	 * 	Speicher zugegriffen werden kann, ein Status-Check muss dennoch stattfinden (anders
	 * 	als bei Environment.isExternalStorageRemovable()
	 */
	public static boolean isExternalStorageRemovable() { 
		if (mDeviceList==null) rescanDevices();
		return mPrimary.mRemovable;
	}
	
	
	/**
	 * LIste aller gefundenen Removable-Geräte ausgeben bzw. nach Kriterien einschränken
	 * 
	 * @param key null findet alle, sonst nur diejenigen, bei denen key in getName() vorkommt
	 * @param available false findet alle, true nur diejenigen, die eingesteckt sind
	 * @param intern mit oder ohne den internen Speicher (unter Berücksichtigung von available, aber nicht key)
	 * @return Array der Device-EInträge mit den gewünschten Suchkriterien
	 */
	public static Device[] getDevices(String key, boolean available, boolean intern) {
		if (mDeviceList==null) rescanDevices();
		if (key!=null) key = key.toLowerCase();
		ArrayList<Device> temp = new ArrayList<Device>(mDeviceList.size());
		if (intern && ( !available || mPrimary.mAvailable)) temp.add(mPrimary);
		for (Device d : mDeviceList) {
			if ( ((key==null) || d.mName.toLowerCase().contains(key)) && (!available || d.mAvailable) ) temp.add(d);
		}
		return temp.toArray(new Device[temp.size()]);
	}
	
	
	public static Device getPrimaryExternalStorage() {
		if (mDeviceList==null) rescanDevices();
		return mPrimary;
	}
	
	
	public static Device getSecondaryExternalStorage() {
		if (mDeviceList==null) rescanDevices();
		return mSecondary;
	}
	
	
	public static Device getInternalStorage() {
		if (mDeviceList==null) rescanDevices();
		return new Device(Environment.getDataDirectory());
	}

	
	/**
	* Status der Zweit-SD. Da die MEDIA-Konstanten nichts für "nicht vorhanden" vorsehen, 
	* gibt's in diesem Fall null zurück. Damit auf die Karte geschrieben werden kann, muss
	* eine Permission gesetzt sein:     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	* 
	* TODO ab JellyBean 4.1 soll es auch eine Read-Permission geben?!
	* 
	 * @return null, falls keine Zweit-SD vorhanden, sonst einer von den drei States 
	 * 	MEDIA_MOUNTED, _MOUNTED_READ_ONLY und _REMOVED
	 */
	public static String getSecondaryExternalStorageState() {
		if (mDeviceList==null) rescanDevices();
		if (mSecondary==null) 
			return null;
		else {
			if (mSecondary.mAvailable)
				return mSecondary.mWriteable ? Environment.MEDIA_MOUNTED : Environment.MEDIA_MOUNTED_READ_ONLY;
			else 
				return Environment.MEDIA_REMOVED;
		}
	}

	
	/**
	 * Zeigt an, ob die Zweit-SD entfernt werden kann; derzeit kenne ich kein Gerät, bei dem die fest eingebaut wäre, also immer true
	 * @return true, auch wenn gar keine Karte vorhanden ist
	 */
	public final static boolean isSecondaryExternalStorageRemovable() {
		return true;
	}
	
	
	/**
	 * Fragt ab, ob die Zweit-SD vorhanden ist
	 * @return true, wenn eine Zweit-SD vorhanden ist, false wenn nicht eingelegt oder kein Slot vorhanden
	 */
	public static boolean isSecondaryExternalStorageAvailable() {
		if (mDeviceList==null) rescanDevices();
		return mSecondary!=null && mSecondary.mAvailable;
	}

	
	/**
	 * Ein Zeiger auf die Zweit-SD, falls gefunden
	 * @return null, falls keine gefunden, sonst das File. 
	 */
	public static File getSecondaryExternalStorageDirectory() {
		if (isSecondaryExternalStorageAvailable()) return mSecondary.getFile(); else return null;
	}
	

	/**
	 * interne Routine ohne Fehlerüberprüfung und mit Möglichkeit, den Pfad zu erstellen -- oder auch nicht
	 * @param s der Pfad, der an External angehängt wird, darf nicht null sein
	 * @param create Verzeichnis erzeugen oder nicht
	 * @return das angeforderte Verzeichnis
	 */
	private static File getSecondaryDirectoryLow(String s, boolean create) {
		File f = new File(mSecondary.mMountPoint+"/"+s);
		Log.v(TAG, "getLow "+f.getAbsolutePath()+" e:"+f.exists()+" d:"+f.isDirectory()+" w:"+f.canWrite() );
		if (create && !f.isDirectory() && mSecondary.mWriteable) 
			// erzeugen, falls es nicht existiert und Schreibzugriff auf die SD vorhanden
			f.mkdirs(); 
		return f;
	}

	
	/**
	 * Gibt die Public-Directories auf der Zweit-SD zurück; legt sie (wie die Environment-Methode) nicht an
	 * 
	 * @param s String aus Environment.DIRECTORY_xxx, darf nicht null sein. (Funktioniert auch mit
	 * 	anderen Pfadnamen und mit verschachtelten)
	 * 
	 * @return ein File dieses Verzeichnisses. Wenn Schreibzugriff gewährt, wird es angelegt, falls nicht vorhanden
	 */
	public static File getSecondaryExternalStoragePublicDirectory(String s) {
		if (isSecondaryExternalStorageAvailable()) { 
			if (s==null) throw new IllegalArgumentException("s darf nicht null sein");
			return getSecondaryDirectoryLow(s, false);
		} else 
			return null;
	}
	
	
	/**
	 * Nachbau der Context-Methode getExternalFilesDir(String) mit zwei Unterschieden:
	 *		1. man muss halt Context übergeben
	 *		2. das Verzeichnis wird bei der App-Deinstallation nicht gelöscht
	 *
	 * @param context Context der App; benötigt, um den Pfadnamen auszulesen
	 * @param s String aus Environment.DIRECTORY_xxx, kann aber auch ein anderer (verschachtelter) sein oder null
	 * @return null, falls keine Zweit-SD existiert, sonst das Verzeichnis. Wird angelegt, wenn man Schreibzugriff hat
	 */
	public static File getSecondaryExternalFilesDir(Context context, String s) {
		if (isSecondaryExternalStorageAvailable()) {
			if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
			String name = "/Android/data/" + context.getPackageName() + "/files";
			if (s!=null) name += "/" + s;
			return getSecondaryDirectoryLow(name, true);
		} else 
			return null;
	}
	
	
	public static File getSecondaryExternalCacheDir(Context context) {
		if (isSecondaryExternalStorageAvailable()) {
			if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
			String name = "/Android/data/" + context.getPackageName() + "/cache";
			return getSecondaryDirectoryLow(name, true);
		} else 
			return null;
	}
}
