package de.jockels.open.pref;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import de.jockels.open.Device;
import de.jockels.open.Environment2;

/**
 * PreferenceScreen-Element, das die Auswahl der verfügbaren Speicherorte erlaubt. 
 * Es benutzt dazu die von {@link Environment2#getDevices(String, boolean, boolean, boolean)}
 * gelieferte Liste. Die Parameter kann man über die XML-Datei steuern, und zwar mit den
 * Variablen devices_key, _available, _intern und _data. Und devices_apppath
 * 
 * Wie die Liste der Geräte dann optisch dargestellt wird, kann in einem Erben
 * geändert werden, und zwar durch Überladen von createEntry.
 * 
 * Gespeichert wird ein String, der {@link Device#getMountPoint()}. Dieser Pfad
 * ist nicht unbedingt direkt verwertbar: Beim internen Speicher zeigt er auf /data,
 * worauf man keine Schreibrechte besitzt. Abhilfe ist, im XML devices_apppath zu
 * setzen, dann wird ein Pfad nach anderem Muster erzeugt.
 * 
 * Einbinden dann etwa so: <pre>
       &lt;PreferenceCategory android:title="@string/cfg_files"&gt;
        &lt;de.jockels.open.pref.DevicesListPreference 
            android:key="filespath" 
            android:title="@string/cfg_filespath_title" 
            android:summary="@string/cfg_filespath_summary"
            devices_available="1"
            devices_intern="1"
            devices_data="1"
         /&gt;
 * </pre>
 * 
 * @author Jockel
 *
 */
public class DevicesListPreference extends ListPreference {
	private static final String TAG = "Device";
	
	boolean useAppPath; // MountPoint oder + /data...name... abspeichern
	
	public DevicesListPreference(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		
		String key = attrs.getAttributeValue(null, "devices_key");
		boolean available = attrs.getAttributeBooleanValue(null, "devices_available", true);
		boolean intern = attrs.getAttributeBooleanValue(null, "devices_intern", true);
		boolean data = attrs.getAttributeBooleanValue(null, "devices_data", false);
		useAppPath = attrs.getAttributeBooleanValue(null, "devices_apppath", false);
		
		Device[] devices = Environment2.getDevices(key, available, intern, data);
		CharSequence[] entries = new CharSequence[devices.length];
		CharSequence[] entryValues = new CharSequence[devices.length];
		for (int i=0; i<devices.length; i++) {
			entries[i] = createEntry(devices[i]);
			entryValues[i] = createEntryValue(devices[i]);
		}
		setEntries(entries);
		setEntryValues(entryValues);
	}

	
	public static Device getDevice(Context ctx, SharedPreferences cfg, String key) {
		String n = cfg.getString(key, null);
		Device[] d = Environment2.getDevices(null, false, true, true);
		Log.v(TAG, "gesucht: "+n);
		for (Device i : d) {
			Log.v(TAG, "- vielleicht "+i.getMountPoint()+": "+n.startsWith(i.getMountPoint()));
			if (n.startsWith(i.getMountPoint())) return i;
		}
		Log.v(TAG, "huch, nix gefunden...");
		return null;
	}
	
	protected String f(long l) {return Formatter.formatShortFileSize(getContext(), l);}
	
	protected String createEntry(Device d) {
		return d.getName() + (d.isAvailable() ? 
						"\n\t" + (f( d.isRemovable() ? d.getSize().guessSize() : d.getSize().second ) + " / " + f( d.getSize().first) + " frei")
					:	" (fehlt)");
	}
	
	protected String createEntryValue(Device d) {
		if (useAppPath) {
			File f = d.getFilesDir(getContext());
			if (f!=null) return f.getAbsolutePath();
		} 
		return d.getMountPoint();
	}
}
