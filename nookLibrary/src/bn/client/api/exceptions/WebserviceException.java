package bn.client.api.exceptions;

public class WebserviceException extends Exception {
    
    /**
     * 
     */
    private static final long serialVersionUID = -6750239279362527911L;
    private WebserviceError error;
    private long mExtraLong;
    
    public WebserviceException(WebserviceError error) {
        super();
        this.error = error;
        mExtraLong = 0;
    }
    
    public WebserviceException(WebserviceError error, long extra) {
        super();
        this.error = error;
        mExtraLong = extra;
    }
    
    public WebserviceError getError() {
        return error;
    }
    
    public long getExtraLong() {
        return mExtraLong;
    }
}
