package com.streamhub.nio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamhub.handler.Handler;

class DispatcherPool {
	private final Dispatcher[] dispatchers;
	private int index = 0;
	private final int size;
	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	public DispatcherPool(Handler handler, int size) {
		this.size = size;
		dispatchers = new Dispatcher[size];
		
		for (int i = 0; i < size; i++) {
			dispatchers[i] = new Dispatcher(handler);
			threadPool.execute(dispatchers[i]);
		}
	}
	
	public synchronized Dispatcher nextDispatcher() {
		return dispatchers[(index++) % size];
	}
	
	public void stop() {
		for (int i = 0; i < dispatchers.length; i++) {
			Dispatcher dispatcher = dispatchers[i];
			dispatcher.stop();
		}
		threadPool.shutdownNow();
	}
}
