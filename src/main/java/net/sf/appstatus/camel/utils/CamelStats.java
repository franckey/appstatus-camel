package net.sf.appstatus.camel.utils;

import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.lang3.StringUtils;

import net.sf.appstatus.web.pages.Resources;

public class CamelStats {

    private static final int PERCENT_100 = 100;

    public static final Object[] HEADERS_CONTEXT = { "", "CamelId", "TotalRoutes", "ExchangesTotal", "Inflight",
            "MinProcessingTime", "MaxProcessingTime", "MeanProcessingTime", "ExchangesFailed",
            "LastExchangeCompleted" };

    public static final Object[] HEADERS_ROUTE = { "", "CamelId", "RouteId", "ExchangesTotal", "Inflight",
            "MinProcessingTime", "MaxProcessingTime", "MeanProcessingTime", "ExchangesFailed",
            "LastExchangeCompleted" };

    private CamelStats() {
        // Nothing to do
    }

    public static Object[] getRouteStats(ManagedRouteMBean stats) throws Exception {
        return new Object[] { //
                stats.getCamelId(), //
                stats.getRouteId(), //
                stats.getExchangesTotal(), //
                stats.getExchangesInflight(), //
                stats.getMinProcessingTime(), //
                stats.getMaxProcessingTime(), //
                stats.getMeanProcessingTime(), //
                stats.getExchangesFailed() + getPercent(stats.getExchangesFailed(), stats.getExchangesTotal()), //
                stats.getLastExchangeCompletedTimestamp() //
        };
    }

    public static Object[] getContextStats(ManagedCamelContextMBean stats) throws Exception {
        return new Object[] { //
                stats.getCamelId(), //
                stats.getTotalRoutes(), //
                stats.getExchangesTotal(), //
                stats.getExchangesInflight(), //
                stats.getMinProcessingTime(), //
                stats.getMaxProcessingTime(), //
                stats.getMeanProcessingTime(), //
                stats.getExchangesFailed() + getPercent(stats.getExchangesFailed(), stats.getExchangesTotal()), //
                stats.getLastExchangeCompletedTimestamp() //
        };
    }

    private static String getPercent(long value1, long value2) {
        if (value2 == 0) {
            return " (-%)";
        }
        return " (" + ((PERCENT_100 * value1) / value2) + "%)";
    }

    public static String getContextStatus(ManagedCamelContextMBean mbean) {
        return getStatus(mbean.getState());
    }

    public static String getRouteStatus(ManagedRouteMBean mbean) {
        return getStatus(mbean.getState());
    }

    private static String getStatus(String state) {
        if (StringUtils.equals(state, ServiceStatus.Started.name())) {
            return Resources.STATUS_OK;
        }
        return Resources.STATUS_WARN;
    }

}
