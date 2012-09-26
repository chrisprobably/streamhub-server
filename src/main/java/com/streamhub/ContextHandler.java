package com.streamhub;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.streamhub.api.SubscriptionManager;
import com.streamhub.handler.Handler;
import com.streamhub.request.Request;
import com.streamhub.util.SocketUtils;
import com.streamhub.util.UrlUtils;

public class ContextHandler implements Handler {
	private static final String ROOT_CONTEXT = "/";
	private static final Logger log = Logger.getLogger(ContextHandler.class);
	private static final String CONNECTION_CLOSED = "Connection closed ";
	private final Handler httpHandler;
	final Map<String, Handler> contexts = new HashMap<String, Handler>();

	public ContextHandler(SubscriptionManager subscriptionManager) {
		httpHandler = new HttpHandler();
		contexts.put("/streamhubws", new WebSocketHandler(subscriptionManager, httpHandler));
		contexts.put("/streamhub", new HttpCometHandler(subscriptionManager, httpHandler));
		contexts.put(ROOT_CONTEXT, httpHandler);
	}

	public void addContext(String context, Handler handler) {
		contexts.put(UrlUtils.normalizeContext(context), handler);
	}

	public void handle(Connection connection) {
		Request request = null;

		try {
			request = connection.getRequest();
			handle(connection, request);
		} catch (Exception e) {
			log.error("Error handling request for connection " + SocketUtils.toString(connection), e);
		} finally {
			if (! request.isKeepAliveConnection() && ! connection.isSelfClosing() && ! request.isWebSocket()) {
				connection.close();
				log.debug(new StringBuilder(CONNECTION_CLOSED).append(SocketUtils.toString(connection)).toString());
			}
		}
	}

	private void handle(Connection connection, Request request) throws Exception {
		String context = request.getContext();
		if (contexts.containsKey(context)) {
			Handler handler = contexts.get(context);
			log.debug("Invoking handler [" + handler.getClass().getName() + "] for url [" + request.getUrl() + "]");
			handler.handle(connection);
		} else if (contexts.containsKey(ROOT_CONTEXT)) {
			Handler handler = contexts.get(ROOT_CONTEXT);
			log.debug("Invoking /* handler [" + handler.getClass().getName() + "] for url [" + request.getUrl() + "]");
			handler.handle(connection);
		} else {
			log.debug("Failing over to default handler for url [" + request.getUrl() + "]");
			httpHandler.handle(connection);
		}
	}
}
