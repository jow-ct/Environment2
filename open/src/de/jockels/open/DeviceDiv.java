package de.jockels.open;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils.SimpleStringSplitter;

class DeviceDiv extends Device {
	private String mLabel, mName;
	private boolean mAvailable, mWriteable;
	

	/**
	 * Constructor, der eine Zeile aus vold.fstab bekommt (dev_mount schon weggelesen)
	 * @param sp der StringSplitter, aus dem die Zeile gelesen wird, wobei
	 * 		"dev_mount" schon weggelesen sein muss.
	 * @return this für Verkettungen wie {@code return new Device().initFrom...() } 
	 */
	DeviceDiv(SimpleStringSplitter sp) {
		mLabel = sp.next().trim();
		mMountPoint = sp.next().trim();
		updateState();
	}
	
	@Override
	public boolean isAvailable() {
		updateState();
		return mAvailable;
	}

	@Override
	public boolean isWriteable() {
		updateState();
		return mWriteable;
	}

	
	private final void updateState() {
		File f = new File(mMountPoint);
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

	private File getFilesDirLow(Context ctx, String s) {
		if (s!=null && !s.startsWith("/")) s = "/" + s;
		File f = new File(getMountPoint() + Environment2.PATH_PREFIX + ctx.getPackageName() + s);
		if (!f.isDirectory() && isWriteable()) 
			f.mkdirs(); 
		return f;
	}

	@Override
	public File getPublicDirectory(String s) { 
		if (s!=null && !s.startsWith("/")) s = "/" + s;
		return new File(getMountPoint() + s);
	}

	@Override
	public String getState() {
		updateState();
		if (mAvailable)
			return mWriteable ? Environment.MEDIA_MOUNTED : Environment.MEDIA_MOUNTED_READ_ONLY;
		else 
			return Environment.MEDIA_REMOVED;
	}


}
