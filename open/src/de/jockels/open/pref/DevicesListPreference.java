package de.jockels.open.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import de.jockels.open.Device;
import de.jockels.open.Environment2;

/**
 * PreferenceScreen-Element, das die Auswahl der verfügbaren Speicherorte erlaubt. 
 * Es benutzt dazu die von {@link Environment2#getDevices(String, boolean, boolean, boolean)}
 * gelieferte Liste. Die Parameter kann man über die XML-Datei steuern, und zwar mit folgenden
 * Variablen:
 * <ul>
 * <li>devices_key: ein String als Filter für die Liste ("USB")
 * <li>devices_available: bei 1 werden nur verfügbare Devies gelistet, bei 0 auch z.B. leere
 * 	SD-Schächte und nicht angebundene USB-Geräte
 * <li>devices_intern: bei 1 wird die primäre SD-Karte mit angeboten, bei 0 nur weitere 
 * 	Geräte. 0 macht nur Sinn, wenn man USB-Geräte zur Auswahl bereitstellen möchte.
 * <li>devices_data: bei 1 wird der interne Speicher (/data) mit angeboten. Vorsicht, dann klappt
 * 	{@link Device#getPublicDirectory(String)} nicht.
 * </ul>
 * Als Default-Wert für android:defaultValue sind gültig: 0=interner Speicher (/data), 1=primäre
 * SD (meist /mnt/sdcard), 2=sekundäre SD-Karte falls vorhanden, sonst primäre
 * 
 * Wie die Liste der Geräte dann optisch dargestellt wird, kann in einem Erben
 * geändert werden, und zwar durch Überladen von createEntry.
 * 
 * Gespeichert wird ein String, der {@link Device#getMountPoint()}. Das kann in
 * einem Erben per Überladen von createEntryValue geändert werden.
 * 
 * Den Pfad kann man über die normale {@link SharedPreferences#getString(String, String)}
 * auslesen, doch das liefert nur einen String. Besser geht es über die statische Methode
 * {@link #getDevice(Context, SharedPreferences, String)}, die direkt ein {@link Device}
 * liefert, dessen Methoden wie {@link Device#getFilesDir(Context)} man benutzen kann.
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
	
	public DevicesListPreference(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		
		String key = attrs.getAttributeValue(null, "devices_key");
		boolean available = attrs.getAttributeBooleanValue(null, "devices_available", true);
		boolean intern = attrs.getAttributeBooleanValue(null, "devices_intern", true);
		boolean data = attrs.getAttributeBooleanValue(null, "devices_data", false);
		
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
		if (n==null) {
			// Wert in Konfigurationsdatei nicht gefunden und kein Default-Wert
			if (Environment2.getPrimaryExternalStorage().isAvailable())
				return Environment2.getPrimaryExternalStorage();
			else
				return Environment2.getInternalStorage();
		} else {
			// Wert in Devices-Tabelle suchen
			Device[] d = Environment2.getDevices(null, false, true, true);
			for (Device i : d) { if (n.startsWith(i.getMountPoint())) return i; }
			Log.i(TAG, "didn't find mount point "+n);
			return null;
		}
	}
	

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		String s = a.getString(index);
		if ("1".equals(s)) // primäre/interne SD
			return Environment2.getPrimaryExternalStorage().getMountPoint();
		else if ("2".equals(s)) // sekundäre SD falls vorhanden
			return Environment2.getCardDirectory().getAbsolutePath(); 
		else // "0" oder anderer Wert oder existiert nicht
			return Environment2.getInternalStorage().getMountPoint();
	}


	protected String f(long l) {return Formatter.formatShortFileSize(getContext(), l);}
	
	
	protected String createEntry(Device d) {
		return d.getName() + (d.isAvailable() ? 
						"\n\t" + (f( d.isRemovable() ? d.getSize().guessSize() : d.getSize().second ) + " / " + f( d.getSize().first) + " frei")
					:	" (fehlt)");
	}
	
	
	protected String createEntryValue(Device d) {
		return d.getMountPoint();
	}
}
