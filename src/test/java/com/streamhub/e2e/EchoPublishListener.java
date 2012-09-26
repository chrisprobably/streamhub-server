package com.streamhub.e2e;

import java.util.Map;

import com.streamhub.api.Client;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PublishListener;
import com.streamhub.api.PushServer;

public class EchoPublishListener implements PublishListener {
	public static final String PUBLISH_ECHO_TOPIC = "publish.echo";
	private EchoMode mode = EchoMode.SEND;
	private PushServer server = null;
	
	public EchoPublishListener(EchoMode mode, PushServer server) {
		this.mode = mode;
		this.server = server;
	}
	
	public EchoPublishListener() {
	}

	public void onMessageReceived(Client client, String topic, Payload payload) {
		StringBuilder message = new StringBuilder();
		
		Map<String, String> fields = payload.getFields();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			message.append("key=").append(entry.getKey());
			message.append(" value=").append(entry.getValue()).append(" ");
		}
		
		message.append("topic=").append(topic);
		
		Payload echoPayload = new JsonPayload(PUBLISH_ECHO_TOPIC);
		echoPayload.addField("Name", "echo");
		echoPayload.addField("Last", message.toString());
		
		if (mode == EchoMode.SEND) {
			client.send(topic, echoPayload);
		} else {
			server.publish(topic, echoPayload);
		}
	}
}
