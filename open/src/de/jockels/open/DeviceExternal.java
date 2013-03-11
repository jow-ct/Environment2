package de.jockels.open;

import java.io.File;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

/**
 * Beschreibt die (primäre) SD-Karte in {@link Device}-Form. Interessant für 
 * API7 ist, dass die dort in {@link Context} nicht vorhandenen Methoden wie 
 * {@link Context#getExternalFilesDir(String)} emuliert werden.
 * 
 * TODO Die Erkennung, ob es sich um ein Gerät mit fester SD-Karte handelt,
 * findet nicht hier statt. {@link #isRemovable()} liefert also TRUE, zumindest
 * solange in {@link Environment2#rescanDevices()} kein Hack durchgeführt
 * wird.
 * 
 * @author Jockel
 * 
 * @version 1.2 - API7 wird unterstützt
 *
 */
class DeviceExternal extends Device {
	private boolean mRemovable; 
	private String mState;
	
	/**
	 * liest Parameter aus {@link Environment#getExternalStorageDirectory()},
	 * also i.Allg. /mnt/sdcard, bei API7 /sdcard. Das Device wird als 
	 * Removable gekennzeichnet, was der Normalfall ist. Eine 
	 * Erkennung, ob es sich um z.B. ein Nexus mit fester SD-Karte
	 * handelt, findet hier nicht statt.
	 * 
	 * @return this für Verkettungen wie {@code return new Device().initFromExternalStorageDirectory() } 
	 */
	@SuppressLint("NewApi") 
	DeviceExternal() {
		File f = Environment.getExternalStorageDirectory();
		mMountPoint = f.getAbsolutePath();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) 
    		setRemovable(Environment.isExternalStorageRemovable()); // Gingerbread weiß es genau
		else 
			setRemovable(true); // Default ist, dass eine SD-Karte rausgenommen werden kann
		
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

	protected final void setRemovable(boolean remove) { mRemovable = remove; }

	@Override
	public boolean isAvailable() {
		return (Environment.MEDIA_MOUNTED.equals(mState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mState));
	}

	@Override
	public boolean isWriteable() {
		return Environment.MEDIA_MOUNTED.equals(mState);
	}

	
	@Override
	public File getFilesDir(Context ctx) { return getFilesDir(ctx, null); }

	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	public File getFilesDir(Context ctx, String s) { 
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) 
			return ctx.getExternalFilesDir(s); 
		else 
			return getFilesDirLow(ctx, s);
	}

	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	public File getCacheDir(Context ctx) { 
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) 
			return ctx.getExternalCacheDir(); 
		else 
			return getFilesDirLow(ctx, "/cache");
	}

	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	public File getPublicDirectory(String s) { 
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) 
			return Environment.getExternalStoragePublicDirectory(s); 
		else {
			if (s!=null && !s.startsWith("/")) s = "/" + s;
			return new File(getMountPoint() + s);
		}
	}

	
	@Override
	public String getState() { return mState; }

}
