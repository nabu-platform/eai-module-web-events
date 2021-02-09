package be.nabu.eai.module.web.events;

import java.net.InetSocketAddress;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeUtils;

// for actual events: add metadata, at the very least an id!!
// clients will likely receive the same event multiple times (from different servers) if sticky sessions are not enabled
// in that case, the client needs to know if it has been handled already (it has to keep a list)

// have backend only events: no one in frontend sends them
// all events are backend only?
public class WebEventEnricher implements EventEnricher {

	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
			Pipeline pipeline = PipelineUtils.getPipeline();
			SourceContext sourceContext = pipeline == null ? null : pipeline.getSourceContext();
			if (sourceContext != null) {
				String ip = sourceContext.getSocketAddress() instanceof InetSocketAddress ? ((InetSocketAddress) sourceContext.getSocketAddress()).getAddress().getHostAddress() : null;
				if (ip != null) {
					ModifiablePart content = ((HTTPRequest) currentRequest).getContent();
					if (content != null) {
						Header header = MimeUtils.getHeader("Event-Session-Id", content.getHeaders());
						if (header != null) {
							String value = header.getValue();
							// Example: 20210207T201641440Z
							if (value.matches("^[0-9]{8}T[0-9]{9}Z$")) {
								if (!(object instanceof ComplexContent)) {
									object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
								}
								if (object != null) {
									// if we have a field called "sessionId", we enrich it
									if (((ComplexContent) object).getType().get("sessionId") != null) {
										Object current = ((ComplexContent) object).get("sessionId");
										if (current == null) {
											((ComplexContent) object).set("sessionId", value + "@" + ip);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

}
