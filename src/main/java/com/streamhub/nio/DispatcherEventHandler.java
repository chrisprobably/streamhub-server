package com.streamhub.nio;

import com.streamhub.Connection;
import com.streamhub.handler.Handler;

class DispatcherEventHandler {
	private final Handler handler;

	public DispatcherEventHandler(Handler handler) {
		this.handler = handler;
	}

	void onReadableEvent(Connection connection) {
		try {
			handler.handle(connection);
		} catch (Exception e) {
		}
	}

	void onWriteableEvent(Connection con) {
	}
}
