package com.streamhub.handler;

import com.streamhub.Connection;

public interface Handler {
	void handle(Connection connection) throws Exception;
}
