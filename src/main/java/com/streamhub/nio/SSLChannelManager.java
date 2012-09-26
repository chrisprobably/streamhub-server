package com.streamhub.nio;

import java.util.HashSet;
import java.util.Set;

class SSLChannelManager {
	private final Set<SSLChannel> readListeners = new HashSet<SSLChannel>();
	private final Set<SSLChannel> writeListeners = new HashSet<SSLChannel>();

	public void fireEvents() {
		while (!readListeners.isEmpty() || !writeListeners.isEmpty()) {
			SSLChannel[] sc;
			if (!readListeners.isEmpty()) {
				sc = (SSLChannel[])readListeners.toArray(
						new SSLChannel[readListeners.size()]);
				readListeners.clear();
				for (int i = 0; i < sc.length; i++) {
					sc[i].fireReadEvent();
				}
			}
			
			if (!writeListeners.isEmpty()) {
				sc = (SSLChannel[])writeListeners.toArray(
						new SSLChannel[writeListeners.size()]);
				writeListeners.clear();
				for (int i = 0; i < sc.length; i++) {
					sc[i].fireWriteEvent();
				}
			}
		}
	}
		
	public void registerForRead(SSLChannel l) {
		readListeners.add(l);
	}
	
	public void unregisterForRead(SSLChannel l) {
		readListeners.remove(l);
	}
		
	public void registerForWrite(SSLChannel l) {
		writeListeners.add(l);
	}
	
	public void unregisterForWrite(SSLChannel l) {
		writeListeners.remove(l);
	}
}
