/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.gplatform.spring.social.weibo.connect;

import net.gplatform.spring.social.weibo.api.Weibo;
import net.gplatform.spring.social.weibo.api.impl.WeiboTemplate;

import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;

public class WeiboServiceProvider extends AbstractOAuth2ServiceProvider<Weibo> {

	public WeiboServiceProvider(String clientId, String clientSecret) {
		super(getOAuth2Template(clientId, clientSecret));
	}

	private static OAuth2Template getOAuth2Template(String clientId, String clientSecret) {
		OAuth2Template oAuth2Template = new WeiboOAuth2Template(clientId, clientSecret, "https://api.weibo.com/oauth2/authorize",
				"https://api.weibo.com/oauth2/access_token");
		oAuth2Template.setUseParametersForClientAuthentication(true);
		return oAuth2Template;
	}

	public Weibo getApi(String accessToken) {
		return new WeiboTemplate(accessToken);
	}

}
