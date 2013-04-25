package de.jockels.open;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

/**
 * Ein {@link Device}, das ein speziell gemountetes Ger�t beschreibt, z.B.
 * die Secondary-SDs vieler moderner Ger�te und die USB-Ger�te bzw.
 * Kartenleser. Erkennt die Pfade aus vold.fstab (siehe {@link Environment2}
 * und emuliert die getXXXDir-Methoden, die sonst {@link Context} hat.
 * 
 * @author Jockel
 *
 */
class DeviceDiv extends Device {
	private String mLabel, mName;
	private boolean mAvailable, mWriteable;
	

	/**
	 * Constructor, der eine Zeile aus vold.fstab bekommt (dev_mount schon weggelesen)
	 * @param sp der StringSplitter, aus dem die Zeile gelesen wird, wobei
	 * 		"dev_mount" schon weggelesen sein muss.
	 * @return this f�r Verkettungen wie {@code return new Device().initFrom...() } 
	 */
	DeviceDiv(SimpleStringSplitter sp) {
		mLabel = sp.next().trim();
		mMountPoint = sp.next().trim();
		updateState();
	}
	
	@Override
	public boolean isAvailable() { return mAvailable; }

	@Override
	public boolean isWriteable() { return mWriteable; }

	@Override
	protected void updateState() {
		File f = new File(mMountPoint);
		
		//Workaround f�r speziellen vold.fstab Syntax von Motorola
		//Format: dev_mount <label> <mount_point[:[asec_point]:[lun_point]]> <part> <sysfs_path1...> 
		if(!(f.isDirectory() && f.canRead()) && mMountPoint.contains(":")) {
			String possibleMountPoint = mMountPoint.substring(0, mMountPoint.indexOf(":"));
			Log.v("DeviceDiv", "mp " + possibleMountPoint);
			File pf = new File(possibleMountPoint);
			if(pf.exists() && pf.isDirectory()) {
				mMountPoint = possibleMountPoint;
				f = pf;
			}
		}
		setName(f.getName()); // letzter Teil des Pfads
		if (mAvailable = f.isDirectory() && f.canRead()) { // ohne canRead() klappts z.B. beim Note2 nicht
			mSize = Size.getSpace(f); 
			mWriteable = f.canWrite();
			// Korrektur, falls in /mnt/sdcard gemountet (z.B. Samsung)
			if (mMountPoint.startsWith(Environment2.mPrimary.mMountPoint) && mSize.equals(Environment2.mPrimary.mSize)) 
				mAvailable = mWriteable = false;
		} else 
			mWriteable = false;
		
	}

	public final String getLabel() { return mLabel; }

	public String getName() { return mName; }
	protected final void setName(String name) { mName = name; }

	@Override
	public boolean isRemovable() { return true; }

	@Override
	public File getCacheDir(Context ctx) { return getFilesDirLow(ctx, "/cache"); }

	@Override
	public File getFilesDir(Context ctx) { return getFilesDirLow(ctx, "/files"); }
	
	@Override
	public File getFilesDir(Context ctx, String s) { return getFilesDirLow(ctx, s); }

	@Override
	public File getPublicDirectory(String s) { 
		if (s!=null && !s.startsWith("/")) s = "/" + s;
		return new File(getMountPoint() + s);
	}

	@Override
	public String getState() {
		if (mAvailable)
			return mWriteable ? Environment.MEDIA_MOUNTED : Environment.MEDIA_MOUNTED_READ_ONLY;
		else 
			return Environment.MEDIA_REMOVED;
	}

}
