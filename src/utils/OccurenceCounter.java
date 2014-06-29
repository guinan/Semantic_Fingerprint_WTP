package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class OccurenceCounter<T> {
	protected final HashMap<T, Integer> counter = new HashMap<T, Integer>();
	
	/**
	 * 
	 * @param key
	 */
	public void inc(T key) {
		Integer num = counter.get(key);
		if (num == null) {
			num = new Integer(1);
		} else {
			num++;
		}
		counter.put(key, num);
	}
	
	/**
	 * 
	 * @return
	 */
	public HashMap<T, Integer> getOccurences() {
		return counter;
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<T> getSortedKeysByOccurence() {
		ArrayList<T> keys = new ArrayList<T>(counter.size());
		keys.addAll(counter.keySet());
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
		if (!counter.isEmpty()) {
			ArrayList<T> sortedKeys = getSortedKeysByOccurence();
			for (T key : sortedKeys) {
				st.append(key + ": " + counter.get(key) + ", ");
			}
			st.setLength(st.length()-2);
		}
		st.append("}");
		return st.toString();
	}

	/**
	 * 
	 * @return
	 */
	public Set<Map.Entry<T, Integer>> entrySet() {
		return counter.entrySet();
	}
}
