package servlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import util.RouteMapping;
import util.Utilitaire;

public class FrontControllerServlet extends HttpServlet {
    
    private List<Class<?>> listContr;
    private Map<String, RouteMapping> routes;

    private Utilitaire util;

    public void init() throws ServletException {
        try {
            this.util = new Utilitaire();

            this.listContr = util.findController("test");

            this.routes = util.findRoutes(listContr);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    protected void processRequest(HttpServletRequest req,
                                  HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String url = req.getRequestURI();

        output(url, req, resp);
    }

    private void output(String url,
                        HttpServletRequest req,
                        HttpServletResponse resp)
            throws ServletException, IOException {

        PrintWriter out = resp.getWriter();

        out.println(url);

        boolean lien = false;


        RouteMapping route = routes.get(url);

        if (route != null) {
            out.println("<br>URL existant : " + url + " methode : "
                    + route.getControllerClass() + "->" + route.getMethod().getName());
            lien = true;
        }

        if (!lien) {
            if (listContr == null || listContr.isEmpty()) {
                out.println("<br>Aucun controleur trouve.");
                return;
            }

            out.println("Voila les liens existants : ");
            for (Class<?> clazz : listContr) {
                out.println("<br>Controller : " + clazz.getSimpleName());

                boolean hasMappedMethod = false;

                for (Map.Entry<String, RouteMapping> entry : routes.entrySet()) {
                    RouteMapping route1 = entry.getValue();
                    out.println("<br>URL existant : " + entry.getKey()
                            + " methode : " + route1.getMethod().getName());
                }

                if (!hasMappedMethod) {
                    out.println("<br>Aucune methode annotee @UrlMapping");
                }
            }
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        processRequest(req, resp);
    }

}