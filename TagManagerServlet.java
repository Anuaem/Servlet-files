package com.deloittetraining.aem.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessControlException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.AssetException;
import com.day.cq.tagging.InvalidTagFormatException;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/*
 * Purpose of this servlet is to illustrate a programmatic way of performing CRUD operation on tags in AEM.
 * Using this servlet one can do the following
 * - Create Tag
 * - Get tag details
 */

@Component(service = Servlet.class, property = {
    Constants.SERVICE_DESCRIPTION + "=AEM Assets Training Servlet for Asset Management",
    ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ServletResolverConstants.DEFAULT_RESOURCE_TYPE,
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + "json",
    ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + "deloittetrainingtm"
})
public class TagManagerServlet extends SlingSafeMethodsServlet {

  final static Logger LOG = LoggerFactory.getLogger(TagManagerServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String action = request.getParameter("action");
    String name = request.getParameter("name");

    ResourceResolver resolver = request.getResourceResolver();

    if (null != action && null != resolver) {
      TagManager tagManager = resolver.adaptTo(TagManager.class);
      switch (action) {
        case "createTag":
          if (null == name) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot process request. Tag name is not defined. (Example: ?action=createTag&name=Deloitte)",
                response);
            return;
          }
          try {
            Tag tag = tagManager.createTag("/content/cq:tags/default/"
                .concat(name.toLowerCase()), name, name, true);
            generateResponse(HttpServletResponse.SC_ACCEPTED, "Tag created at: ".concat(tag.getPath()), response);
            return;
          } catch (AccessControlException | InvalidTagFormatException e) {
            LOG.error("Error while creating tags: ", e);
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Error. Cannot create tag.",
                response);
            return;
          }
        case "getTag":
          if (null == name) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot process request. Tag name is not defined. (Example: ?action=getTag&name=deloitte)",
                response);
            return;
          }
          try {
            Tag getTag = tagManager.resolve(name);
            if (null != getTag) {
              generateResponse(HttpServletResponse.SC_ACCEPTED, "Got the tag: ".concat(getTag.getTitle()), response);
            } else {
              generateResponse(HttpServletResponse.SC_BAD_REQUEST, "Cannot find the tag you are looking for.",
                  response);
            }
            return;
          } catch (AssetException ex) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST, "Cannot delete asset. Asset might not exist!",
                response);
            return;
          }
        default:
          break;
      }
    } else {
      generateResponse(HttpServletResponse.SC_BAD_REQUEST,
          "Cannot process request. Action is not defined. (Example: ?action=createTag)", response);
      return;
    }

  }

  private void generateResponse(int status, String message, HttpServletResponse response) throws IOException {
    PrintWriter writer = response.getWriter();
    response.setStatus(status);
    writer.write(message);
    writer.flush();
  }

}
