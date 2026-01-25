package org.openlmis.web.view.excel.requisition;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.GeographicZone;
import org.openlmis.core.service.ConfigurationSettingService;
import org.openlmis.core.service.MessageService;
import org.openlmis.rnr.domain.*;
import org.openlmis.web.model.PrintRnrLineItem;
import org.openlmis.order.service.OrderService;
import org.openlmis.core.service.ProgramService;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static org.openlmis.web.controller.RequisitionController.LOSSES_AND_ADJUSTMENT_TYPES;
import static org.openlmis.web.controller.RequisitionController.NUMBER_OF_MONTHS;
import static org.openlmis.web.controller.RequisitionController.RNR_TEMPLATE;
import static org.openlmis.web.controller.RequisitionController.STATUS_CHANGES;

public class RequisitionExcelWriter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private final Workbook workbook;
    private final ByteArrayOutputStream stream;
    private final MessageService messageService;
    private final ConfigurationSettingService configService;
    private final OrderService orderService;
    private final ProgramService programService;

    public RequisitionExcelWriter(Workbook workbook, ByteArrayOutputStream stream,
                                  MessageService messageService,
                                  ConfigurationSettingService configService,
                                  OrderService orderService,
                                  ProgramService programService) {
        this.workbook = workbook;
        this.stream = stream;
        this.messageService = messageService;
        this.configService = configService;
        this.orderService = orderService;
        this.programService = programService;
    }

    public void buildWith(Map<String, Object> model) throws IOException {
        Rnr requisition = (Rnr) model.get("rnr");
        List<Column> columns = (List<Column>) model.get(RNR_TEMPLATE);
        List<RnrLineItem> fullSupplyLineItems = requisition.getFullSupplyLineItems();
        List<RnrLineItem> nonFullSupplyLineItems = requisition.getNonFullSupplyLineItems();
        List<RegimenLineItem> regimenLineItems = requisition.getRegimenLineItems();
        List<RequisitionStatusChange> statusChanges = (List<RequisitionStatusChange>) model.get(STATUS_CHANGES);
        List<LossesAndAdjustmentsType> lossTypes = (List<LossesAndAdjustmentsType>) model.get(LOSSES_AND_ADJUSTMENT_TYPES);
        Integer numberOfMonths = (Integer) model.get(NUMBER_OF_MONTHS);
        String currency = messageService.message("label.currency.symbol");

        if (!configService.getBoolValue("RNR_PRINT_REPEAT_CURRENCY_SYMBOL")) {
            currency = "";
        }

        Sheet sheet = workbook.createSheet("Full Supply Products");
        int rowCount = 0;
        rowCount = writeHeader(sheet, rowCount, requisition);

        Row header = sheet.createRow(rowCount++);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i).getLabel());
            cell.setCellStyle(boldStyle());
        }

        for (RnrLineItem item : fullSupplyLineItems) {
            PrintRnrLineItem printRnrLineItem = new PrintRnrLineItem(item);

            printRnrLineItem.calculate(columns, lossTypes, numberOfMonths, requisition.getStatus());

//            item.calculate(columns, lossTypes, numberOfMonths, requisition.getStatus());
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < columns.size(); i++) {
                Column col = columns.get(i);
                try {

                    String val = "mos".equalsIgnoreCase(col.getName()) ? item.getMos() : item.getValue(col.getName());
                    writeTypedCell(row, i, col.getColumnType(), val, currency);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        // Non-Full Supply Sheet
        if (!nonFullSupplyLineItems.isEmpty()) {
            Sheet nonFullSheet = workbook.createSheet("Non-Full Supply Products");
            int nrow = 0;
            Row nfHeader = nonFullSheet.createRow(nrow++);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = nfHeader.createCell(i);
                cell.setCellValue(columns.get(i).getLabel());
                cell.setCellStyle(boldStyle());
            }

            for (RnrLineItem item : nonFullSupplyLineItems) {
//                item.calculate(columns, lossTypes, numberOfMonths, requisition.getStatus());
                PrintRnrLineItem printRnrLineItem = new PrintRnrLineItem(item);

                printRnrLineItem.calculate(columns, lossTypes, numberOfMonths, requisition.getStatus());

                Row row = nonFullSheet.createRow(nrow++);
                for (int i = 0; i < columns.size(); i++) {
                    Column col = columns.get(i);
                    try {
                        String val = "mos".equalsIgnoreCase(col.getName()) ? item.getMos() : item.getValue(col.getName());
                        writeTypedCell(row, i, col.getColumnType(), val, currency);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        // Regimen Sheet
        if (!regimenLineItems.isEmpty()) {
            Sheet regimenSheet = workbook.createSheet("Regimens");
            Row rh = regimenSheet.createRow(0);
            rh.createCell(0).setCellValue("Code");
            rh.createCell(1).setCellValue("Name");
            rh.createCell(2).setCellValue("Valeurs");
            rh.createCell(3).setCellValue("Remarques");

            int r = 1;
            for (RegimenLineItem ri : regimenLineItems) {
                Row row = regimenSheet.createRow(r++);
                row.createCell(0).setCellValue(ri.getCode());
                row.createCell(1).setCellValue(ri.getName());
                row.createCell(2).setCellValue(ri.getName());
//                row.createCell(2).setCellValue(String.valueOf(ri.getValue()));
                row.createCell(3).setCellValue(ri.getRemarks());
            }
        }

        // Summary Sheet
        Sheet summarySheet = workbook.createSheet("Summary");
        int s = 0;
        requisition.fillFullSupplyCost();
        requisition.fillNonFullSupplyCost();

        DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
        summarySheet.createRow(s++).createCell(0).setCellValue("Allocated Budget");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(requisition.getAllocatedBudget() == null
          ? messageService.message("msg.budget.not.allocated")
          : currency + moneyFormat.format(requisition.getAllocatedBudget()));

        summarySheet.createRow(s++).createCell(0).setCellValue("Total Cost For Full Supply Items");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(currency + moneyFormat.format(requisition.getFullSupplyItemsSubmittedCost().toDecimal()));

        summarySheet.createRow(s++).createCell(0).setCellValue("Total Cost For Non Full Supply Items");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(currency + moneyFormat.format(requisition.getNonFullSupplyItemsSubmittedCost().toDecimal()));

        summarySheet.createRow(s++).createCell(0).setCellValue("Total Cost");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(currency + moneyFormat.format(
          requisition.getFullSupplyItemsSubmittedCost().toDecimal()
            .add(requisition.getNonFullSupplyItemsSubmittedCost().toDecimal())));

        RequisitionStatusChange submitted = statusChanges.stream().filter(sc -> sc.getStatus() == RnrStatus.SUBMITTED).findFirst().orElse(null);
        RequisitionStatusChange authorized = statusChanges.stream().filter(sc -> sc.getStatus() == RnrStatus.AUTHORIZED).findFirst().orElse(null);

        summarySheet.createRow(s++).createCell(0).setCellValue("Submitted By");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(
          submitted != null ? submitted.getCreatedBy().getFullName() : "");
        summarySheet.createRow(s++).createCell(0).setCellValue("Date");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(
          submitted != null ? DATE_FORMAT.format(submitted.getCreatedDate()) : "");

        summarySheet.createRow(s++).createCell(0).setCellValue("Authorized By");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(
          authorized != null ? authorized.getCreatedBy().getFullName() : "");
        summarySheet.createRow(s++).createCell(0).setCellValue("Date");
        summarySheet.getRow(s - 1).createCell(1).setCellValue(
          authorized != null ? DATE_FORMAT.format(authorized.getCreatedDate()) : "");

        workbook.write(stream);
        workbook.close();
    }

    private int writeHeader(Sheet sheet, int rowNum, Rnr requisition) {
        Facility facility = requisition.getFacility();
        GeographicZone zone = facility.getGeographicZone();
        GeographicZone parent = zone.getParent();

        Row title = sheet.createRow(rowNum++);
        title.createCell(0).setCellValue("Report and Requisition for: " + requisition.getProgram().getName()
          + " (" + requisition.getStatus().name() + ")");

        Row f1 = sheet.createRow(rowNum++);
        f1.createCell(0).setCellValue("Facility: " + facility.getName());
        f1.createCell(1).setCellValue("Operated by: " + facility.getOperatedBy().getText());
        f1.createCell(2).setCellValue("Maximum Stock Level: " + facility.getFacilityType().getNominalMaxMonth());
        f1.createCell(3).setCellValue("Emergency Order Point: " + facility.getFacilityType().getNominalEop());

        Row f2 = sheet.createRow(rowNum++);
        f2.createCell(0).setCellValue("Facility code: " + facility.getCode());
        f2.createCell(1).setCellValue(zone.getLevel().getName() + ": " + zone.getName());
        f2.createCell(2).setCellValue(parent.getLevel().getName() + ": " + parent.getName());
        f2.createCell(3).setCellValue("Reporting Period: " +
          DATE_FORMAT.format(requisition.getPeriod().getStartDate()) + " - " + DATE_FORMAT.format(requisition.getPeriod().getEndDate()));

        Row f3 = sheet.createRow(rowNum++);
        f3.createCell(0).setCellValue("Type: " +
          (requisition.isEmergency() ? messageService.message("requisition.type.emergency")
            : messageService.message("requisition.type.regular")));
        // Add order number next to type
        String orderNumber = orderService.getOrderNumberConfiguration()
            .getOrderNumberFor(requisition.getId(), programService.getById(requisition.getProgram().getId()), requisition.isEmergency());
        f3.createCell(1).setCellValue("Order Number: " + orderNumber);

        return rowNum + 1;
    }

    private void writeTypedCell(Row row, int col, ColumnType type, String value, String currency) {
        DecimalFormat moneyFormatter = new DecimalFormat("#,##0.00");
        DecimalFormat numberFormatter = new DecimalFormat("#,##0");
        Cell cell = row.createCell(col);

        if (!StringUtils.hasText(value)) {
            cell.setCellValue("");
            return;
        }

        switch (type) {
            case TEXT:
                cell.setCellValue(value);
                break;
            case BOOLEAN:
                cell.setCellValue(Boolean.parseBoolean(value));
                break;
            case NUMERIC:
                if (value.matches("^-?\\d+(\\.\\d+)?$"))
                    cell.setCellValue(Double.parseDouble(value));
                else
                    cell.setCellValue(value);
                break;
            case CURRENCY:
                if (value.matches("^-?\\d+(\\.\\d+)?$"))
                    cell.setCellValue(currency + moneyFormatter.format(Double.parseDouble(value)));
                else
                    cell.setCellValue(currency);
                break;
        }
    }

    private CellStyle boldStyle() {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }
}
