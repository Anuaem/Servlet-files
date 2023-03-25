package com.deloittetraining.aem.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Purpose of this servlet is to illustrate a programmatic way of getting, setting and removing metadata from assets.
 * Using this servlet one can do the following
 * - Set Metadata
 * - Get Metadata
 * - Remove Metadata
 * - Get All Metadata
 */
//Decalring Servlet using @Component Annotation - STARTS
@Component(service = Servlet.class, property = {
    Constants.SERVICE_DESCRIPTION + "=AEM Assets Training Servlet for Metadata Management",
    ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ServletResolverConstants.DEFAULT_RESOURCE_TYPE,
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + "json",
    ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + "deloittetrainingmm"
})
// Decalring Servlet using @Component Annotation - ENDS
public class MetadataManagerServlet extends SlingSafeMethodsServlet {

  // Logger and constants to always be declared on top of the class for easy
  // maintainence - STARTS

  final static Logger LOG = LoggerFactory.getLogger(MetadataManagerServlet.class);

  private static final String METADATALOCATION = "jcr:content/metadata";

  // Logger and constants to always be declared on top of the class for easy
  // maintainence - ENDS

  // Since the Servlet extends SlingSafeMethodsServlet, we can use only GET
  // request.
  // If POST or other methods (PUT, DELETE) is to be used, then
  // SlingAllMethodsServlet should be extended.
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Getting all possible user inputs from request.
    String action = request.getParameter("action");
    String key = request.getParameter("key");
    String value = request.getParameter("value");

    // ResourceResolver is used to get instance of different APIs used in Java. It
    // can be adapted to any feasible class to carry on with the classes' in-build
    // methods.
    // ResourceResolver is also used to get AEM resource from the given path.
    // To get a resource resolver, one has to get it using an appropriate user.
    // If it is a servlet, resolver can be obtained from servlet's request which
    // internally contains the user who triggered the servlet.
    // If the backend operation is to be performed on user's name, then get the
    // resolver from servlet. But if has to be performed with a service user who has
    // more permissions to do the task, then get the resolver from
    // ResourceResolverFactory
    /*
     * Map<String, Object> serviceUserParams = new HashMap<>();
     * serviceUserParams.put(ResourceResolverFactory.SUBSERVICE, "myserviceuser");
     * ResourceResolver resolver =
     * resolverFactory.getServiceResourceResolver(serviceUserParams);
     */
    ResourceResolver resolver = request.getResourceResolver();

    // Checking if the variables we are going to use henceforth are null or not, if
    // any of them is null, we don't want to continue any further because we know at
    // one point in time there is going to be error in the code because a variable
    // is null. So I'm stopping the code, sending an error response.
    if (null != action && null != resolver) {

      //getting the asset resource directly from the request. It's going to give you a resource whose path matches with the path in your browser URL when you maake a call to this servlet.
      Resource assetResource = request.getResource();

      if (!StringUtils.startsWith(assetResource.getPath(), "/content/dam")) {
        LOG.info("The input path is not an asset.");
        generateResponse(HttpServletResponse.SC_BAD_REQUEST,
            "The input path is not an asset. Please make a GET request from an asset",
            response);
        return;
      }

      //From the resource, getting the child resource (jcr:content/metadata)
      Resource assetMetadataResource = assetResource.getChild(METADATALOCATION);
      
      if (null != assetMetadataResource) {
        // Getting ValueMap from asset containing all metadata properties
        ValueMap assetMetadataValueMap = assetMetadataResource.getValueMap();
        // Getting modifiable value map from asset to set or remove property
        ModifiableValueMap assetModifiableMetadataValueMap = assetMetadataResource.adaptTo(ModifiableValueMap.class);
        
        // Checking if any of the above is null. If null, stopping the process.
        if (null != assetMetadataValueMap && null != assetModifiableMetadataValueMap) {
          // Since we have more than two types of action, I'm using switch case to find
          // which action to perform.
          switch (action) {
            case "getMetadata":
              // If key is null, we cannot get the metadata value. Stopping the process.
              if (null == key) {
                generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot get metadata. Key is empty. (Example: ?action=getMetadata&key=testkey)",
                    response);
                return;
              }
              try {
                // using value map to get the property
                // parameter 1 - key for the property
                // parameter 2 - return type of the value
                String assetProperty = assetMetadataValueMap.get(key, String.class);
                generateResponse(HttpServletResponse.SC_ACCEPTED,
                    "Value for ".concat(key.concat(" is ".concat(assetProperty))), response);
                return;
              } catch (NullPointerException e) {
                generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot get metadata. Does not exist.",
                    response);
                return;
              }
            case "getAllMetadata":
              // creating an empty json object to collect all key and value
              JSONObject metadataObj = new JSONObject();
              // iterating the value map to get each property from the metadata
              for (Map.Entry<String, Object> eachProperty : assetMetadataValueMap.entrySet()) {
                try {
                  // for each iteration, picking the key and value from the metadata node and
                  // adding it to json object
                  metadataObj.put(eachProperty.getKey(), eachProperty.getValue());
                } catch (JSONException e) {
                  LOG.error("Error occured while getting all metadata properties: ", e);
                  generateResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      "Error occured while getting all metadata from asset. Check logs.",
                      response);
                  return;
                }
              }
              generateResponse(HttpServletResponse.SC_ACCEPTED,
                  metadataObj.toString(), response);
              return;
            case "setMetadata":
              // if either key or value is empty we cannot set metadata property. So stopping
              // the process
              if (null == key || null == value) {
                generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                    "Either key or value is empty. (Example: ?action=setMetadata&key=testkey&value=testvalue)",
                    response);
                return;
              }
              // using modifiable value map to set property to asset
              assetModifiableMetadataValueMap.put(key, value);
              // saving the changes
              resolver.commit();
              generateResponse(HttpServletResponse.SC_ACCEPTED,
                  "Metadata set for "
                      .concat(assetResource.getPath().concat(" key: ".concat(key.concat(" & value: ".concat(value))))),
                  response);
              return;
            case "deleteMetadata":
              // if key is null, we dont know which metadata property to delete. so stopping
              // the process
              if (null == key) {
                generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot delete metadata. Key is empty. (Example: ?action=deleteMetadata&key=testkey)",
                    response);
                return;
              }
              // using modifiable value map to remove the metadata property
              assetModifiableMetadataValueMap.remove(key);
              // saving the changes
              resolver.commit();
              generateResponse(HttpServletResponse.SC_ACCEPTED,
                  "Metadata deleted for "
                      .concat(assetResource.getPath().concat(" key: ".concat(key))),
                  response);
              return;
            default:
              break;
          }
        }
      }
    } else {
      generateResponse(HttpServletResponse.SC_BAD_REQUEST,
          "Cannot process request. Action is not defined. (Example: ?action=getAllMetadata)", response);
      return;
    }

  }

  private void generateResponse(int status, String message, HttpServletResponse response) throws IOException {
    PrintWriter writer = response.getWriter();
    response.setStatus(status);
    writer.write(message);
    writer.flush();
  }

  // Assignment
  // 1. Create a metadata schema
  // 2. Create a metadata profile
  // 3. Apply the schema and profile to a folder in AEM and check if the
  // properties are getting retained when a new asset is uploaded.
  // 4. In java, get, set and delete a multi value property on an asset.

}
