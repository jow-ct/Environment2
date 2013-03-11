package de.jockels.open;

import java.io.File;

import android.content.Context;
import android.os.Environment;


/**
 * Hilfsklasse zum Beschreiben von /data, Erbe von {@link Device}. 
 * 
 * Nutzt für {@link #getFilesDir(Context)} und {@link #getCacheDir(Context)} 
 * die Methoden von Context, hat aber für das dort nicht vorhandene 
 * {@link #getFilesDir(Context, String)} (also mit dem String-Parameter) eine 
 * Emulation. 
 * 
 * {@link #getPublicDirectory(String)} kann nicht emuliert werden, 
 * weil man auf /data ja keinen Zugriff hat; liefert also null. 
 * 
 * {@link #getState()} liefert MEDIA_MOUNTED.
 * 
 * {@link #getName()} liefert "intern".
 * 
 * @author Jockel
 * @version 1.1
 *
 */
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
