package net.sf.appstatus.camel.utils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.collections.CollectionUtils;

import net.sf.appstatus.camel.StatusCamelException;

public final class JMXHelper {

    private static final String CONTEXT_OBJECT_NAME = "org.apache.camel:context=*,type=context,name=\"*\"";
    private static final String ROUTE_OBJECT_NAME = "org.apache.camel:context=*,type=routes,name=\"*\"";

    private static MBeanServer mBeanServer;

    static {
        // Could throw SecurityException
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    private JMXHelper() {
        // Nothing to do
    }

    public static List<ManagedCamelContextMBean> getRegisteredContexts() throws StatusCamelException {
        List<String> contexts = getRegistered(CONTEXT_OBJECT_NAME);
        if (CollectionUtils.isEmpty(contexts)) {
            return new ArrayList<>();
        }

        return getMBeans(contexts, ManagedCamelContextMBean.class);
    }

    public static List<ManagedRouteMBean> getRegisteredRoutes() throws StatusCamelException {
        List<String> routes = getRegistered(ROUTE_OBJECT_NAME);
        if (CollectionUtils.isEmpty(routes)) {
            return new ArrayList<>();
        }

        return getMBeans(routes, ManagedRouteMBean.class);
    }

    public static ManagedRouteMBean getRegisteredRoute(String objectName) throws StatusCamelException {
        try {
            ObjectName on = ObjectName.getInstance(objectName);
            return getRegisteredRoute(on);
        } catch (JMException e) {
            throw new StatusCamelException(e);
        }
    }

    public static ManagedRouteMBean getRegisteredRoute(String camelId, String idRoute) throws StatusCamelException {
        try {
            ObjectName on = ObjectName
                    .getInstance("org.apache.camel:context=" + camelId + ",type=routes,name=\"" + idRoute + "\"");
            return getRegisteredRoute(on);
        } catch (JMException e) {
            throw new StatusCamelException(e);
        }
    }

    private static ManagedRouteMBean getRegisteredRoute(ObjectName on) throws StatusCamelException {
        if (!mBeanServer.isRegistered(on)) {
            return null;
        }

        List<ManagedRouteMBean> mbeans = getMBeans(Arrays.asList(on.getCanonicalName()), ManagedRouteMBean.class);
        if (CollectionUtils.isEmpty(mbeans) || CollectionUtils.size(mbeans) != 1) {
            return null;
        }
        return mbeans.get(0);
    }

    public static List<String> getRegistered(String objName) throws StatusCamelException {
        try {
            ObjectName name = ObjectName.getInstance(objName);

            Set<ObjectName> ons = mBeanServer.queryNames(name, null);
            if (CollectionUtils.isEmpty(ons)) {
                return new ArrayList<>();
            }

            List<String> result = new ArrayList<>();
            for (ObjectName on : ons) {
                result.add(on.getCanonicalName());
            }
            Collections.sort(result);

            return result;
        } catch (JMException e) {
            throw new StatusCamelException(e);
        }
    }

    private static <T> List<T> getMBeans(List<String> contexts, Class<T> clazz) throws StatusCamelException {
        try {
            List<T> result = new ArrayList<>();
            for (String qn : contexts) {
                ObjectName name = ObjectName.getInstance(qn);
                result.add(MBeanServerInvocationHandler.newProxyInstance(mBeanServer, name, clazz, true));
            }
            return result;
        } catch (JMException e) {
            throw new StatusCamelException(e);
        }
    }

}
