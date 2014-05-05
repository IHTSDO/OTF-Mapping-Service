package org.ihtsdo.otf.mapping.jpa.helpers;

/**
 * Service exception.
 */
@SuppressWarnings("serial")
public class ServiceException extends Exception {
	
    /** The error code. */
    private String errorCode="Unknown_Service_Exception";
     
    /**
     * Instantiates a {@link ServiceException} from the specified parameters.
     *
     * @param message the message
     * @param errorCode the error code
     */
    public ServiceException(String message, String errorCode){
        super(message);
        this.errorCode=errorCode;
    }
     
    /**
     * Instantiates a {@link ServiceException} from the specified parameters.
     *
     * @param message the message
     */
    public ServiceException(String message) {
    	 super(message);
         this.errorCode=message;
	}

	/**
	 * Returns the error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode(){
        return this.errorCode;
    }
}
