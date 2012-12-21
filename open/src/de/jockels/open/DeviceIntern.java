package de.jockels.open;

import java.io.File;

import android.content.Context;
import android.os.Environment;

class DeviceIntern extends Device {
	/**
	 * liest Parameter aus {@link Environment#getDataDirectory() }, also
	 * i.Allg. /data
	 * @return this für Verkettungen wie {@code return new Device().initFromDataDirectory() } 
	 */
	DeviceIntern() {
		File f = Environment.getDataDirectory();
		mMountPoint = f.getAbsolutePath();
		mSize = Size.getSpace(f);
	}

	@Override
	public String getName() { return "intern"; }

	@Override
	public boolean isRemovable() { return false; }

	@Override
	public boolean isAvailable() { return true; }

	@Override
	public boolean isWriteable() { return true; }

	@Override 
	public File getFilesDir(Context ctx) { return ctx.getFilesDir(); }
	
	@Override 
	public File getCacheDir(Context ctx) { return ctx.getCacheDir(); }

	@Override public File getFilesDir(Context ctx, String s) {
		if (s==null)
			return getFilesDir(ctx);
		else {
			File f = new File(ctx.getFilesDir(), s);
			f.mkdir();
			return f;
		}
	}

	@Override 
	public File getPublicDirectory(String s) { return null; }

	@Override
	public String getState() { return Environment.MEDIA_MOUNTED; }

}
