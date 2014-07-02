package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public final class OccurenceCounter<T> extends HashMap<T, Integer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param key
	 */
	public void inc(T key) {
		Integer num = this.get(key);
		if (num == null) {
			num = new Integer(1);
		} else {
			num++;
		}
		this.put(key, num);
	}
	
	@Override
	public Integer get(Object key) {
		Integer n = super.get(key);
		if (n == null) return 0;
		else return n;
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<T> getSortedKeysByOccurence() {
		final ArrayList<T> keys = new ArrayList<T>(this.size());
		keys.addAll(this.keySet());
		final OccurenceCounter<T> counter = this;
		Collections.sort(keys, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return counter.get(o1).compareTo(counter.get(o2));
			}
		});
		return keys;
	}
	
	@Override
	public String toString() {
		StringBuilder st = new StringBuilder("{");
		if (!this.isEmpty()) {
			ArrayList<T> sortedKeys = getSortedKeysByOccurence();
			for (T key : sortedKeys) {
				st.append(key + ": " + this.get(key) + ", ");
			}
			st.setLength(st.length()-2);
		}
		st.append("}");
		return st.toString();
	}
}
