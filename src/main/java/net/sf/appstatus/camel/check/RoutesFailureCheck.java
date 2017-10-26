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

public class RoutesFailureCheck extends AbstractCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesFailureCheck.class);

    private static final int DEFAULT_LIMIT_ERROR = 10;

    private static final int DEFAULT_LIMIT_WARN = 5;

    private static final String MSG_HTML_ERROR = "Service <b>%s#%s</b> failure ratio (%d%%) is over error limit (%d%%)";

    private static final String HTML_CARRIAGE_RETURN = "<br/>";

    private int limitWarn = DEFAULT_LIMIT_WARN;
    private int limitError = DEFAULT_LIMIT_ERROR;

    @Override
    public ICheckResult checkStatus(Locale locale) {
        ICheckResult result = null;
        try {
            List<String> warns = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            List<ManagedRouteMBean> routes = JMXHelper.getRegisteredRoutes();
            for (ManagedRouteMBean route : routes) {
                if (route.getExchangesTotal() == 0) {
                    continue;
                }

                long failureRatio = (route.getExchangesFailed() * 100) / route.getExchangesTotal();
                if (failureRatio > limitError) {
                    errors.add(String.format(MSG_HTML_ERROR, route.getCamelId(), route.getRouteId(), failureRatio,
                            limitError));
                } else if (failureRatio > limitWarn) {
                    errors.add(String.format(MSG_HTML_ERROR, route.getCamelId(), route.getRouteId(), failureRatio,
                            limitWarn));
                }
            }

            if (CollectionUtils.isNotEmpty(errors)) {
                String description = StringUtils.join(errors, HTML_CARRIAGE_RETURN);
                if (CollectionUtils.isNotEmpty(warns)) {
                    description = description + " <br/>Additional warnings: "
                            + StringUtils.join(warns, HTML_CARRIAGE_RETURN);
                }
                result = result(this).code(ICheckResult.ERROR).fatal().description(description).build();
            } else if (CollectionUtils.isNotEmpty(warns)) {
                result = result(this).code(ICheckResult.ERROR)
                        .description(StringUtils.join(warns, HTML_CARRIAGE_RETURN)).build();
            } else {
                result = result(this).code(ICheckResult.OK).description("All failure ratios under " + limitWarn + "%")
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
        return "Failures";
    }

    @Override
    public void setConfiguration(Properties configuration) {
        super.setConfiguration(configuration);

        String error = getConfiguration().getProperty("routesFailureCheck.limitError");
        if (error != null) {
            limitError = Integer.valueOf(error);
        }

        String warn = getConfiguration().getProperty("routesFailureCheck.limitWarn");
        if (warn != null) {
            limitWarn = Integer.valueOf(warn);
        }
    }
}
