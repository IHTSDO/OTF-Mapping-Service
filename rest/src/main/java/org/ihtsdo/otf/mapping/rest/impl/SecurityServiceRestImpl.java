/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.json.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


/**
 * REST implementation for security service.
 */
@Path("/security")
@Api(value = "/security", description = "Operations supporting application authentication and authorization.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class SecurityServiceRestImpl extends RootServiceRestImpl implements SecurityServiceRest {

  /** The HTTP request */
  // @Resource
  @Context
  private HttpServletRequest httpServletRequest;
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#authenticate(java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/authenticate/{username}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @Produces({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Authenticate a map user.", notes = "Performs authentication on specified username and password and returns a token upon successful authentication. Throws 401 error if not.", response = String.class)
  public String authenticate(
    @ApiParam(value = "Username", required = true) @PathParam("username") String username,
    @ApiParam(value = "Password, as string post data", required = true) String password)
    throws Exception {
	  
    Logger.getLogger(SecurityServiceRestImpl.class)
        .info("RESTful call (Authentication): /authentication for map user = "
            + username);
    final SecurityService securityService = new SecurityServiceJpa();
    try {
      return securityService.authenticate(username, password).getAuthToken();
    } catch (LocalException e) {
      throw new WebApplicationException(
          Response.status(401).entity(e.getMessage()).build());
    } catch (Exception e) {
      handleException(e, "Unexpected error trying to authenticate a map user");
      return null;
    } finally {
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#logout(java.lang.String)
   */
  @Override
  @POST
  @Path("/logout/user/id/{userName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Log out.", notes = "Logs a map user out of the tool.", response = String.class)
  public String logout(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName)
    throws Exception {

    Logger.getLogger(SecurityServiceRestImpl.class)
        .info("RESTful call (Logout) : /logout/user/id/" + userName);
    final SecurityService securityService = new SecurityServiceJpa();
    try {
      return securityService.logout(userName);
    } catch (LocalException e) {
      throw new WebApplicationException(
          Response.status(401).entity(e.getMessage()).build());
    } catch (Exception e) {
      handleException(e, "Unexpected error trying to authenticate a map user");
    } finally {
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#getConfigProperties()
   */
  @Override
  @GET
  @Path("/properties")
  @Produces({
      MediaType.APPLICATION_JSON
  })
  @ApiOperation(value = "Get configuration properties", notes = "Gets user interface-relevant configuration properties", response = String.class, responseContainer = "Map")
  public Map<String, String> getConfigProperties() {
    Logger.getLogger(getClass())
        .info("RESTful call (Configure): /configure/properties");
    try {
      Map<String, String> map = new HashMap<>();
      for (final Map.Entry<Object, Object> o : ConfigUtility
          .getUiConfigProperties().entrySet()) {
        map.put(o.getKey().toString(), o.getValue().toString());
      }
      return map;
    } catch (Exception e) {
      handleException(e, "getting ui config properties");
    } finally {
      // n/a
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#getConfigProperties()
   */
  @Override
  //@GET
  @Path("/callback")
  @Produces({
    MediaType.APPLICATION_JSON
  })
  @ApiOperation(value = "", notes = "Handle callback from OAuth2", response = String.class)
  public Response callback() throws Exception {
    Logger.getLogger(getClass()).info("RESTful call /security/callback");
    // FOR DOCUMENATION SEE
    // https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow
    try {
      
      //TODO: Most of this should be in the OAuth2SecurityHandler
      
      //log everything
      logHttpRequest("CALLBACK");
      
      // 1. READ AUTHORIZATION RESPONSE 
      
      //final String body = getHttpServletRequestBody();
      final String queryString = httpServletRequest.getQueryString();
      List<NameValuePair> params = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
      
      // 1A. HANDLE FAILURE FROM AUTHORIZATION RESPONSE - throw error
      // ex: GET https://localhost/security/callback
      //   ?error=access_denied&error_description=the+user+canceled+the+authentication
      if (params.contains("error")) {
        final String errorMsg = "Error: " + params.get(params.indexOf("error")).getValue()
            + " Error Description: " + params.get(params.indexOf("error_description")).getValue();
        Logger.getLogger(getClass()).error(errorMsg);
        throw new Exception(errorMsg);
      }
      
      final Properties config = ConfigUtility.getConfigProperties();      
      
      // 1B. HANDLE SUCCESS FROM AUTHORIZATION RESPONSE
      // ex: GET https://localhost/security/callback
      //   ?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGBCmLdgfSTLEMPGYuNHSUYBrq...&state=12345
      final String code = params.get(params.indexOf("code")).getValue();
      
      
      // 2. GET ACCESS TOKEN
      // ex:
      // POST /{tenant}/oauth2/v2.0/token HTTP/1.1
      // Host: https://login.microsoftonline.com
      // Content-Type: application/x-www-form-urlencoded
      //
      // client_id=6731de76-14a6-49ae-97bc-6eba6914391e
      // &scope=user.read%20mail.read
      // &code=OAAABAAAAiL9Kn2Z27UubvWFPbm0gLWQJVzCTE9UkP3pSx1aXxUjq3n8b2JRLk4OxVXr...
      // &redirect_uri=http%3A%2F%2Flocalhost%2Fmyapp%2F
      // &grant_type=authorization_code
      // &client_secret=JqQX2PNo9bpM0uEihUPzyrh // NOTE: Only required for web
      // apps
      
      final String accessTokenParams = String.format(
          "?client_id=%s&scope=%s&code=%s&redirect_uri=%s&grant_type=%s",
          config.get("security.handler.OAUTH2.client_id"),
          config.get("security.handler.OAUTH2.scope"), config.get(code),
          config.get("security.handler.OAUTH2.redirect_uri"),
          config.get("security.handler.OAUTH2.url.grant_type"));
      
      Logger.getLogger(getClass()).info("Access Token params" + accessTokenParams);
      
      // 2. HANDLE RESPONSE FROM GET ACCESS TOKEN
      final URL accessTokenUrl = new URL(config.getProperty("security.handler.OAUTH2.url.access_token"));
      final JSONObject tokenResponse = retrieveJsonPost(accessTokenUrl, accessTokenParams);
      Logger.getLogger(getClass()).info("Access Token Reponse " + tokenResponse);
      
      // ex: response
      // {
      //    "token_type": "Bearer",
      //    "scope": "user.read%20Fmail.read",
      //    "expires_in": 3600,
      //    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6...",
      //    "refresh_token": "AwABAAAAvPM1KaPlrEqdFSBzjqfTGAMxZGUTdM0t4B4..."
      // }
      final String accessToken = tokenResponse.getString("access_token");
      final String tokenType = tokenResponse.getString("token_type");

      // 3. GET USER INFORMATION
      // ex:
      // GET https://graph.microsoft.com/v1.0/me
      //  Authorization: Bearer eyJ0eXAiO ... 0X2tnSQLEANnSPHY0gKcgw
      //  Host: graph.microsoft.com
      
      final URL userInfoUrl = new URL(config.getProperty("security.handler.OAUTH2.url.user_info"));
      JSONObject userResponse = retrieveJsonGet(userInfoUrl, accessToken, tokenType);
      
      if (userResponse.getString("error") != null) {
        final String errorMsg = "Error: " + params.get(params.indexOf("error")).getValue()
            + " Error Description: " + params.get(params.indexOf("error_description")).getValue();
        Logger.getLogger(getClass()).error("Full response: " + userResponse.toString());
        throw new Exception(errorMsg);
      }
            
      //response
      // HTTP/1.1 200 OK
      // Content-Type:
      // application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8
      // request-id: f45d08c0-6901-473a-90f5-7867287de97f
      // client-request-id: f45d08c0-6901-473a-90f5-7867287de97f
      // OData-Version: 4.0
      // Duration: 727.0022
      // Date: Thu, 20 Apr 2017 05:21:18 GMT
      // Content-Length: 407
      //
      // {
      // "@odata.context":"https://graph.microsoft.com/v1.0/$metadata#users/$entity",
      //    "id":"12345678-73a6-4952-a53a-e9916737ff7f",
      //    "businessPhones":[
      //        "+1 555555555"
      //    ],
      //    "displayName":"Chris Green",
      //    "givenName":"Chris",
      //    "jobTitle":"Software Engineer",
      //    "mail":null,
      //    "mobilePhone":"+1 5555555555",
      //    "officeLocation":"Seattle Office",
      //    "preferredLanguage":null,
      //    "surname":"Green",
      //    "userPrincipalName":"ChrisG@contoso.onmicrosoft.com"
      // }
      
      userResponse.append("access_token", accessToken);
      authenticate(userResponse.getString("userPrincipalName"), userResponse.toString());
      
      //https://host/index.html#login?token=
      return Response.temporaryRedirect(new URI(config.getProperty("security.handler.OAUTH2.url.redirect") + accessToken)).build();
    }
    catch(Exception e) {
      handleException(e, "callback");
      return Response.serverError().build();
    }
    finally {
      
    }
  }
  
  /**
   * 
   * @param step
   */
  private void logHttpRequest(String step) {
    Logger.getLogger(getClass()).info(step);
    Logger.getLogger(getClass()).info("\tHTTP Method: " + httpServletRequest.getMethod());
    Logger.getLogger(getClass()).info("\tQuery String: " + httpServletRequest.getQueryString());
    httpServletRequest.getHeaderNames().asIterator().forEachRemaining(headerName ->
    {
      Logger.getLogger(getClass()).info("\tHeader: " + headerName + " value: " + httpServletRequest.getHeader(headerName));
    });
    httpServletRequest.getParameterNames().asIterator().forEachRemaining(param ->
    {
      Logger.getLogger(getClass()).info("\tParams: " + param + " value: " + httpServletRequest.getHeader(param));
    });
    Logger.getLogger(getClass()).info("\tBody: " + getHttpServletRequestBody());
  }
  
  /**
   * temporary until the callback method knows exactly what it is getting
   * @return
   */
  private String getHttpServletRequestBody() { 
    StringBuilder stringBuilder = new StringBuilder();  
    BufferedReader bufferedReader = null;  
  
    try {  
        InputStream inputStream = httpServletRequest.getInputStream(); 
  
        if (inputStream != null) {  
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));  
  
            char[] charBuffer = new char[128];  
            int bytesRead = -1;  
  
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {  
                stringBuilder.append(charBuffer, 0, bytesRead);  
            }  
        } else {  
            stringBuilder.append("");  
        }  
    } catch (IOException ex) {  
      Logger.getLogger(getClass()).error("Error reading the request body...");  
    } finally {  
        if (bufferedReader != null) {  
            try {  
                bufferedReader.close();  
            } catch (IOException ex) {  
              Logger.getLogger(getClass()).error("Error closing bufferedReader...");  
            }  
        }  
    }  
  
    return stringBuilder.toString();  
  }
  
  /**
   * Forms an HTTP request, sends it using POST method and returns the result of
   * the request as a JSONObject.
   * 
   * @param url The URL to query for a JSONObject.
   * @param parameters Additional POST parameters
   * @return The translated String.
   * @throws Exception on error.
   */
  private JSONObject retrieveJsonPost(final URL url, final String parameters)
    throws Exception {
    try {

      final byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
      final HttpURLConnection httpUrlConnection =
          (HttpURLConnection) url.openConnection();
      httpUrlConnection.setDoOutput(true);
      httpUrlConnection.setInstanceFollowRedirects(false);
      httpUrlConnection.setRequestMethod("POST");
      httpUrlConnection.setRequestProperty("Content-Type",
          "application/x-www-form-urlencoded");
      httpUrlConnection.setRequestProperty("charset", "utf-8");
      httpUrlConnection.setRequestProperty("Content-Length",
          Integer.toString(postData.length));
      httpUrlConnection.setUseCaches(false);

      final PrintWriter pw =
          new PrintWriter(httpUrlConnection.getOutputStream());
      pw.write(parameters);
      pw.close();
      httpUrlConnection.getOutputStream().close();

      try {
        final String result =
            inputStreamToString(httpUrlConnection.getInputStream());

        return new JSONObject(result);
      } finally {
        // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
        if (httpUrlConnection.getInputStream() != null) {
          httpUrlConnection.getInputStream().close();
        }
        if (httpUrlConnection.getErrorStream() != null) {
          httpUrlConnection.getErrorStream().close();
        }
        if (pw != null) {
          pw.close();
        }
      }
    } catch (Exception ex) {
      throw new Exception("Error retrieving translation.", ex);
    }
  }
  
  /**
   * Forms an HTTP request, sends it using GET method and returns the result of
   * the request as a JSONObject.
   * 
   * @param url The URL to query for a JSONObject.
   * @param accessToken Access Token
   * @param tokenType Type of token ex: Bearer, Basic
   * @return The JSONObject String.
   * @throws Exception on error.
   */
  private JSONObject retrieveJsonGet(final URL url, final String accessToken,
    final String tokenType) throws Exception {
    try {

      final HttpURLConnection httpUrlConnection =
          (HttpURLConnection) url.openConnection();
      httpUrlConnection.setDoOutput(true);
      httpUrlConnection.setInstanceFollowRedirects(false);
      httpUrlConnection.setRequestMethod("GET");
      httpUrlConnection.setRequestProperty(
          org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json");
      httpUrlConnection.setRequestProperty("charset", "utf-8");
      httpUrlConnection.setRequestProperty("Authorization",
          tokenType + " " + accessToken);
      httpUrlConnection.setUseCaches(false);
      httpUrlConnection.getOutputStream().close();

      try {
        final String result =
            inputStreamToString(httpUrlConnection.getInputStream());
        return new JSONObject(result);

      } finally {
        // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
        if (httpUrlConnection.getInputStream() != null) {
          httpUrlConnection.getInputStream().close();
        }
        if (httpUrlConnection.getErrorStream() != null) {
          httpUrlConnection.getErrorStream().close();
        }
      }
    } catch (Exception ex) {
      throw new Exception("Error retrieving json from url " + url.toString(),
          ex);
    }
  }

  /**
   * Reads an InputStream and returns its contents as a String. Also effects
   * rate control.
   * @param inputStream The InputStream to read from.
   * @return The contents of the InputStream as a String.
   * @throws Exception on error.
   */
  private static String inputStreamToString(final InputStream inputStream)
    throws Exception {
    final StringBuilder outputBuilder = new StringBuilder();

    String string;
    if (inputStream != null) {
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      while (null != (string = reader.readLine())) {
        outputBuilder.append(string).append('\n');
      }
    }

    return outputBuilder.toString();
  }
  
}
  