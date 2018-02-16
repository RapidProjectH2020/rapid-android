package eu.project.rapid.gvirtus4a;

import java.io.IOException;
import java.util.Vector;

import eu.project.rapid.gvirtus4a.params.IntParam;

/**
 * Created by raffaelemontella on 26/04/2017.
 */

public class Providers {
    private static final String LOG_TAG = "PROVIDERS";
    private Vector<Provider> providers;
    private static Providers instance;

    private Providers() {
        providers = new Vector<>();
        providers.add(new Provider("193.205.230.23", 9998));
        providers.add(new Provider("80.158.23.133", 9998));
        providers.add(new Provider("54.72.110.23", 9998));

        instance = this;
    }

    public static Providers getInstance() {
        if (instance == null) {
            instance = new Providers();
        }
        return instance;
    }

    public Provider getBest() {
        /*
        Provider result=null;
        Vector<Provider> availables=new Vector<>();
        for (Provider provider:providers) {
            try {
                provider.deviceQuery();
                availables.add(provider);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
        result=availables.get(0);
        return result;
        */
        return providers.get(0);
    }

    public void register(String host, int port) {
        Provider provider = new Provider(host, port);
        providers.add(provider);
    }


}
