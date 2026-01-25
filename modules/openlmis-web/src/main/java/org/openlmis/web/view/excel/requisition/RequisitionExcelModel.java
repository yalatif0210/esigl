package org.openlmis.web.view.excel.requisition;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.openlmis.core.domain.Money;
import org.openlmis.core.service.ConfigurationSettingService;
import org.openlmis.core.service.MessageService;
import org.openlmis.rnr.domain.Column;
import org.openlmis.rnr.domain.LineItem;
import org.openlmis.rnr.domain.LossesAndAdjustmentsType;
import org.openlmis.rnr.domain.RequisitionStatusChange;
import org.openlmis.rnr.domain.Rnr;
import org.openlmis.rnr.domain.RnrLineItem;
import org.openlmis.rnr.domain.RnrStatus;
import org.openlmis.rnr.domain.Template;
import org.openlmis.web.model.PrintRnrLineItem;
import org.openlmis.web.view.pdf.requisition.RequisitionPdfModel;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static org.openlmis.rnr.domain.RnrStatus.AUTHORIZED;
import static org.openlmis.rnr.domain.RnrStatus.SUBMITTED;
import static org.openlmis.web.controller.RequisitionController.LOSSES_AND_ADJUSTMENT_TYPES;
import static org.openlmis.web.controller.RequisitionController.NUMBER_OF_MONTHS;
import static org.openlmis.web.controller.RequisitionController.REGIMEN_TEMPLATE;
import static org.openlmis.web.controller.RequisitionController.RNR;
import static org.openlmis.web.controller.RequisitionController.RNR_TEMPLATE;
import static org.openlmis.web.controller.RequisitionController.STATUS_CHANGES;

public class RequisitionExcelModel {

    private final Rnr requisition;
    private final List<? extends Column> rnrColumnList;
    private final List<? extends Column> regimenColumnList;
    private final List<RequisitionStatusChange> statusChanges;
    private final List<LossesAndAdjustmentsType> lossesAndAdjustmentsTypes;
    private final Integer numberOfMonths;
    private final MessageService messageService;
    private final ConfigurationSettingService configService;

    public RequisitionExcelModel(Map<String, Object> model, MessageService messageService,
                                 ConfigurationSettingService configService) {
        this.requisition = (Rnr) model.get(RNR);
        this.rnrColumnList = (List<? extends Column>) model.get(RNR_TEMPLATE);
        this.regimenColumnList = (List<? extends Column>) model.get(REGIMEN_TEMPLATE);
        this.statusChanges = (List<RequisitionStatusChange>) model.get(STATUS_CHANGES);
        this.lossesAndAdjustmentsTypes = (List<LossesAndAdjustmentsType>) model.get(LOSSES_AND_ADJUSTMENT_TYPES);
        this.numberOfMonths = (Integer) model.get(NUMBER_OF_MONTHS);
        this.messageService = messageService;
        this.configService = configService;
    }

    public int createHeader(Sheet sheet, int rowNum) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(messageService.message("label.requisition") + ": " +
          requisition.getProgram().getName() + " (" + requisition.getFacility().getFacilityType().getName() + ")");
        return rowNum + 1;
    }

    public int createFullSupplyTable(Sheet sheet, int rowNum) {
        List<? extends LineItem> items = requisition.getFullSupplyLineItems();
        if (items.isEmpty()) return rowNum;

        rowNum = createSectionHeader(sheet, rowNum, messageService.message("label.full.supply.products"));
        return createLineItemsTable(sheet, rowNum, items, true, rnrColumnList);
    }

    public int createNonFullSupplyTable(Sheet sheet, int rowNum) {
        List<? extends LineItem> items = requisition.getNonFullSupplyLineItems();
        if (items.isEmpty()) return rowNum;

        rowNum = createSectionHeader(sheet, rowNum, messageService.message("label.non.full.supply.products"));
        return createLineItemsTable(sheet, rowNum, items, false, rnrColumnList);
    }

    public int createRegimenTable(Sheet sheet, int rowNum) {
        List<? extends LineItem> items = requisition.getRegimenLineItems();
        if (items.isEmpty()) return rowNum;

        rowNum = createSectionHeader(sheet, rowNum, messageService.message("label.regimens"));
        return createLineItemsTable(sheet, rowNum, items, null, regimenColumnList);
    }

    private int createSectionHeader(Sheet sheet, int rowNum, String title) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(title);
        return rowNum;
    }

    private int createLineItemsTable(Sheet sheet, int rowNum, List<? extends LineItem> items,
                                     Boolean isFullSupply, List<? extends Column> columnList) {
        Template template = Template.getInstance(columnList);
        List<? extends Column> visibleColumns = template.getPrintableColumns(isFullSupply);

        // Create header row
        Row header = sheet.createRow(rowNum++);
        int col = 0;
        for (Column column : visibleColumns) {
            header.createCell(col++).setCellValue(column.getLabel());
        }

        for (LineItem item : items) {
            if (item.isRnrLineItem()) {
                PrintRnrLineItem printItem = new PrintRnrLineItem((RnrLineItem) item);
                printItem.calculate(rnrColumnList, lossesAndAdjustmentsTypes, numberOfMonths, requisition.getStatus());
            }

            Row row = sheet.createRow(rowNum++);
            col = 0;
            for (Column column : visibleColumns) {
                Object value = column.getName();
                row.createCell(col++).setCellValue(value != null ? value.toString() : "");
            }
        }
        return rowNum + 1;
    }

    public void createSummaryTable(Sheet sheet, int rowNum) {
        Row title = sheet.createRow(rowNum++);
        title.createCell(0).setCellValue(messageService.message("label.summary"));

        DecimalFormat format = new DecimalFormat("#,##0.00");

        if (!requisition.isEmergency() && requisition.getProgram().getBudgetingApplies()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(messageService.message("label.allocated.budget"));
            row.createCell(1).setCellValue(requisition.getAllocatedBudget() != null ?
              format.format(requisition.getAllocatedBudget()) :
              messageService.message("msg.budget.not.allocated"));
        }

        rowNum = createSummaryRow(sheet, rowNum, "label.total.cost.full.supply.items",
          requisition.getFullSupplyItemsSubmittedCost().toDecimal());
        rowNum = createSummaryRow(sheet, rowNum, "label.total.cost.non.full.supply.items",
          requisition.getNonFullSupplyItemsSubmittedCost().toDecimal());
        rowNum = createSummaryRow(sheet, rowNum, "label.total.cost",
          getTotalCost().toDecimal());

        fillAuditFields(sheet, rowNum);
    }

    private int createSummaryRow(Sheet sheet, int rowNum, String labelKey, Object value) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(messageService.message(labelKey));
        row.createCell(1).setCellValue(value != null ? value.toString() : "");
        return rowNum;
    }

    private void fillAuditFields(Sheet sheet, int rowNum) {
        RequisitionStatusChange submitted = getStatusChangeFor(SUBMITTED);
        RequisitionStatusChange authorized = getStatusChangeFor(AUTHORIZED);

        sheet.createRow(rowNum++).createCell(0).setCellValue(
          messageService.message("label.submitted.by") + ": " +
            (submitted != null ? submitted.getCreatedBy().getFullName() : "")
        );
        sheet.createRow(rowNum++).createCell(0).setCellValue(
          messageService.message("label.date") + ": " +
            (submitted != null ? RequisitionPdfModel.DATE_FORMAT.format(submitted.getCreatedDate()) : "")
        );
        sheet.createRow(rowNum++).createCell(0).setCellValue(
          messageService.message("label.authorized.by") + ": " +
            (authorized != null ? authorized.getCreatedBy().getFullName() : "")
        );
        sheet.createRow(rowNum++).createCell(0).setCellValue(
          messageService.message("label.date") + ": " +
            (authorized != null ? RequisitionPdfModel.DATE_FORMAT.format(authorized.getCreatedDate()) : "")
        );
    }

    private RequisitionStatusChange getStatusChangeFor(RnrStatus status) {
        return statusChanges.stream()
          .filter(s -> s.getStatus().equals(status))
          .findFirst()
          .orElse(null);
    }

    private Money getTotalCost() {
        return new Money(requisition.getFullSupplyItemsSubmittedCost().getValue()
          .add(requisition.getNonFullSupplyItemsSubmittedCost().getValue()));
    }
}
