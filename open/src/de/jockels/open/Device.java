package de.jockels.open;

import java.io.File;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils.SimpleStringSplitter;

/**
 * Hilfsklasse zur Beschreibung eines Devices, womit MountPoints gemeint sind, also
 * unter anderem Partitionen des internen Gerätespeichers, aber auch SD-Karten oder
 * per USB ankoppelbare Geräte. Wird nur von {@link Environment2} benutzt und hat
 * daher keine public constructors. Kann aber für manche Zwecke genutzt werden,
 * daher einige public methods.
 * 
 * @see Environment2
 * @see Size
 * @author Jörg Wirtgen (jow@ct.de)
 * 
 */
public class Device  {
	private Size mSize;
	private String mLabel, mMountPoint, mName;
	private boolean mRemovable, mAvailable, mWriteable;

	protected Device() {}
	
	
	/**
	 * liest Parameter aus {@link Environment#getDataDirectory() }, also
	 * i.Allg. /data
	 * @return this für Verkettungen wie {@code return new Device().initFromDataDirectory() } 
	 */
	protected Device initFromDataDirectory() {
		File f = Environment.getDataDirectory();
		mLabel = mMountPoint = f.getAbsolutePath();
		mName = "intern";
		mRemovable = false;
		if (mAvailable = f.isDirectory()) {
			mWriteable = f.canWrite();
			mSize = Size.getSpace(f);
		}
		return this;
	}
	
	
	/**
	 * liest Parameter aus {@link Environment#getExternalStorageDirectory()},
	 * also i.Allg. /mnt/data
	 * @return this für Verkettungen wie {@code return new Device().initFromExternalStorageDirectory() } 
	 */
	@SuppressLint("NewApi") 
	protected Device initFromExternalStorageDirectory() {
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
		return this;
	}

	
	/**
	 * Constructor, der eine Zeile aus vold.fstab bekommt (dev_mount schon weggelesen)
	 * @param sp der StringSplitter, aus dem die Zeile gelesen wird, wobei
	 * 		"dev_mount" schon weggelesen sein muss.
	 * @return this für Verkettungen wie {@code return new Device().initFrom...() } 
	 */
	protected Device initFromStringSplitter(SimpleStringSplitter sp) {
		mRemovable = true;
		mLabel = sp.next().trim();
		mMountPoint = sp.next().trim();
		File f = new File(mMountPoint);
		mName = f.getName(); // letzter Teil des Pfads
		if (mAvailable = f.isDirectory() && f.canRead()) { // ohne canRead() klappts z.B. beim Note2 nicht
			mSize = Size.getSpace(f); 
			mWriteable = f.canWrite();
			// Korrektur, falls in /mnt/sdcard gemountet (z.B. Samsung)
			if (mMountPoint.startsWith(Environment2.mPrimary.mMountPoint) && mSize.equals(Environment2.mPrimary.mSize)) 
				mAvailable = mWriteable = false;
		} else 
			mWriteable = false;
		return this;
	}
	
	public final File getFile() { return new File(mMountPoint); }
	public final Size getSize() { return mSize; }
	public final String getLabel() { return mLabel; }
	public final String getMountPoint() { return mMountPoint; }
	public final String getName() { return mName; }
	public final boolean isRemovable() { return mRemovable; }
	public final boolean isAvailable() { return mAvailable; }
	public final boolean isWriteable() { return mWriteable; }
	
	protected final void setName(String name) { mName = name; }
	protected final void setRemovable(boolean remove) { mRemovable = remove; }
}
