package com.deloittetraining.aem.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

@Component(service = Servlet.class, property = {
		Constants.SERVICE_DESCRIPTION + "=AEM Assets Training Servlet for Content Fragment Management",
		ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ServletResolverConstants.DEFAULT_RESOURCE_TYPE,
		ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
		ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + "json",
		ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + "createemailtemplate" })
public class ContentFragmentManagerServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 1L;

	final static Logger LOG = LoggerFactory.getLogger(ContentFragmentManagerServlet.class);

	private static final String TEMPLATELOCATION = "/content/dam/deloittetraining/email-templates/deloitte-training-cf";

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		ResourceResolver resolver = request.getResourceResolver();
		ArrayList<JSONObject> finalObj = new ArrayList<>();

		if (null == resolver) {
			generateResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Internal server error occured. Please try again.", response);
			return;
		}

		Resource templateResource = resolver.getResource(TEMPLATELOCATION);

		if (null != templateResource) {
			ContentFragment templateFragment = templateResource.adaptTo(ContentFragment.class);
			String fromAddress = templateFragment.hasElement("fromAddress")
					? templateFragment.getElement("fromAddress").getContent()
					: "Anonymous";
			String recipientsStr = templateFragment.hasElement("recipients")
					? templateFragment.getElement("recipients").getContent()
					: "";
			LOG.info("recipients: {}", recipientsStr);
			String[] recipientsArr = StringUtils.contains(recipientsStr, "\n") ? StringUtils.split(recipientsStr, "\n")
					: new String[0];
			LOG.info("Recipient length: {}", recipientsArr.length);
			String topic = templateFragment.hasElement("topic") ? templateFragment.getElement("topic").getContent()
					: "General Topic";
			String message = templateFragment.hasElement("message")
					? templateFragment.getElement("message").getContent()
					: "No Message";
			String subject = templateFragment.hasElement("subject")
					? templateFragment.getElement("subject").getContent()
					: "No Subject";
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

			Map<String, String> values = new HashMap<>();
			values.put("fromAddress", fromAddress);
			values.put("date", sdf.format(new Date().getTime()));
			values.put("topic", resolveTag(topic, resolver));

			for (String recipient : recipientsArr) {
				JSONObject obj = new JSONObject();
				values.put("recipient", recipient);
				try {
					obj.put("From", populateValues(fromAddress, values));
					obj.put("To", populateValues(recipient, values));
					obj.put("Subject", populateValues(subject, values));
					obj.put("Message", populateValues(message, values));
					finalObj.add(obj);
				} catch (JSONException e) {
					LOG.error("Error occured while adding data to JSON Object: ", e);
				}
			}

			generateResponse(HttpServletResponse.SC_OK, Arrays.toString(finalObj.toArray()), response);

		}

	}

	private String resolveTag(String topic, ResourceResolver resolver) {
		TagManager tm = resolver.adaptTo(TagManager.class);
		Tag tag = tm.resolve(topic);
		return null != tag ? tag.getTitle() : topic;
	}

	private String populateValues(String template, Map<String, String> values) {
		for (String key : values.keySet()) {
			template = StringUtils.replace(template, "{{".concat(key.concat("}}")), values.get(key));
		}
		return template;
	}

	private void generateResponse(int status, String message, HttpServletResponse response) throws IOException {
		PrintWriter writer = response.getWriter();
		response.setStatus(status);
		writer.write(message);
		writer.flush();
	}

}
