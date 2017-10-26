package net.sf.appstatus.camel.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.appstatus.camel.utils.JMXHelper;
import net.sf.appstatus.core.check.AbstractCheck;
import net.sf.appstatus.core.check.ICheckResult;

public class RoutesPerformanceCheck extends AbstractCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesFailureCheck.class);

    private int delay = 10;
    private int limitWarn = 40000;
    private int limitError = 60000;

    @Override
    public ICheckResult checkStatus(Locale locale) {
        ICheckResult result = null;
        try {
            List<ManagedRouteMBean> routes = JMXHelper.getRegisteredRoutes();

            List<String> warns = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (ManagedRouteMBean route : routes) {
                // Do not report if hit count is too small
                if (this.delay > route.getExchangesTotal()) {
                    continue;
                }

                if (route.getMeanProcessingTime() > limitError) {
                    errors.add(
                            "Route <b>" + route.getCamelId() + "#" + route.getRouteId() + "</b> mean processing time ("
                                    + route.getMeanProcessingTime() + "ms is over error limit (" + limitError + "ms)");
                } else if (route.getMeanProcessingTime() > limitWarn) {
                    warns.add(
                            "Route <b>" + route.getCamelId() + "#" + route.getRouteId() + "</b> mean processing time ("
                                    + route.getMeanProcessingTime() + "ms is over warn limit (" + limitWarn + "ms)");
                }
            }

            if (CollectionUtils.isNotEmpty(errors)) {
                String description = StringUtils.join(errors, "<br/>");
                if (CollectionUtils.isNotEmpty(warns)) {
                    description = description + " <br/>Additional warnings: " + StringUtils.join(warns, "<br/>");
                }
                result = result(this).code(ICheckResult.ERROR).description(description).build();
            } else if (CollectionUtils.isNotEmpty(warns)) {
                result = result(this).code(ICheckResult.ERROR).description(StringUtils.join(warns, "<br/>")).build();
            } else {
                result = result(this).code(ICheckResult.OK).description("All average times under " + limitWarn + "ms")
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("Reading statistics has failed", e);
            result = result(this).code(ICheckResult.ERROR).fatal().description("Reading statistics has failed").build();
        }

        return result;
    }

    @Override
    public String getGroup() {
        return "Routes";
    }

    @Override
    public String getName() {
        return "Performance";
    }

    @Override
    public void setConfiguration(Properties configuration) {
        super.setConfiguration(configuration);

        String error = getConfiguration().getProperty("routesPerformanceCheck.limitError");
        if (error != null) {
            limitError = Integer.valueOf(error);
        }

        String warn = getConfiguration().getProperty("routesPerformanceCheck.limitWarn");
        if (warn != null) {
            limitWarn = Integer.valueOf(warn);
        }

        String delai = getConfiguration().getProperty("routesPerformanceCheck.delay");
        if (delai != null) {
            this.delay = Integer.valueOf(delai);
        }
    }

}
