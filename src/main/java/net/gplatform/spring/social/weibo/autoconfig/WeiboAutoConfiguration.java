package net.gplatform.spring.social.weibo.autoconfig;

import javax.sql.DataSource;

import net.gplatform.spring.social.weibo.api.Weibo;
import net.gplatform.spring.social.weibo.connect.WeiboConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.social.SocialWebAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.web.GenericConnectionStatusView;
import org.springframework.web.servlet.View;

@Configuration
@ConditionalOnClass(value = { SocialConfigurerAdapter.class, WeiboConnectionFactory.class })
@ConditionalOnProperty(prefix = "spring.social.weibo.", value = "app-id")
@AutoConfigureBefore(SocialWebAutoConfiguration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class WeiboAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	@ConditionalOnClass(value = { SocialConfigurerAdapter.class, WeiboConnectionFactory.class })
	protected static class WeiboAutoConfigurationAdapter extends SocialConfigurerAdapter implements EnvironmentAware {
		private static final Logger LOG = LoggerFactory.getLogger(WeiboAutoConfigurationAdapter.class);
		private RelaxedPropertyResolver properties;

		@Autowired(required = false)
		DataSource dataSource;

		@Override
		public void addConnectionFactories(ConnectionFactoryConfigurer configurer, Environment environment) {
			configurer.addConnectionFactory(createConnectionFactory(this.properties));
		}

		@Override
		public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
			if (dataSource == null) {
				return super.getUsersConnectionRepository(connectionFactoryLocator);
			} else {
				JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
				try{
					jdbcTemplate.queryForList("select count(*) from UserConnection");
				}catch(Exception e){
					LOG.debug("Create table UserConnection");
					try {
						ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
						rdp.addScript(new ClassPathResource("/org/springframework/social/connect/jdbc/JdbcUsersConnectionRepository.sql"));
						rdp.populate(dataSource.getConnection());
					} catch (Exception e1) {
						LOG.error("Error create table UserConnection", e1);
					}
				}
				TextEncryptor textEncryptor = Encryptors.noOpText();
				JdbcUsersConnectionRepository jdbcUsersConnectionRepository = new JdbcUsersConnectionRepository(dataSource, connectionFactoryLocator,
						textEncryptor);
				
				return jdbcUsersConnectionRepository;
			}
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.properties = new RelaxedPropertyResolver(environment, getPropertyPrefix());
		}

		protected String getPropertyPrefix() {
			return "spring.social.weibo.";
		}

		protected ConnectionFactory<?> createConnectionFactory(RelaxedPropertyResolver properties) {
			try {
				ConnectionFactory<Weibo> factory = new WeiboConnectionFactory(properties.getRequiredProperty("app-id"),
						properties.getRequiredProperty("app-secret"));
				return factory;
			} catch (Exception e) {
				LOG.error("Error when createConnectionFactory", e);
				return null;
			}
		}

		@Bean
		@ConditionalOnMissingBean(Weibo.class)
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public Weibo weibo(ConnectionRepository repository) {
			try {
				Connection<Weibo> connection = repository.findPrimaryConnection(Weibo.class);
				return connection != null ? connection.getApi() : null;
			} catch (Exception e) {
				LOG.error("Error when create weibo api", e);
				return null;
			}
		}

		@Bean(name = { "connect/weiboConnect", "connect/weiboConnected" })
		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
		public View weiboConnectView() {
			return new GenericConnectionStatusView("weibo", "Weibo");
		}

	}
}