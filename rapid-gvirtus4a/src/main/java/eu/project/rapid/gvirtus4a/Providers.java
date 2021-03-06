package eu.project.rapid.gvirtus4a;

import android.util.Log;

import java.io.IOException;
import java.util.Comparator;
import java.util.Vector;

import eu.project.rapid.gvirtus4a.params.IntParam;

/**
 * Created by raffaelemontella on 26/04/2017.
 */

public class Providers  {
    private static final String LOG_TAG = "PROVIDERS";
    private static final int DEFAULT_MIN_RUNTIME_VERSION=6500;
    private Vector<Provider> providers;
    private static Providers instance;
    private Provider defaultProvider;

    public void setDefaultProvider(Provider provider) {
        this.defaultProvider=provider;
    }
    public Provider getDefaultProvider() {
        if (defaultProvider==null) {
            defaultProvider=getBest();
        }
    return defaultProvider;
    }

    private Providers() {
        providers = new Vector<>();

        instance = this;
    }

    public static Providers getInstance() {
        if (instance == null) {
            instance = new Providers();
        }
        return instance;
    }

    public Provider getBest() {
        return getBest(DEFAULT_MIN_RUNTIME_VERSION);
    }

    public Provider getBest(int minRuntimeVersion) {
        Log.d(LOG_TAG,"getBest");
        Provider result=null;
        Vector<Provider> availables=new Vector<>();
        for (Provider provider:providers) {
            Log.d(LOG_TAG,"Provider:"+provider.getHost()+":"+provider.getPort());
            try {
                provider.deviceQuery();
                if (provider.getRuntimeVersion()>=minRuntimeVersion) {
                    CudaDeviceProp cudaDeviceProp = provider.getDeviceProperties();
                    Log.d(LOG_TAG,"Name:"+cudaDeviceProp.getName());
                    Log.d(LOG_TAG,"RuntimeVersion:"+provider.getRuntimeVersion());
                    Log.d(LOG_TAG,"Minor:"+cudaDeviceProp.getMinor());
                    Log.d(LOG_TAG,"Major:"+cudaDeviceProp.getMajor());

                    boolean success = provider.matrixMul();
                    if (success==true) {
                        Log.d(LOG_TAG,"wallClock:"+provider.getWallClock());
                        availables.add(provider);
                        Log.d(LOG_TAG,"Added!");
                    } else {
                        Log.d(LOG_TAG,"MatrixMul failed!");
                    }
                }

            } catch (IOException ex) {
                Log.e(LOG_TAG,ex.getMessage());
            }

        }
        if (availables.size()>0) {
            result = availables.get(0);
            for (Provider provider:availables) {
                if (result.getWallClock()>provider.getWallClock()) {
                    result=provider;
                }
            }
        }
        return result;
    }

    public void register(String host, int port) {
        Provider provider = new Provider(host, port);
        providers.add(provider);
    }



    public void unregister() {
        providers.removeAllElements();
    }
}
