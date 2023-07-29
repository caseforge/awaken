package io.github.caseforge.awaken.spring;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import io.github.caseforge.awaken.ResourceProvider;

public class SpringResourceProvider implements ResourceProvider {

    private ApplicationContext applicationContext;

    public SpringResourceProvider(ApplicationContext applicationContext) {
        super();
        this.applicationContext = applicationContext;
    }

    @Override
    public Object getBean(String name) throws Exception {
        return applicationContext.getBean(name);
    }

    @Override
    public byte[] getResource(String uri) throws Exception {
        Resource resource = applicationContext.getResource(uri);

        if (!resource.exists()) {
            return null;
        }

        InputStream inputStream = resource.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] bs = new byte[512];
            int len = 0;
            while ((len = inputStream.read(bs)) > -1) {
                baos.write(bs, 0, len);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return baos.toByteArray();
    }

}
