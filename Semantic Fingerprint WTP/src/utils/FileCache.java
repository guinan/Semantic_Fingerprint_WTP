package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Stores cached data to a file.
 * Notice: The cache will grow steadily because there is no tidy mechanism until now.
 * @author Christian Nywelt
 *
 * @param <T>
 */
public class FileCache <T extends Serializable> {
	public final String cacheName;
	public final String cacheFolder;
	private static final String cacheTablename = "FileCache.EntryMap";

	/**
	 * 
	 * @param name
	 */
	public FileCache(String cacheName) {
		String appFolder;
		try {
			appFolder = new File(".").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			appFolder = "";
		}
		// initalize
		this.cacheName = cacheName;
		cacheFolder = appFolder	+ File.separator + "caches" + File.separator + this.cacheName + File.separator;
		// load cache
		Object o = loadObject(cacheTablename);
		if (o == null) {
			entries = new HashMap<String, CacheEntry<T>>();
		} else {
			entries = (HashMap<String, CacheEntry<T>>) o; // TODO: on type error erase all cache content
		}
	}

	/**
	 * 
	 * @author Chris
	 * 
	 */
	private static class CacheEntry <T> implements Serializable {
		public int expires;
		public long filesize;
		public T value;
	}

	/**
	 * Key -> Value
	 */
	private HashMap<String, CacheEntry<T>> entries;

	/**
	 * 
	 * @param key
	 * @return
	 */
	public T get(String key) {
		CacheEntry<T> e = entries.get(key);
		if (e == null) { // TODO: check expire date
			return null;
		} else {
			return e.value;
		}
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, T value) {
		CacheEntry<T> e = entries.get(key);
		if (e == null) e = new CacheEntry<T>();
		e.value = value;
		// save to index
		entries.put(key, e);
		// TODO: persist object and entry seperately
		//persistObject(key, e);
		//e.filesize = new File(cacheFolder + key + ".ser").length();
		// persist index
		persistObject(cacheTablename, entries);
	}

	/**
	 * Writes an object to the cache folder
	 * 
	 * @param o
	 * @param fileName
	 */
	protected void persistObject(String key, Object obj) {
		String fileName = cacheFolder + key + ".ser";
		try {
			// create file if not exists
			File f = new File(fileName);
			if(!f.exists()) {
			    f.getParentFile().mkdirs();
			    f.createNewFile();
			}
			// write object to it
			FileOutputStream fileOut = new FileOutputStream(fileName, false);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
	}

	/**
	 * 
	 * @param fileName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Object loadObject(String key) {
		Object obj = null;
		String fileName = cacheFolder + key + ".ser";
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			obj = in.readObject();
			// close files
			in.close();
			fileIn.close();
		} catch (IOException i) {
			//i.printStackTrace();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		}
		return obj;
	}

}
