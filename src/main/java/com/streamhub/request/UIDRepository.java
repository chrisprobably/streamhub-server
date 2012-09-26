package com.streamhub.request;

import com.streamhub.Connection;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;

interface UIDRepository {
	PublishListener findOrCreatePublishListener(String uid, Connection connection);
	SubscriptionListener findOrCreateSubscriptionListener(String uid, Connection connection);
}
