package de.jockels.open.pref;

import android.content.Context;
import android.preference.ListPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import de.jockels.open.Device;
import de.jockels.open.Environment2;

public class DevicesListPreference extends ListPreference {
	private static final String TAG ="DevicesListPreference";
	private static final boolean DEBUG = true;
	
	public DevicesListPreference(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);

		Device[] devices = Environment2.getDevices(null, false, true, true);
		CharSequence[] entries = new CharSequence[devices.length];
		CharSequence[] entryValues = new CharSequence[devices.length];
		for (int i=0; i<devices.length; i++) {
			entries[i] = createEntry(devices[i]);
			entryValues[i] = createEntryValue(devices[i]);
		}
		setEntries(entries);
		setEntryValues(entryValues);
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
