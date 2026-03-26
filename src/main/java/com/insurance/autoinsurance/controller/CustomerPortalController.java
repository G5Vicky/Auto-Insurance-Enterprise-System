package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PaymentRepository;
import com.insurance.autoinsurance.repository.PolicyRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/portal")
public class CustomerPortalController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final PolicyRepository  polRepo;
    private final PaymentRepository payRepo;

    public CustomerPortalController(PolicyRepository polRepo, PaymentRepository payRepo) {
        this.polRepo = polRepo; this.payRepo = payRepo;
    }

    /** Landing page — shown in new tab */
    @GetMapping({"", "/"})
    public String portal() { return "portal/index"; }

    /** AJAX: fetch policy details by ID */
    @GetMapping(value = "/api/lookup", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> lookup(@RequestParam Long policyId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<Policy> opt = polRepo.findById(policyId);
        if (opt.isEmpty()) {
            result.put("found", false);
            result.put("message", "No policy found with ID #" + policyId);
            return result;
        }
        Policy p = opt.get();
        // Fetch payments with policy (safe — within transaction)
        List<Payment> payments = payRepo.findByPolicy_PolicyIdOrderByPaymentDateDesc(policyId);
        double totalPaid = payments.stream()
            .filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS)
            .mapToDouble(pay -> pay.getPaymentAmount() != null ? pay.getPaymentAmount() : 0)
            .sum();

        result.put("found",             true);
        result.put("policyId",          p.getPolicyId());
        result.put("policyName",        p.getPolicyName());
        result.put("holderName",        p.getPolicyHolderName());
        result.put("vehicleNumber",     p.getVehicleNumber());
        result.put("vehicleModel",      p.getVehicleModel());
        result.put("coverageType",      p.getCoverageType());
        result.put("policyStatus",      p.getPolicyStatus().getLabel());
        result.put("policyStartDate",   p.getPolicyStartDate() != null ? p.getPolicyStartDate().format(FMT) : "—");
        result.put("policyEndDate",     p.getPolicyEndDate()   != null ? p.getPolicyEndDate().format(FMT)   : "—");
        result.put("annualPremium",     p.getPolicyAmount());
        result.put("totalPayments",     payments.size());
        result.put("totalPaid",         totalPaid);
        result.put("successPayments",   payments.stream().filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS).count());
        result.put("failedPayments",    payments.stream().filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.FAILED).count());
        result.put("holderEmail",       p.getHolderEmail());
        result.put("holderPhone",       p.getHolderPhone());
        return result;
    }

    /** Generate and download PDF payment history */
    @GetMapping("/download-pdf")
    public void downloadPdf(@RequestParam Long policyId, HttpServletResponse response)
            throws IOException, DocumentException {
        Policy p = polRepo.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy #" + policyId + " not found."));
        List<Payment> payments = payRepo.findByPolicy_PolicyIdOrderByPaymentDateDesc(policyId)
            .stream()
            .filter(pay -> pay.getPaymentDate() != null
                && !pay.getPaymentDate().isAfter(LocalDate.now()))
            .collect(Collectors.toList());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=PaymentHistory_Policy" + policyId + ".pdf");

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        // ── Fonts ──────────────────────────────────────────────────────────
        Font titleFont  = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,  new BaseColor(15, 17, 23));
        Font headFont   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,  new BaseColor(59, 130, 246));
        Font labelFont  = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,  new BaseColor(100, 116, 139));
        Font valueFont  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(30, 41, 59));
        Font thFont     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,  BaseColor.WHITE);
        Font tdFont     = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(30, 41, 59));
        Font footerFont = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, new BaseColor(148, 163, 184));

        // ── Header banner ──────────────────────────────────────────────────
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell();
        bannerCell.setBackgroundColor(new BaseColor(15, 17, 23));
        bannerCell.setPadding(18);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        Paragraph brandPara = new Paragraph("InsureTrack Enterprise", titleFont);
        brandPara.setAlignment(Element.ALIGN_LEFT);
        bannerCell.addElement(brandPara);
        Paragraph subPara = new Paragraph("Payment History Report  |  Confidential", footerFont);
        subPara.setAlignment(Element.ALIGN_LEFT);
        bannerCell.addElement(subPara);
        banner.addCell(bannerCell);
        doc.add(banner);
        doc.add(Chunk.NEWLINE);

        // ── Policy Info box ────────────────────────────────────────────────
        doc.add(new Paragraph("POLICY INFORMATION", headFont));
        doc.add(new LineSeparator(0.5f, 100, new BaseColor(59, 130, 246), Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(6);
        addInfoCell(infoTable, "Policy ID",     "#" + p.getPolicyId(),           labelFont, valueFont);
        addInfoCell(infoTable, "Policy Name",   p.getPolicyName(),               labelFont, valueFont);
        addInfoCell(infoTable, "Holder Name",   p.getPolicyHolderName(),         labelFont, valueFont);
        addInfoCell(infoTable, "Status",        p.getPolicyStatus().getLabel(),  labelFont, valueFont);
        addInfoCell(infoTable, "Vehicle No.",   nvl(p.getVehicleNumber()),       labelFont, valueFont);
        addInfoCell(infoTable, "Vehicle Model", nvl(p.getVehicleModel()),        labelFont, valueFont);
        addInfoCell(infoTable, "Coverage Type", nvl(p.getCoverageType()),        labelFont, valueFont);
        addInfoCell(infoTable, "Annual Premium","₹" + fmt(p.getPolicyAmount()),  labelFont, valueFont);
        addInfoCell(infoTable, "Start Date",    p.getPolicyStartDate() != null ? p.getPolicyStartDate().format(FMT) : "—", labelFont, valueFont);
        addInfoCell(infoTable, "End Date",      p.getPolicyEndDate()   != null ? p.getPolicyEndDate().format(FMT)   : "—", labelFont, valueFont);
        addInfoCell(infoTable, "Phone",         nvl(p.getHolderPhone()),         labelFont, valueFont);
        addInfoCell(infoTable, "Generated On",  LocalDate.now().format(FMT),     labelFont, valueFont);
        doc.add(infoTable);
        doc.add(Chunk.NEWLINE);

        // ── Summary stats ──────────────────────────────────────────────────
        doc.add(new Paragraph("PAYMENT SUMMARY", headFont));
        doc.add(new LineSeparator(0.5f, 100, new BaseColor(59, 130, 246), Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        long success = payments.stream().filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS).count();
        long failed  = payments.stream().filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.FAILED).count();
        long pending = payments.stream().filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.PENDING).count();
        double totalPaid = payments.stream()
            .filter(pay -> pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS)
            .mapToDouble(pay -> pay.getPaymentAmount() != null ? pay.getPaymentAmount() : 0).sum();

        PdfPTable sumTable = new PdfPTable(4);
        sumTable.setWidthPercentage(100);
        sumTable.setSpacingBefore(6);
        addSumCell(sumTable, "Total Transactions", String.valueOf(payments.size()), labelFont, valueFont, new BaseColor(59,130,246,30));
        addSumCell(sumTable, "Total Paid (₹)",     "₹" + fmt(totalPaid),           labelFont, valueFont, new BaseColor(16,185,129,30));
        addSumCell(sumTable, "Successful",          String.valueOf(success),         labelFont, valueFont, new BaseColor(16,185,129,30));
        addSumCell(sumTable, "Failed",              String.valueOf(failed),          labelFont, valueFont, new BaseColor(239,68,68,30));
        doc.add(sumTable);
        doc.add(Chunk.NEWLINE);

        // ── Payments table ─────────────────────────────────────────────────
        doc.add(new Paragraph("TRANSACTION HISTORY", headFont));
        doc.add(new LineSeparator(0.5f, 100, new BaseColor(59, 130, 246), Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable payTable = new PdfPTable(new float[]{1.2f, 2f, 2.5f, 1.8f, 3f});
        payTable.setWidthPercentage(100);
        payTable.setSpacingBefore(6);

        // Header row
        for (String h : new String[]{"#", "Date", "Amount (₹)", "Status", "Remarks"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h, thFont));
            hc.setBackgroundColor(new BaseColor(30, 41, 59));
            hc.setPadding(8); hc.setBorder(Rectangle.NO_BORDER);
            payTable.addCell(hc);
        }

        boolean alt = false;
        for (Payment pay : payments) {
            BaseColor rowBg = alt ? new BaseColor(248, 250, 252) : BaseColor.WHITE;
            BaseColor statusColor = pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS
                ? new BaseColor(16, 185, 129)
                : pay.getPaymentStatus() == Payment.PaymentStatus.FAILED
                    ? new BaseColor(239, 68, 68)
                    : new BaseColor(245, 158, 11);

            addTdCell(payTable, "#" + pay.getPaymentId(), tdFont, rowBg);
            addTdCell(payTable, pay.getPaymentDate() != null ? pay.getPaymentDate().format(FMT) : "—", tdFont, rowBg);
            addTdCell(payTable, "₹" + fmt(pay.getPaymentAmount()), tdFont, rowBg);
            Font sf = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, statusColor);
            PdfPCell sc = new PdfPCell(new Phrase(pay.getPaymentStatus().getLabel(), sf));
            sc.setBackgroundColor(rowBg); sc.setPadding(7); sc.setBorder(Rectangle.BOTTOM);
            sc.setBorderColor(new BaseColor(226, 232, 240)); payTable.addCell(sc);
            addTdCell(payTable, nvl(pay.getRemarks()), tdFont, rowBg);
            alt = !alt;
        }
        doc.add(payTable);

        if (payments.isEmpty()) {
            Paragraph noPay = new Paragraph("No payment records found for this policy.", valueFont);
            noPay.setAlignment(Element.ALIGN_CENTER);
            noPay.setSpacingBefore(20);
            doc.add(noPay);
        }

        // ── Footer ────────────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        doc.add(new LineSeparator(0.5f, 100, new BaseColor(226, 232, 240), Element.ALIGN_LEFT, 0));
        Paragraph footer = new Paragraph(
            "This document was automatically generated by InsureTrack Enterprise Platform. " +
            "Generated on " + LocalDate.now().format(FMT) + ". This is a system-generated report.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6);
        doc.add(footer);

        doc.close();
    }

    private void addInfoCell(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell c = new PdfPCell();
        c.addElement(new Paragraph(label, lf));
        c.addElement(new Paragraph(value, vf));
        c.setPadding(8); c.setBorder(Rectangle.BOX);
        c.setBorderColor(new BaseColor(226, 232, 240));
        c.setBackgroundColor(new BaseColor(249, 250, 251));
        t.addCell(c);
    }

    private void addSumCell(PdfPTable t, String label, String value, Font lf, Font vf, BaseColor bg) {
        PdfPCell c = new PdfPCell();
        c.addElement(new Paragraph(label, lf));
        Font bigVal = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(30, 41, 59));
        c.addElement(new Paragraph(value, bigVal));
        c.setPadding(12); c.setBorder(Rectangle.BOX);
        c.setBorderColor(new BaseColor(226, 232, 240));
        c.setBackgroundColor(bg);
        t.addCell(c);
    }

    private void addTdCell(PdfPTable t, String val, Font f, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(val, f));
        c.setBackgroundColor(bg); c.setPadding(7);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new BaseColor(226, 232, 240));
        t.addCell(c);
    }

    private String nvl(String s) { return s != null && !s.isBlank() ? s : "—"; }
    private String fmt(Double v)  {
        if (v == null) return "0.00";
        return String.format("%,.2f", v);
    }
}
