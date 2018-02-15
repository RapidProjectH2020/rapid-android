package eu.project.rapid.gvirtus4a;

import java.io.IOException;
import java.util.Vector;

import eu.project.rapid.gvirtus4a.params.IntParam;

/**
 * Created by raffaelemontella on 26/04/2017.
 */

public class Provider {
    private static final String LOG_TAG = "PROVIDER";
    private String host;
    private int port;
    private Integer driverVersion;
    private Integer runtimeVersion;

    private Vector<CudaDeviceProp> properties=new Vector<CudaDeviceProp>();

    public Provider(String host, int port) {
        this.host=host;
        this.port=port;

        try {
            deviceQuery();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    public void deviceQuery() throws IOException {

        CudaRtFrontend cudaRtFrontend = new CudaRtFrontend(host, port);
        IntParam intParam=new IntParam();
        int exitCode = cudaRtFrontend.cudaGetDeviceCount(intParam);
        if (exitCode != 0) {
            return;
        }
        int deviceCount=intParam.value;

        cudaRtFrontend.cudaDriverGetVersion(intParam);
        driverVersion=intParam.value;

        cudaRtFrontend.cudaRuntimeGetVersion(intParam);
        runtimeVersion=intParam.value;

        for (int i = 0; i < deviceCount; i++) {
            cudaRtFrontend.cudaSetDevice(i);
            CudaDeviceProp deviceProp=new CudaDeviceProp(runtimeVersion);
            exitCode=cudaRtFrontend.cudaGetDeviceProperties(i,deviceProp);
            if (exitCode==0) {
                properties.add(deviceProp);
            }
        }
        cudaRtFrontend.close();
    }
}
