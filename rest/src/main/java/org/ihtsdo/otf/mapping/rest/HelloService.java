package org.ihtsdo.otf.mapping.rest;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
// called with http://localhost:8080/mapping-rest/rest/hello/abc

@Path("/hello")
public class HelloService { 
	//http://localhost:8080/mapping-rest/hello/1/abc
	@GET
	@Path("/1/{param}")
  public Response sayHelloInPlainText(@PathParam("param") String msg) {		  
    String output =  "Hello world from JAX-RS! " + msg;
    return Response.status(200).entity(output).build();

  }
  //http://localhost:8080/mapping-rest/hello/2/abc
	@GET
	@Path("/2/{param}")
 @Produces(MediaType.TEXT_PLAIN)
  public String sayHelloInPlainText2(@PathParam("param") String msg) {		  
    return "Hello world from JAX-RS2! " + msg;

  }
 
  // This method is called if HTML is requested
	// http://localhost:8080/mapping-rest/hello/3
  @GET
	@Path("/3")
  @Produces(MediaType.TEXT_HTML)
  public String sayHelloInHtml() {
    return "<html> " + "<title>" + "Hello world from JAX-RS!" + "</title>"
        + "<body><h1>" + "Hello world from JAX-RS!" + "</body></h1>" + "</html> ";
  }
  
  // do 2nd one for getConcept() with id instead of param, return xml not plain text
  // /2 will be /concept
  // ContentService
  // one method to return json
}
