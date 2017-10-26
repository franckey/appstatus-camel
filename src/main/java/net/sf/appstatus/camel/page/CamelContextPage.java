package net.sf.appstatus.camel.page;

import static net.sf.appstatus.web.HtmlUtils.applyLayout;
import static net.sf.appstatus.web.HtmlUtils.generateBeginTable;
import static net.sf.appstatus.web.HtmlUtils.generateEndTable;
import static net.sf.appstatus.web.HtmlUtils.generateHeaders;
import static net.sf.appstatus.web.HtmlUtils.generateRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.appstatus.camel.StatusCamelException;
import net.sf.appstatus.camel.utils.CamelStats;
import net.sf.appstatus.camel.utils.JMXHelper;
import net.sf.appstatus.web.StatusWebHandler;
import net.sf.appstatus.web.pages.AbstractPage;

public class CamelContextPage extends AbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelContextPage.class);

    private static final String MSG_ERROR = "<div class='alert alert-danger'><strong>Can't access stats  :</strong> %s</div>";
    private static final String PAGE_ERROR_LAYOUT = "camelErrorLayout.html";
    private String contentLayout = "camelContentLayout.html";
    private Map<String, String> additionnalInformation;

    @Override
    public void doGet(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        setup(resp, "text/html");
        ServletOutputStream os = resp.getOutputStream();

        Map<String, String> valuesMap = new HashMap<>();
        // generating content
        String content = null;
        try {
            if (MapUtils.isNotEmpty(additionnalInformation)) {
                for (Entry<String, String> entry : additionnalInformation.entrySet()) {
                    valuesMap.put(entry.getKey(), entry.getValue());
                }
            }
            content = generateContent(valuesMap);
        } catch (StatusCamelException e) {
            LOGGER.error("Reading statistics has failed", e);
            content = generateContentError(valuesMap, e.getMessage());
        }

        valuesMap.clear();
        valuesMap.put("content", content);
        // generating page
        os.write(getPage(webHandler, valuesMap).getBytes(StandardCharsets.UTF_8.name()));
    }

    @Override
    public void doPost(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp) {
        try {
            doGet(webHandler, req, resp);
        } catch (IOException e) {
            LOGGER.error("error occured", e);
        }
    }

    private String generateContentError(Map<String, String> valuesMap, String msg) throws IOException {
        valuesMap.put("errors", String.format(MSG_ERROR, msg));
        return applyLayout(valuesMap, PAGE_ERROR_LAYOUT);
    }

    private String generateContent(Map<String, String> valuesMap) throws StatusCamelException, IOException {
        valuesMap.put("contextsTable", generateContextTable());
        valuesMap.put("routesTable", generateRouteTable());
        return applyLayout(valuesMap, contentLayout);
    }

    private String generateContextTable() throws StatusCamelException {
        try {
            StrBuilder table = new StrBuilder();
            List<ManagedCamelContextMBean> contexts = JMXHelper.getRegisteredContexts();
            if (generateBeginTable(table, contexts.size())) {
                generateHeaders(table, CamelStats.HEADERS_CONTEXT);
                for (ManagedCamelContextMBean stats : contexts) {
                    generateRow(table, CamelStats.getContextStatus(stats), CamelStats.getContextStats(stats));
                }
                generateEndTable(table, contexts.size());
            }
            return table.toString();
        } catch (Exception e) {
            throw new StatusCamelException(e);
        }
    }

    private String generateRouteTable() throws StatusCamelException {
        try {
            StrBuilder table = new StrBuilder();
            List<ManagedRouteMBean> routes = JMXHelper.getRegisteredRoutes();
            if (CollectionUtils.isNotEmpty(routes) && generateBeginTable(table, routes.size())) {
                generateHeaders(table, CamelStats.HEADERS_ROUTE);
                for (ManagedRouteMBean stats : routes) {
                    generateRow(table, CamelStats.getRouteStatus(stats), CamelStats.getRouteStats(stats));
                }
                generateEndTable(table, routes.size());
            }
            return table.toString();
        } catch (Exception e) {
            throw new StatusCamelException(e);
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

    public void setContentLayout(String contentLayout) {
        this.contentLayout = contentLayout;
    }

    public void setAdditionnalInformation(Map<String, String> additionnalInformation) {
        this.additionnalInformation = additionnalInformation;
    }

}
