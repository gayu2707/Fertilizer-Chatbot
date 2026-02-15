package com.example.chat;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.*;
import java.time.LocalDateTime;

public class ReportServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String issue = request.getParameter("issue");

        if (issue == null || issue.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("No issue provided.");
            return;
        }

        File dir = new File(getServletContext().getRealPath("/issues"));
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "issues.txt");

        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(LocalDateTime.now() + " - " + issue);
        }

        response.setContentType("text/plain");
        response.getWriter().write("✅ Issue reported successfully. Saved at: " + file.getAbsolutePath());
    }
}

