package com.streamhub.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CircularFIFOMap<K, V> implements Map<K, V> {
	private final int maxSize;
	private int size;
	private final Map<K, V> map = new HashMap<K, V>();
	private final Queue<K> keys = new LinkedList<K>();

	public CircularFIFOMap(int maxSize) {
		this.maxSize = maxSize;
	}

	public void clear() {
		map.clear();
		size = 0;
		keys.clear();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public V get(Object key) {
		return map.get(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public V put(K key, V value) {
		V result = null;
		keys.offer(key);

		if (size+1 > maxSize) {
			result = map.remove(keys.remove());
		} else {
			size++;
		}
		
		map.put(key, value);
		return result;
	}

	public void putAll(Map<? extends K, ? extends V> t) {
		for (Map.Entry<? extends K, ? extends V> entry : t.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}
	}

	public V remove(Object key) {
		V removedElement = map.remove(key);
		if (removedElement != null) {
			size--;
			keys.remove(key);
		}
		return removedElement;
	}

	public int size() {
		return map.size();
	}

	public Collection<V> values() {
		return map.values();
	}

}
