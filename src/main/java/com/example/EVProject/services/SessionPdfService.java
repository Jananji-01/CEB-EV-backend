package com.example.EVProject.services;

import com.example.EVProject.dto.SessionDetailDTO;
import com.example.EVProject.dto.PowerReadingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generateHtmlReport(SessionDetailDTO session) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Charging Session Report</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("h1 { color: #2c3e50; text-align: center; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
        html.append("h2 { color: #34495e; margin-top: 30px; border-left: 4px solid #3498db; padding-left: 15px; }");
        html.append("h3 { color: #34495e; margin-top: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th { background-color: #3498db; color: white; padding: 12px; text-align: left; }");
        html.append("td { border: 1px solid #ddd; padding: 8px; }");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }");
        html.append(".info-table td:first-child { font-weight: bold; width: 30%; background-color: #ecf0f1; }");
        html.append(".total { font-size: 18px; font-weight: bold; color: #27ae60; margin-top: 20px; padding: 15px; background-color: #f0f9f0; border-radius: 5px; }");
        html.append(".footer { text-align: center; margin-top: 50px; font-size: 12px; color: #7f8c8d; border-top: 1px solid #ddd; padding-top: 20px; }");
        html.append(".badge { display: inline-block; padding: 3px 8px; border-radius: 3px; font-size: 12px; font-weight: bold; }");
        html.append(".badge-success { background-color: #27ae60; color: white; }");
        html.append(".badge-warning { background-color: #f39c12; color: white; }");
        html.append("@media print { body { margin: 0; } .no-print { display: none; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Print button
        html.append("<div class='no-print' style='text-align: right; margin-bottom: 20px;'>");
        html.append("<button onclick='window.print();' style='padding: 10px 20px; background-color: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;'>");
        html.append("🖨️ Print / Save as PDF");
        html.append("</button>");
        html.append("</div>");

        // Title
        html.append("<h1>🔋 Charging Session Report</h1>");

        // Session Information
        html.append("<h2>📋 Session Information</h2>");
        html.append("<table class='info-table'>");
        html.append("<tr><td>Session ID:</td><td><strong>").append(session.getSessionId()).append("</strong></td></tr>");
        html.append("<tr><td>Device ID:</td><td>").append(session.getIdDevice() != null ? session.getIdDevice() : "N/A").append("</td></tr>");
        html.append("<tr><td>E-Account Number:</td><td>").append(session.getEAccountNo() != null ? session.getEAccountNo() : "N/A").append("</td></tr>");
        html.append("<tr><td>Start Time:</td><td>").append(session.getStartTime() != null ? session.getStartTime().format(DATE_FORMATTER) : "N/A").append("</td></tr>");
        html.append("<tr><td>End Time:</td><td>").append(session.getEndTime() != null ? session.getEndTime().format(DATE_FORMATTER) : "In Progress").append("</td></tr>");
        html.append("<tr><td>Status:</td><td>");
        if ("COMPLETED".equals(session.getStatus())) {
            html.append("<span class='badge badge-success'>").append(session.getStatus()).append("</span>");
        } else {
            html.append("<span class='badge badge-warning'>").append(session.getStatus()).append("</span>");
        }
        html.append("</td></tr>");
        html.append("<tr><td>Charging Mode:</td><td>").append(session.getChargingMode() != null ? session.getChargingMode() : "N/A").append("</td></tr>");
        html.append("<tr><td>Total Consumption:</td><td><strong>").append(String.format("%.3f kWh", session.getTotalConsumption() != null ? session.getTotalConsumption() : 0)).append("</strong></td></tr>");
        html.append("<tr><td>Amount:</td><td><strong>").append(String.format("Rs. %.2f", session.getAmount() != null ? session.getAmount() : 0)).append("</strong></td></tr>");
        html.append("</table>");

        // Energy Calculation
        if (session.getEnergyCalculation() != null) {
            html.append("<h2>⚡ Energy Calculation Details</h2>");
            html.append("<p><strong>Calculation Method:</strong> ").append(session.getEnergyCalculation().getCalculationMethod()).append("</p>");
            html.append("<p><strong>Total Readings Processed:</strong> ").append(session.getEnergyCalculation().getTotalReadings()).append("</p>");
            html.append("<p><strong>Total Energy Calculated:</strong> <strong>").append(String.format("%.3f kWh", session.getEnergyCalculation().getTotalEnergyKwh())).append("</strong></p>");

            // Sample intervals
            if (session.getEnergyCalculation().getSampleIntervals() != null &&
                    !session.getEnergyCalculation().getSampleIntervals().isEmpty()) {
                html.append("<h3>📊 Sample Calculation Intervals (First 10)</h3>");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>#</th><th>Start Time</th><th>End Time</th><th>Avg Power (W)</th><th>ΔTime (hr)</th><th>Energy (Wh)</th><th>Cumulative (kWh)</th>");
                html.append("</tr></thead><tbody>");

                int count = 1;
                for (var interval : session.getEnergyCalculation().getSampleIntervals()) {
                    html.append("<tr>");
                    html.append("<td>").append(count++).append("</td>");
                    html.append("<td>").append(interval.getStartTime() != null ? interval.getStartTime().format(DATE_FORMATTER) : "N/A").append("</td>");
                    html.append("<td>").append(interval.getEndTime() != null ? interval.getEndTime().format(DATE_FORMATTER) : "N/A").append("</td>");
                    html.append("<td>").append(String.format("%.2f", interval.getAvgPower())).append("</td>");
                    html.append("<td>").append(String.format("%.6f", interval.getTimeDeltaHours())).append("</td>");
                    html.append("<td>").append(String.format("%.6f", interval.getEnergyWh())).append("</td>");
                    html.append("<td>").append(String.format("%.6f", interval.getCumulativeKwh())).append("</td>");
                    html.append("</tr>");
                }
                html.append("</tbody></table>");
            }
        }

        // Power Readings
        if (session.getPowerReadings() != null && !session.getPowerReadings().isEmpty()) {
            html.append("<h2>📈 Power Readings</h2>");
            html.append("<table>");
            html.append("<thead><tr>");
            html.append("<th>#</th><th>Timestamp</th><th>Active Power (W)</th><th>Energy (Wh)</th><th>Cumulative (kWh)</th>");
            html.append("</tr></thead><tbody>");

            List<PowerReadingDTO> readings = session.getPowerReadings();
            int displayCount = Math.min(20, readings.size());

            for (int i = 0; i < displayCount; i++) {
                PowerReadingDTO reading = readings.get(i);
                html.append("<tr>");
                html.append("<td>").append(i + 1).append("</td>");
                html.append("<td>").append(reading.getTimestamp() != null ? reading.getTimestamp().format(DATE_FORMATTER) : "N/A").append("</td>");
                html.append("<td>").append(String.format("%.3f", reading.getActivePower())).append("</td>");
                html.append("<td>").append(String.format("%.6f", reading.getEnergyWh())).append("</td>");
                html.append("<td>").append(String.format("%.6f", reading.getCumulativeEnergyKwh())).append("</td>");
                html.append("</tr>");
            }

            if (readings.size() > 20) {
                html.append("<tr><td colspan='5' style='text-align:center; background-color:#f9f9f9;'>");
                html.append("... and ").append(readings.size() - 20).append(" more readings");
                html.append("</td></tr>");
            }
            html.append("</tbody></table>");
        }

        // Summary
        html.append("<div class='total'>");
        html.append("<h2>📊 Summary</h2>");
        html.append("<p><strong>🔋 Total Energy Consumed:</strong> ").append(String.format("%.3f kWh", session.getTotalConsumption() != null ? session.getTotalConsumption() : 0)).append("</p>");
        html.append("<p><strong>💰 Total Amount:</strong> Rs. ").append(String.format("%.2f", session.getAmount() != null ? session.getAmount() : 0)).append("</p>");

        // Calculate duration if both times exist
        if (session.getStartTime() != null && session.getEndTime() != null) {
            java.time.Duration duration = java.time.Duration.between(session.getStartTime(), session.getEndTime());
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            html.append("<p><strong>⏱️ Session Duration:</strong> ").append(hours).append(" hours, ").append(minutes).append(" minutes, ").append(seconds).append(" seconds</p>");
        }

        // Calculate average power
        if (session.getTotalConsumption() != null && session.getStartTime() != null && session.getEndTime() != null) {
            java.time.Duration duration = java.time.Duration.between(session.getStartTime(), session.getEndTime());
            double hours = duration.toSeconds() / 3600.0;
            if (hours > 0) {
                double avgPower = (session.getTotalConsumption() * 1000) / hours;
                html.append("<p><strong>⚡ Average Power:</strong> ").append(String.format("%.2f", avgPower)).append(" W</p>");
            }
        }

        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Generated by EV Charging System on ").append(java.time.LocalDateTime.now().format(DATE_FORMATTER)).append("</p>");
        html.append("<p><em>Users can save this report as PDF using browser's Print > Save as PDF functionality</em></p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }
}