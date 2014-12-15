package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.Servlet30WrapperFilter;
import org.springframework.cloud.netflix.zuul.filters.route.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.http.ZuulServlet;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties(ZuulProperties.class)
@ConditionalOnClass(ZuulServlet.class)
@ConditionalOnExpression("${zuul.enabled:true}")
public class ZuulConfiguration {

	@Autowired(required = false)
	private TraceRepository traces;

	@Autowired
	private SpringClientFactory clientFactory;

	@Autowired
	private DiscoveryClient discovery;

	@Autowired
	private ZuulProperties zuulProperties;

	@Bean
	public ProxyRouteLocator routes() {
		return new ProxyRouteLocator(discovery, zuulProperties);
	}

	@Bean
	public ZuulController zuulController() {
		return new ZuulController();
	}

	@Bean
	@RefreshScope
	public ZuulHandlerMapping zuulHandlerMapping() {
		return new ZuulHandlerMapping(routes(), zuulController());
	}

	@Configuration
	protected static class ZuulFilterConfiguration {

		@Autowired
		private Map<String, ZuulFilter> filters;

		@Bean
		public FilterInitializer zuulFilterInitializer() {
			return new FilterInitializer(filters);
		}

	}

	@Bean
	public RoutesEndpoint zuulEndpoint() {
		return new RoutesEndpoint(zuulHandlerMapping());
	}

	// pre filters
	@Bean
	public DebugFilter debugFilter() {
		return new DebugFilter();
	}

	@Bean
	public PreDecorationFilter preDecorationFilter() {
		return new PreDecorationFilter(routes(), zuulProperties);
	}

	@Bean
	public Servlet30WrapperFilter servlet30WrapperFilter() {
		return new Servlet30WrapperFilter();
	}

	// route filters
	@Bean
	public RibbonRoutingFilter ribbonRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (traces != null) {
			helper.setTraces(traces);
		}
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper , clientFactory);
		return filter;
	}

	@Bean
	public SimpleHostRoutingFilter simpleHostRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (traces != null) {
			helper.setTraces(traces);
		}
		return new SimpleHostRoutingFilter(helper);
	}

	// post filters
	@Bean
	public SendResponseFilter sendResponseFilter() {
		return new SendResponseFilter();
	}

	@Bean
	public SendErrorFilter sendErrorFilter() {
		return new SendErrorFilter();
	}
}
