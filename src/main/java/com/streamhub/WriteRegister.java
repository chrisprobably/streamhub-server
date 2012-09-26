package com.streamhub;

public interface WriteRegister {
	void registerForWrite(Connection connection);
	void deregisterForWrite(Connection channel);
}
