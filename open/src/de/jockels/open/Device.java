package de.jockels.open;

import java.io.File;

import android.content.Context;
import android.os.Environment;

/**
 * Hilfsklasse zur Beschreibung eines Devices, womit MountPoints gemeint sind, also
 * unter anderem Partitionen des internen Gerätespeichers, aber auch SD-Karten oder
 * per USB ankoppelbare Geräte. Wird nur von {@link Environment2} benutzt und hat
 * daher keine public constructors. Kann aber für manche Zwecke genutzt werden,
 * daher einige public methods.
 * 
 * Die konkrete Implementierung findet seit Version 1.2 in den Klassen 
 * {@link DeviceIntern} (/data), {@link DeviceExternal} (/mnt/sdcard) und
 * {@link DeviceDiv} (weitere SD-Karten, USB-Geräte) statt.
 * 
 * @see Environment2
 * @see Size
 * @author Jörg Wirtgen (jow@ct.de)
 * @version 1.3
 * 
 */
public abstract class Device  {
	protected Size mSize;
	protected String mMountPoint;
	
	// Zugriff auf interne Felder -------------------------------------------------------------------
	public final File getFile() { return new File(mMountPoint); }
	public final Size getSize() { return mSize; }
	public final String getMountPoint() { return mMountPoint; }
	public abstract String getName();
	public abstract boolean isRemovable();
	public abstract boolean isAvailable();
	public abstract boolean isWriteable();

	/**
	 * sollte die Verfügbarkeit des Devices erneuern; wird vom BroadcastReceiver
	 * von Environment2 aufgerufen.
	 * @since 1.3
	 */
	protected void updateState() {}
	
	
	/**
	 * Liefert analog zu Context.getXXXFilesDir ein Datenverzeichnis auf
	 * diesem Gerät zurück und erzeugt das Verzeichnis bei Bedarf.. 
	 * @param ctx der Context der App
	 * @return ein File, das auf einen für App-Daten nutzbaren Pfad auf dem
	 * 	Device zeigt. Wenn es sich um den internen oder primären Speicher
	 * 	handelt, kommen die Methoden von {@link Context} zum Einsatz, beim
	 * 	sekundären Speicher und sonstigen Devices (USB) ein dem 
	 * 	nachempfundener Pfad. Falls null kommt, könnte die Permission
	 * 	android.permission.WRITE_EXTERNAL_STORAGE fehlen.
	 * @since 1.2
	 */
	public abstract File getFilesDir(Context ctx);
	public abstract File getFilesDir(Context ctx, String s);
	public abstract File getCacheDir(Context ctx);
	
	/**
	 * Liefert analog zu {@link Environment#getExternalStoragePublicDirectory(String)}
	 * ein Datenverzeichnis auf diesem Gerät zurück
	 * @param s der Name des Unterverzeichnis als String, wobei die 
	 * Environment-Konstanten wie {@link Environment#DIRECTORY_DOWNLOADS}
	 * auch funktionieren. Kann null sein, entspricht dann getMountPoint()
	 * @return ein File, das auf das angeforderte Verzeichnis zeigt. Es wird wie die 
	 * 	Environment-Methode nicht erzeugt. null, falls Device auf den internen
	 * 	Speicher (/data) zeigt.
	 * @since 1.2
	 */
	public abstract File getPublicDirectory(String s);
	
	/**
	 * Liefert analog zu {@link Environment#getExternalStorageState()} zurück,
	 * ob das Device gemountet (lesbar) und beschreibbar ist. Alternativ kann man
	 * isWriteable() und isAvailable() nutzen.
	 * 
	 * @return einen String mit Konstanten Environment.MEDIA_xxx, wobei
	 * 	der interne Speicher immer {@link Environment#MEDIA_MOUNTED} und
	 * 	die Zusatzspeicher immer das, {@link Environment#MEDIA_MOUNTED_READ_ONLY}
	 * 	oder {@link Environment#MEDIA_UNMOUNTED} liefern, keine
	 * 	detaillierteren Informationen.
	 * @since 1.2
	 */
	public abstract String getState(); 

	
	/**
	 * Hilfsmethode zum Emulieren der getXXXDir-Methoden von {@link Context},
	 * die einerseits nicht für API7 vorhanden sind, andererseits nicht für die 
	 * gemounteten USB- und Secondary-Medien. Wird also von Methoden in
	 * den Erben {@link DeviceDiv} und {@link DeviceIntern} genutzt.
	 * 
	 * @param ctx der Context (für {@link Context#getPackageName()})
	 * @param s ein String, der den Pfadnamen innerhalb des App-Pfads beschreibt
	 * @return ein File mit dem gewünschten Unterverzeichnis; falls nicht
	 * 	vorhanden, wird das Verzeichnis angelegt
	 */
	protected File getFilesDirLow(Context ctx, String s) {
		if (s!=null && !s.startsWith("/")) s = "/" + s;
		File f = new File(getMountPoint() + Environment2.PATH_PREFIX + ctx.getPackageName() + s);
		if (!f.isDirectory() && isWriteable()) 
			f.mkdirs(); 
		return f;
	}


}

