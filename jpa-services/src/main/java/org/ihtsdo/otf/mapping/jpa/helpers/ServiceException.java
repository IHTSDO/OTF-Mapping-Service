package org.ihtsdo.otf.mapping.jpa.helpers;

public class ServiceException extends Exception {
	
	private static final long serialVersionUID = 4664456874499611218L;
    
    private String errorCode="Unknown_Service_Exception";
     
    public ServiceException(String message, String errorCode){
        super(message);
        this.errorCode=errorCode;
    }
     
    public ServiceException(String message) {
    	 super(message);
         this.errorCode=message;
	}

	public String getErrorCode(){
        return this.errorCode;
    }
}
