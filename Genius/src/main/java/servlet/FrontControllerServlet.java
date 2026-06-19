package servlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

import util.Utilitaire;

public class FrontControllerServlet extends HttpServlet {
    
    private List<String> listContr;
    private Utilitaire util;

    public void init() {
        try {
            this.util = new Utilitaire();
            this.listContr = util.findController("test");

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    protected void processRequest(HttpServletRequest req,
                                  HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String url = req.getRequestURI();

        output(url, req, resp);

        for (String nom : listContr) {
            resp.getWriter().println("\n"+nom) ;
        }
    }

    private void output(String url,
                        HttpServletRequest req,
                        HttpServletResponse resp)
            throws ServletException, IOException {


        resp.getWriter().println(url);
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