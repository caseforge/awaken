package io.github.caseforge.awaken;

public class FaultImpl implements Fault {

    private int code;
    
    private String msg;
    
    private String ext;

    public FaultImpl() {
        // TODO Auto-generated constructor stub
    }
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
    
    
    

}
