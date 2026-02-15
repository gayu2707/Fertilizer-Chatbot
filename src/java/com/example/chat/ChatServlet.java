package com.example.chat;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.util.*;

public class ChatServlet extends HttpServlet {

    private final List<Map<String, String>> dataset = new ArrayList<>();

    @Override
    public void init() {
        try {
            String filePath = getServletContext().getRealPath("/WEB-INF/Fertilizer dataset.csv");
            System.out.println("📂 Loading dataset from: " + filePath);
            loadDataset(filePath);
            System.out.println("✅ Loaded " + dataset.size() + " records successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = Optional.ofNullable(request.getParameter("message")).orElse("").toLowerCase().trim();
        HttpSession session = request.getSession(true);
        String lastCrop = (String) session.getAttribute("lastCrop");

        String reply;
        try {
            reply = getBotReply(message, lastCrop, session);
        } catch (Exception e) {
            e.printStackTrace();
            reply = "⚠️ Oops! Something went wrong while processing your question.";
        }

        PrintWriter out = response.getWriter();
        out.print("{\"reply\":\"" + reply.replace("\"", "\\\"") + "\"}");
        out.flush();
    }

    private void loadDataset(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split(",", -1);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim().toLowerCase(), values[i].trim());
                }
                if (row.containsKey("crop"))
                    row.put("crop", row.get("crop").toLowerCase());
                dataset.add(row);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error loading dataset: " + e.getMessage());
        }
    }

    private String getBotReply(String message, String lastCrop, HttpSession session) {

        if (message.isEmpty())
            return "💬 Hi farmer! You can ask things like 'pH for rice' or 'fertilizer for maize'.";

        if (message.matches(".*\\b(hi|hello|hey)\\b.*"))
            return "👋 Hey there! I’m your FarmBot assistant. Ask me about soil, pH, weather, or fertilizers for any crop.";

        if (message.matches(".*\\b(bye|goodbye|see you)\\b.*"))
            return "🌾 Take care! Wishing you healthy crops and good yield.";
        if (message.matches(".*\\b(who are you|what are you)\\b.*"))
            return "I'm a Fertilizer Chatbot.";

        List<String> cropsInMessage = findCropsInMessage(message);
        if (cropsInMessage.size() >= 2)
            return compareCrops(cropsInMessage.get(0), cropsInMessage.get(1), message);

        String crop = cropsInMessage.isEmpty() ? lastCrop : cropsInMessage.get(0);
        if (crop == null || crop.isEmpty())
            return "🌱 Please mention a crop name, like rice, maize, sugarcane, or wheat.";

        session.setAttribute("lastCrop", crop);
        Map<String, String> row = findCropData(crop);
        if (row == null)
            return "❌ Hmm, I don’t have data for " + crop + ". Try another crop!";

        if (containsAny(message, "fertilizer", "dose", "amount", "quantity"))
            return "🌿 For " + crop + ", use around " + safe(row.get("urea(kg)")) + " kg of Urea, "
                    + safe(row.get("ssp(kg)")) + " kg of SSP, and "
                    + safe(row.get("mop(kg)")) + " kg of MOP. Apply in 2–3 splits for better results.";

        if (containsAny(message, "npk", "recommend", "rec"))
            return "📊 The suggested NPK ratio for " + crop + " is N:"
                    + safe(row.get("rec_n")) + ", P:" + safe(row.get("rec_p"))
                    + ", K:" + safe(row.get("rec_k")) + ". It helps the crop grow evenly.";

        if (containsAny(message, "ph", "acidity"))
            return "⚗️ The ideal pH for " + crop + " is around " + safe(row.get("pH"))
                    + ". Try to maintain this level for proper nutrient uptake.";

        if (containsAny(message, "soil"))
            return "🌍 " + crop + " does best in " + safe(row.get("soil_type"))
                    + " soil. Keeping soil moisture balanced will help it thrive.";

        if (containsAny(message, "weather", "rain", "climate"))
            return "🌦️ " + crop + " grows well under " + safe(row.get("weather"))
                    + " conditions. Avoid too much drought or flooding.";

        if (containsAny(message, "nutrient", "available"))
            return "🔬 The available nutrients for " + crop + " are N:"
                    + safe(row.get("available_n")) + ", P:" + safe(row.get("available_p"))
                    + ", K:" + safe(row.get("available_k")) + ". Keep an eye on soil health.";

        if (containsAny(message, "note", "tip", "instruction", "info"))
            return "📘 Tip for " + crop + ": " + safe(row.get("notes"))
                    + ". A simple practice for better yield.";

        return "📋 Here’s a quick overview of " + crop + ": It grows well in "
                + safe(row.get("soil_type")) + " soil with a pH of " + safe(row.get("pH"))
                + ". Recommended NPK is " + safe(row.get("rec_n")) + "-"
                + safe(row.get("rec_p")) + "-" + safe(row.get("rec_k")) + ".";
    }

    private List<String> findCropsInMessage(String message) {
        List<String> crops = new ArrayList<>();
        for (Map<String, String> row : dataset) {
            String crop = row.get("crop");
            if (crop != null && message.contains(crop) && !crops.contains(crop)) {
                crops.add(crop);
            }
        }
        return crops;
    }

    private String compareCrops(String crop1, String crop2, String message) {
        Map<String, String> data1 = findCropData(crop1);
        Map<String, String> data2 = findCropData(crop2);
        if (data1 == null || data2 == null)
            return "⚠️ I couldn’t find complete data for both crops.";

        if (containsAny(message, "ph"))
            return "⚗️ Comparing pH: " + crop1 + " likes around " + safe(data1.get("pH"))
                    + ", while " + crop2 + " prefers about " + safe(data2.get("pH")) + ".";

        if (containsAny(message, "npk", "nutrient"))
            return "📊 " + crop1 + " NPK → " + safe(data1.get("rec_n")) + "-" + safe(data1.get("rec_p")) + "-"
                    + safe(data1.get("rec_k")) + ", while " + crop2 + " → "
                    + safe(data2.get("rec_n")) + "-" + safe(data2.get("rec_p")) + "-"
                    + safe(data2.get("rec_k")) + ".";

        return "🌾 " + crop1 + " grows best in " + safe(data1.get("soil_type"))
                + " soil, whereas " + crop2 + " prefers " + safe(data2.get("soil_type")) + ".";
    }

    private boolean containsAny(String message, String... words) {
        for (String w : words) if (message.contains(w)) return true;
        return false;
    }

    private String safe(String val) {
        return (val == null || val.isEmpty()) ? "N/A" : val;
    }

    private Map<String, String> findCropData(String crop) {
        for (Map<String, String> row : dataset)
            if (row.get("crop").equalsIgnoreCase(crop))
                return row;
        return null;
    }
}
