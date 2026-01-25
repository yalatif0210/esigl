package org.openlmis.web.view.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlmis.core.service.ConfigurationSettingService;
import org.openlmis.core.service.MessageService;
import org.openlmis.web.view.excel.requisition.RequisitionExcelWriter;
import org.openlmis.order.service.OrderService;
import org.openlmis.core.service.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * This class handles XLSX export for requisitions.
 */
@Component("requisitionExcel")
public class OpenLmisExcelView extends AbstractView {

    private final MessageService messageService;
    private final ConfigurationSettingService configService;
    private final OrderService orderService;
    private final ProgramService programService;

    @Autowired
    public OpenLmisExcelView(MessageService messageService, ConfigurationSettingService configService, OrderService orderService, ProgramService programService) {
        this.messageService = messageService;
        this.configService = configService;
        this.orderService = orderService;
        this.programService = programService;
        setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {

        try (ByteArrayOutputStream stream = createTemporaryOutputStream()) {
            RequisitionExcelWriter writer = new RequisitionExcelWriter(new XSSFWorkbook(), stream, messageService, configService, orderService, programService);
            writer.buildWith(model);

            // Set headers and write the Excel stream to the response
            response.setHeader("Content-Disposition", "attachment; filename=requisition.xlsx");
            writeToResponse(response, stream);
        }
    }
}
