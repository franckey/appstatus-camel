package net.sf.appstatus.camel.check;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.appstatus.camel.utils.JMXHelper;
import net.sf.appstatus.core.check.AbstractCheck;
import net.sf.appstatus.core.check.ICheckResult;

public class RoutesAliveCheck extends AbstractCheck {

    private static final String BALISE_BR = "<br/>";

    private static final String TRANSFERT_SFTP = "transfert-sftp";

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesAliveCheck.class);

    private int delay = 1;
    private int limitWarn = 18000000;
    private int limitError = 720000000;

    @Override
    public ICheckResult checkStatus() {
        ICheckResult result = null;
        try {

            List<String> warns = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            List<ManagedRouteMBean> routes = JMXHelper.getRegisteredRoutes();
            for (ManagedRouteMBean route : routes) {

                if (!route.getState().equals(ServiceStatus.Started.name())
                        && !route.getRouteId().startsWith(TRANSFERT_SFTP)) {
                    errors.add("Route <b>" + route.getCamelId() + "#" + route.getRouteId() + "</b> is not Started");
                } else {

                    // Do not report if hit count is too small
                    if (this.delay > route.getExchangesTotal()) {
                        continue;
                    }

                    long now = System.currentTimeMillis();
                    Date dateError = new Date(now - limitError);
                    Date dateWarn = new Date(now - limitWarn);
                    Date lastProceed = getLastProceed(route.getLastExchangeCompletedTimestamp(),
                            route.getLastExchangeFailureTimestamp());

                    if (lastProceed.before(dateError)) {
                        errors.add("Route <b>" + route.getCamelId() + "#" + route.getRouteId()
                                + "</b> last exchange completed " + lastProceed + " is older error limit (" + limitError
                                + "ms)");
                    } else if (lastProceed.before(dateWarn)) {
                        warns.add("Route <b>" + route.getCamelId() + "#" + route.getRouteId()
                                + "</b> last exchange completed " + lastProceed + " is older warn limit (" + limitWarn
                                + "ms)");
                    }
                }

            }

            if (CollectionUtils.isNotEmpty(errors)) {
                String description = StringUtils.join(errors, BALISE_BR);
                if (CollectionUtils.isNotEmpty(warns)) {
                    description = description + " <br/>Additional warnings: " + StringUtils.join(warns, BALISE_BR);
                }
                result = result(this).code(ICheckResult.ERROR).fatal().description(description).build();
            } else if (CollectionUtils.isNotEmpty(warns)) {
                result = result(this).code(ICheckResult.ERROR).description(StringUtils.join(warns, BALISE_BR)).build();
            } else {
                result = result(this).code(ICheckResult.OK)
                        .description("At least one exchange proceeded on each route these last " + limitWarn + "ms")
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("Reading statistics has failed", e);
            result = result(this).code(ICheckResult.ERROR).fatal().description("Reading statistics has failed").build();
        }

        return result;
    }

    public static Date getLastProceed(Date lastCompleted, Date lastFailure) {
        if (lastCompleted != null && lastFailure != null) {
            return lastCompleted.after(lastFailure) ? lastCompleted : lastFailure;
        }

        if (lastCompleted == null) {
            return lastFailure;
        }

        return lastCompleted;
    }

    @Override
    public String getGroup() {
        return "Routes";
    }

    @Override
    public String getName() {
        return "Alive";
    }

    @Override
    public void setConfiguration(Properties configuration) {
        super.setConfiguration(configuration);

        String error = getConfiguration().getProperty("routesAliveCheck.limitError");
        if (error != null) {
            limitError = Integer.valueOf(error);
        }

        String warn = getConfiguration().getProperty("routesAliveCheck.limitWarn");
        if (warn != null) {
            limitWarn = Integer.valueOf(warn);
        }

        String delai = getConfiguration().getProperty("routesAliveCheck.delay");
        if (delai != null) {
            this.delay = Integer.valueOf(delai);
        }
    }

}
