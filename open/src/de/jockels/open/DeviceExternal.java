package de.jockels.open;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

class DeviceExternal extends Device {
	private boolean mRemovable; 
	private String mState;
	
	/**
	 * liest Parameter aus {@link Environment#getExternalStorageDirectory()},
	 * also i.Allg. /mnt/data
	 * @return this f¸r Verkettungen wie {@code return new Device().initFromExternalStorageDirectory() } 
	 */
	@SuppressLint("NewApi") 
	DeviceExternal() {
		File f = Environment.getExternalStorageDirectory();
		mMountPoint = f.getAbsolutePath();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) 
    		mRemovable = Environment.isExternalStorageRemovable(); // Gingerbread weiﬂ es genau
		else
			mRemovable = false; // guess, wird ggf. sp‰ter korrigiert

		updateState();
	}

	
	@Override
	protected void updateState() {
		mState = Environment.getExternalStorageState();
		if (isAvailable()) {
			File f = new File(mMountPoint);
			mSize = Size.getSpace(f);
		}
	}
	
	
	@Override
	public String getName() { return mRemovable ? "SD-Card" : "intern 2"; }

	@Override
	public boolean isRemovable() { return mRemovable; }

	@Override
	public boolean isAvailable() {
		return (Environment.MEDIA_MOUNTED.equals(mState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mState));
	}

	@Override
	public boolean isWriteable() {
		return Environment.MEDIA_MOUNTED.equals(mState);
	}

	protected final void setRemovable(boolean remove) { mRemovable = remove; }

	@Override
	public File getFilesDir(Context ctx) { return ctx.getExternalFilesDir(null); }

	@Override
	public File getFilesDir(Context ctx, String s) { return ctx.getExternalFilesDir(s); }
	
	@Override
	public File getCacheDir(Context ctx) { return ctx.getExternalCacheDir(); }

	@Override
	public File getPublicDirectory(String s) { return Environment.getExternalStoragePublicDirectory(s); }

	@Override
	public String getState() { return mState; }

}
