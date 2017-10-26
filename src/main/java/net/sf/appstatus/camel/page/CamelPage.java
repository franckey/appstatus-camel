package net.sf.appstatus.camel.page;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.appstatus.camel.StatusCamelException;
import net.sf.appstatus.camel.utils.CamelStats;
import net.sf.appstatus.camel.utils.JMXHelper;
import net.sf.appstatus.web.HtmlUtils;
import net.sf.appstatus.web.StatusWebHandler;

public class CamelPage extends CamelContextPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelPage.class);
    private static final String PAGECONTENTLAYOUT = "customCamelContentLayout.html";

    @PostConstruct
    public void init() {
        // Nothing to do
    }

    @Override
    public void doGet(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Map<String, String> valuesMap = new HashMap<>();

        StrBuilder togglzTable = buildTogglzTable();
        valuesMap.put("togglzTable", togglzTable.toString());

        setAdditionnalInformation(valuesMap);
        setContentLayout(PAGECONTENTLAYOUT);

        super.doGet(webHandler, req, resp);

    }

    private StrBuilder buildTogglzTable() throws IOException {
        StrBuilder sbTable = new StrBuilder();

        try {
            List<ManagedRouteMBean> routes = JMXHelper.getRegisteredRoutes();
            if (HtmlUtils.generateBeginTable(sbTable, routes.size())) {
                HtmlUtils.generateHeaders(sbTable, "", "RouteId", "Action");
                for (ManagedRouteMBean route : routes) {
                    HtmlUtils.generateRow(sbTable, CamelStats.getRouteStatus(route), route.getRouteId(),
                            "<form action='?p=Camel#tooglz' method='post'>" //
                                    + "<input type='hidden' name='contextId' value='" + route.getCamelId() + "'/>" //
                                    + "<input type='hidden' name='routeId' value='" + route.getRouteId() + "'/>" //
                                    + "<input type='submit' class='btn' value='togglz' /></form>");
                }
                HtmlUtils.generateEndTable(sbTable, routes.size());
            }
        } catch (StatusCamelException e) {
            LOGGER.error("error occured", e);
        }

        return sbTable;

    }

    @Override
    public void doPost(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp) {
        String routeId = req.getParameter("routeId");
        String contextId = req.getParameter("contextId");

        if (StringUtils.isNotBlank(contextId) && StringUtils.isNotBlank(routeId)) {
            try {
                ManagedRouteMBean route = JMXHelper.getRegisteredRoute(contextId, routeId);
                if (route != null) {

                    ServiceStatus status = ServiceStatus.valueOf(route.getState());
                    if (status.isStarted() || status.isStarting()) {
                        route.stop();
                    } else if (status.isStopped() || status.isStopping()) {
                        route.start();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error occured", e);
            }
        }
    }

    @Override
    public String getId() {
        return "Camel";
    }

    @Override
    public String getName() {
        return "Camel";
    }
}
