package eu.project.rapid.gvirtus4a;

/**
 * Created by raffaelemontella on 16/02/2018.
 */

public class CudaError {
    private int code;
    private String label;
    private String desc;

    public CudaError(int code, String label, String desc) {
        this.code=code;
        this.label=label;
        this.desc=desc;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }
    public String getDesc() { return desc; }
}
