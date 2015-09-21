/*
 * Created on 2005-11-14
 * @author xiexh
 * @version 1.0
 * C2C_PACKET PROJECT
 */

/**
 * @author xiexh
 * �����쳣
 * 11:52:24 
 */
public class ServiceException extends Exception{
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] param=null;
	
	private String code="9999";
    public ServiceException(){
        super();
    }
    public ServiceException(String code){
       this.code=code;
    }
    public ServiceException(String code,String message){
        super(message);
        this.code=code;        
    }
    public ServiceException(String code,String[] param){    	
    	this.code=code;
    	this.param=param;
    	
    }
    public String getEventID(){
        return code;
    }
    public String[] getParam() {
		return param;
	}

}