package com.deloittetraining.aem.core.servlets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

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
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Revision;

/*
 * Purpose of this servlet is to illustrate a programmatic way of performing basic CRUD operation on assets in AEM.
 * Using this servlet one can do the following
 * - Create Asset
 * - Delete Asset
 * - Copy Asset
 * - Move Asset
 * - Create Version
 */
//Decalring Servlet using @Component Annotation - STARTS
@Component(service = Servlet.class, property = {
    Constants.SERVICE_DESCRIPTION + "=AEM Assets Training Servlet for Asset Management",
    ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ServletResolverConstants.DEFAULT_RESOURCE_TYPE,
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + "json",
    ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + "deloittetraining"
})
// Decalring Servlet using @Component Annotation - ENDS
public class AssetManagerServlet extends SlingSafeMethodsServlet {

  // Logger and constants to always be declared on top of the class for easy
  // maintainence - STARTS

  final static Logger LOG = LoggerFactory.getLogger(AssetManagerServlet.class);

  private static final String PARENTLOCATION = "/content/dam/deloittetraining";

  private static final String SAMPLEDATA = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.";

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
    String deletePath = request.getParameter("deletePath");
    String srcPath = request.getParameter("srcPath");
    String targetPath = request.getParameter("targetPath");

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
      // If resolver is not null, I'm using it to adapt the resolver to two types of
      // asset manager classes.
      com.day.cq.dam.api.AssetManager dayCqAssetManager = resolver.adaptTo(com.day.cq.dam.api.AssetManager.class);
      com.adobe.granite.asset.api.AssetManager graniteAssetManager = resolver
          .adaptTo(com.adobe.granite.asset.api.AssetManager.class);

      // Since we have more than two types of action, I'm using switch case to find
      // which action to perform.
      switch (action) {
        case "createAsset":
          // This case creates an asset in a fixed path.
          // Getting an input stream from sample string data to create asset, one can use
          // any type of stream (text, image, video, e.t.c.)
          InputStream is = new ByteArrayInputStream(SAMPLEDATA.getBytes());
          // using asset manager's createAsset method to create an asset under the fixed
          // path
          // parameter 1 - location and name of the asset (I'm using UUID API to create a
          // random ID for name)
          // parameter 2 - input stream obtained from the previous step
          // parameter 3 - mimetype of the asset
          // parameter 4 - should the asset be saved in the repository
          Asset asset = dayCqAssetManager.createAsset(
              PARENTLOCATION.concat("/".concat(UUID.randomUUID().toString().concat(".txt"))), is, "text/plain", true);
          // after asset creation, checking if asset exists in repo, if yes - sending a
          // success response. Otherwise a failure response.
          if (null != asset) {
            generateResponse(HttpServletResponse.SC_ACCEPTED, "Asset created at: ".concat(asset.getPath()), response);
            return;
          } else {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Error. Cannot create asset.",
                response);
            return;
          }
        case "deleteAsset":
        // This case deletes an asset in a fixed path.
        // Checking if user had provided a path where java needs to go and delete the asset. If null, sending an error response.
          if (null == deletePath) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot process request. Delete Path is not defined. (Example: ?action=deleteAsset&deletePath=/content/dam/deloittetraining/abc.txt)",
                response);
            return;
          }
          try {
            //if path exists, using asset manager to remove asset
            graniteAssetManager.removeAsset(deletePath);
            //since this assetmanager does not have a provision to auto save like the other one, using resolver to commit the changes to repo.
            resolver.commit();
            //sending success response.
            generateResponse(HttpServletResponse.SC_ACCEPTED, "Asset deleted successfully.", response);
            return;
          } catch (AssetException ex) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST, "Cannot delete asset. Asset might not exist!",
                response);
            return;
          }
        case "copyAsset":
          //checking if any of the path is null, if yes - stopping the process since we know at one point code is going to throw error.
          if (null == srcPath || null == targetPath) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Either the src path or target path is missing. (Example: ?action=copyAsset&srcPath=/content/dam/deloittetraining/abc.txt&targetPath=/content/dam/deloittetraining/folder-2/abc.txt)",
                response);
            return;
          }
          try {
            //if both paths are present, using asset manager to copy asset
            graniteAssetManager.copyAsset(srcPath, targetPath);
            //since this assetmanager does not have a provision to auto save like the other one, using resolver to commit the changes to repo.
            resolver.commit();
            //sending success response.
            generateResponse(HttpServletResponse.SC_ACCEPTED,
                "Copied asset from ".concat(srcPath.concat(" to ".concat(targetPath))), response);
            return;
          } catch (AssetException e) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Error while copying asset from source to destination folder", response);
            return;
          }
        case "moveAsset":
          //checking if any of the path is null, if yes - stopping the process since we know at one point code is going to throw error.
          if (null == srcPath || null == targetPath) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Either the src path or target path is missing. (Example: ?action=copyAsset&srcPath=/content/dam/deloittetraining/abc.txt&targetPath=/content/dam/deloittetraining/folder-2/abc.txt)",
                response);
            return;
          }
          try {
            //if both paths are present, using asset manager to move asset
            graniteAssetManager.moveAsset(srcPath, targetPath);
            //since this assetmanager does not have a provision to auto save like the other one, using resolver to commit the changes to repo.
            resolver.commit();
            //sending success response.
            generateResponse(HttpServletResponse.SC_ACCEPTED,
                "Moved asset from ".concat(srcPath.concat(" to ".concat(targetPath))), response);
            return;
          } catch (AssetException e) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Error while moving asset from source to destination folder", response);
            return;
          }
        case "createVersion":
         //checking if user had added a path of asset whose version needs to be changed. If not, sending error response.
          if (null == srcPath) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Did not find a path to change version. (Example: ?action=createVersion&srcPath=/content/dam/deloittetraining/abc.txt)",
                response);
            return;
          }
          try {
            //using resolver to get resource and then adapting the resource to Asset class.
            Asset inputAsset = resolver.getResource(srcPath).adaptTo(Asset.class);
            //using asset manager to get all versionf of an asset.
            Collection<Revision> revisions = dayCqAssetManager.getRevisions(srcPath, null);
            //using asset manager to create a new version of the asset.
            //parameter 1 - asset whose version needs to be created.
            //parameter 2 - version number
            //parameter 3 - version comment
            dayCqAssetManager.createRevision(inputAsset, Integer.toString(revisions.size()),
                "Created a new version at ".concat(Long.toString(new Date().getTime())));
            //sending success response
            generateResponse(HttpServletResponse.SC_ACCEPTED, "New asset version created succesfully.", response);
            return;
          } catch (Exception e) {
            generateResponse(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot create version for asset. Check log for details.",
                response);
            LOG.info("Error while creating version: ", e);
            return;
          }
        default:
          break;
      }
    } else {
      generateResponse(HttpServletResponse.SC_BAD_REQUEST,
          "Cannot process request. Action is not defined. (Example: ?action=createAsset)", response);
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
