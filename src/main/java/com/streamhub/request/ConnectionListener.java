package com.streamhub.request;

import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;

interface ConnectionListener {
	void connectionLost(SubscriptionListener subscriptionListener);
	void connectionLost(PublishListener publishListener);
}
