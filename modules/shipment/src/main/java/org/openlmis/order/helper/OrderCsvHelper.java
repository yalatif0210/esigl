/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.order.helper;

import java.text.*;
import org.apache.commons.collections.Predicate;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.*;
import org.openlmis.core.domain.*;
import org.openlmis.core.service.*;
import org.openlmis.order.domain.Order;
import org.openlmis.order.domain.OrderFileColumn;
import org.openlmis.order.dto.OrderFileTemplateDTO;
import org.openlmis.rnr.domain.LineItemComparator;
import org.openlmis.rnr.domain.RnrLineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.filter;
import static org.joda.time.format.DateTimeFormat.forPattern;

/**
 * OrderCsvHelper provides helper methods to generate a csv file for Order entity.
 */

@Component
public class OrderCsvHelper {

  // Constants for order types
  private static final String PSG = "PSG";
  private static final String CSO = "CSO";
  private static final String GTC = "GTC";
  private static final String PSGPN = "PSGPN";

  // Constants for supply line codes
  private static final String AP01 = "AP01";
  private static final String PG01 = "PG01";
  private static final String PG02 = "PG02";

  // Constants for facility codes
  private static final String AG02 = "AG02";
  private static final String AG03 = "AG03";
  private static final String AG04 = "AG04";
  private static final String AG05 = "AG05";
  private static final String AG01 = "AG01";
  private static final String PR01 = "PR01";


  // Constants for program codes
  private static final String KRHGO = "KRHGO";
  private static final String MAN = "MAN";
  private static final String ODNE = "ODNE";

  // Constants for numeric suffixes
  private static final String SUFFIX_1 = "1";
  private static final String SUFFIX_2 = "2";
  private static final String SUFFIX_3 = "3";
  private static final String SUFFIX_4 = "4";

  // Constants for other fields
  private static final String EMERGENCY = "2";
  private static final String NON_EMERGENCY = "1";
  private static final String XOF = "XOF";
  private static final String CDES = "CDES";

  // Constants for SAGE configuration keys
  private static final String SAGE_CONFIGURATIONS = "SAGE_CONFIGURATIONS";
  private static final String NEW_SUPPLY_LINE = "NEW_SUPPLY_LINE";
  private static final String NEW_PROGRAM_SUPPLY_LINE = "NEW_PROGRAM_SUPPLY_LINE";
  private static final String SAGE_PROGRAM_MAPPING = "SAGE_PROGRAM_MAPPING";
  private static final String SAGE_PROGRAM_CODE = "SAGE_PROGRAM_CODE";

  public static final String STRING = "string";
  public static final String LINE_NO = "line_no";
  public static final String ORDER = "order";

  private String lineSeparator = "\r\n";

  private Boolean encloseValuesWithQuotes = false;
  @Autowired
  ConfigurationSettingService configSettingService;

  @Autowired
  private ELMISInterfaceService elmisInterfaceService;

  public OrderCsvHelper() {

  }

  String pattern = "ddMMyy";
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

  public void writeCsvFile(Order order, OrderFileTemplateDTO orderFileTemplateDTO, Writer writer) throws IOException {
    List<OrderFileColumn> orderFileColumns = orderFileTemplateDTO.getOrderFileColumns();
    removeExcludedColumns(orderFileColumns);
    //write first line header
    writeHeaderForSerge(order, writer);

    if (orderFileTemplateDTO.getOrderConfiguration().isHeaderInFile()) {
      writeHeader(orderFileColumns, writer);
    }
    List<RnrLineItem> nonFullSupplyLineItems = order.getRnr().getNonFullSupplyLineItems();
    Collections.sort(nonFullSupplyLineItems, new LineItemComparator());

    writeLineItems(order, order.getRnr().getFullSupplyLineItems(), orderFileColumns, writer);
    writeLineItems(order, nonFullSupplyLineItems, orderFileColumns, writer);
  }

  //TODO: remove the following two functions and remain with one , currently due to lot of changes and unstability keep them all.
  private void writeHeaderForSergeOrg(Order order, Writer writer) throws IOException {
    String orderType = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        SAGE_CONFIGURATIONS);

    String newSupplyLine = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getFacility().getCode(),
        NEW_SUPPLY_LINE);

    String newProgramSupplyLine = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        NEW_PROGRAM_SUPPLY_LINE);

    String codeInitial = order.getRnr().getFacility().getCode();
    String routeOfTheFacility = codeInitial.substring(0, 7);

    String newFacilityCode;
    String facilityCode2;
    String supplyingFacilityCode2 = "";
    String newSupplyingFacility = "";
    String supplyingFacilityCode = order.getSupplyingFacility().getCode();
    String programCode = order.getRnr().getProgram().getCode().toUpperCase();

    // Determine newFacilityCode
    switch (orderType.toUpperCase()) {
      case PSG:
        newFacilityCode = routeOfTheFacility + SUFFIX_4;
        facilityCode2 = "90100018";
        break;
      case CSO:
        newFacilityCode = routeOfTheFacility + SUFFIX_1;
        facilityCode2 = newFacilityCode;
        break;
      case GTC:
        newFacilityCode = routeOfTheFacility + SUFFIX_2;
        facilityCode2 = newFacilityCode;
        break;
      case PSGPN:
        newFacilityCode = routeOfTheFacility + SUFFIX_3;
        facilityCode2 = newFacilityCode;
        break;
      default:
        newFacilityCode = codeInitial;
        facilityCode2 = newFacilityCode;
        break;
    }

    // Determine supplyingFacilityCode2
    switch (supplyingFacilityCode.toUpperCase()) {
      case AG02:
        if (programCode.contains(KRHGO)) {
          supplyingFacilityCode2 = AG04;
        } else if (programCode.contains(MAN)) {
          supplyingFacilityCode2 = AG03;
        } else if (programCode.contains(ODNE)) {
          supplyingFacilityCode2 = AG05;
        } else {
          supplyingFacilityCode2 = AG02;
        }
        break;
      default:
        switch (newSupplyLine.toUpperCase()) {
          case PG01:
            supplyingFacilityCode2 = PG01;
            break;
          case PG02:
            supplyingFacilityCode2 = PG02;
            break;
          default:
            supplyingFacilityCode2 = AG01;
            break;
        }
        break;
    }

    // Determine newSupplyingFacility
    if (AP01.equalsIgnoreCase(newProgramSupplyLine)) {
      newSupplyingFacility = AP01;
    }

    // Write header
    writer.write("E");
    writer.write(";");
    writer.write(!newSupplyingFacility.isEmpty() ? newSupplyingFacility : supplyingFacilityCode);
    writer.write(";");
    writer.write("");
    writer.write(";");
    writer.write(CDES);
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(), SAGE_CONFIGURATIONS));
    writer.write(";");
    writer.write(newFacilityCode);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(simpleDateFormat.format(new Date()));
    writer.write(";");
    writer.write(order.getOrderNumber());
    writer.write(";");
    writer.write(supplyingFacilityCode2);
    writer.write(";");
    writer.write(XOF);
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(), SAGE_PROGRAM_MAPPING));
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(), SAGE_PROGRAM_CODE));
    writer.write(";");
    writer.write(order.getRnr().isEmergency() ? EMERGENCY : NON_EMERGENCY);
    writer.write(System.lineSeparator());
  }
  //Reeal
  private void writeHeaderForSergeReal(Order order, Writer writer) throws IOException {

    String orderType = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        "SAGE_CONFIGURATIONS");

    String newSupplyLine = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getFacility().getCode(),
        "NEW_SUPPLY_LINE");

    String newProgramSupplyLine = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        "NEW_PROGRAM_SUPPLY_LINE");

    String facilityCode = "", sevenPremiersCaracteres="",
        codeInitial = "",
        facilityCode2 = "", newFacilityCode = "";

    codeInitial = order.getRnr().getFacility().getCode();

    sevenPremiersCaracteres = codeInitial.substring(0, 7);

    String routeOfTheFacility = sevenPremiersCaracteres;

    String supplyingFacilityCode = order.getSupplyingFacility().getCode(), supplyingFacilityCode2 = "", newSupplyingFacility = "", newSupplyingFacilityCode2 = "";

    if (orderType.equalsIgnoreCase("PSG")) {
      //facilityCode = order.getRnr().getFacility().getCode() + "4";
      newFacilityCode = routeOfTheFacility + "4";
    } else if (orderType.equalsIgnoreCase("CSO")) {
      newFacilityCode = routeOfTheFacility + "1";
      //facilityCode = order.getRnr().getFacility().getCode() + "1";
    } else if(orderType.equalsIgnoreCase("GTC")) {
      newFacilityCode = routeOfTheFacility + "2";
    } else if (orderType.equalsIgnoreCase("PSGPN")) {
      newFacilityCode = routeOfTheFacility + "3";
    } else {
      newFacilityCode = codeInitial ;
    }

    if (orderType.equalsIgnoreCase("PSG")) {
      facilityCode2 = "90100018";
    } else {
      facilityCode2 = newFacilityCode;
    }

    if (StringUtils.isNotEmpty(newProgramSupplyLine) && (newProgramSupplyLine.equalsIgnoreCase("AP01") || newProgramSupplyLine.equalsIgnoreCase("AP02"))) {
      newSupplyingFacility = newProgramSupplyLine;
    } else {
      newSupplyingFacility = supplyingFacilityCode ;
    }

    String programCode = order.getRnr().getProgram().getCode().toUpperCase();

    if ( StringUtils.isNotEmpty(newSupplyLine) &&
        orderType.equalsIgnoreCase("PSG") &&
        (newSupplyLine.equalsIgnoreCase("PG01") || newSupplyLine.equalsIgnoreCase("PG02"))){
      supplyingFacilityCode2 = newSupplyLine;
    } else {

      if  (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && programCode.contains("KRHGO")
    ) {
      supplyingFacilityCode2 = "AG04";

    } else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
       && programCode.contains("MAN")
    ) {
      supplyingFacilityCode2 = "AG03";
    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && programCode.contains("ODNE")
    ) {
      supplyingFacilityCode2 = "AG05";
    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")){
        supplyingFacilityCode2 = "AG02";
    } else {
        supplyingFacilityCode2 = "AG01";
    }}

    writer.write("E");
    writer.write(";");
    writer.write(!(newSupplyingFacility.equalsIgnoreCase("")) ? newSupplyingFacility : order.getSupplyingFacility().getCode());
    writer.write(";");
    writer.write("");
    writer.write(";");
    writer.write("CDES");
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        "SAGE_CONFIGURATIONS"));
    writer.write(";");
    writer.write(newFacilityCode);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(simpleDateFormat.format(new Date()));
    writer.write(";");
    writer.write(order.getOrderNumber());
    writer.write(";");
    writer.write(supplyingFacilityCode2);
    writer.write(";");
    writer.write("XOF");
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode()
        , "SAGE_PROGRAM_MAPPING"));
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode()
        , "SAGE_PROGRAM_CODE"));
    writer.write(";");
    writer.write((order.getRnr().isEmergency())?"2":"1");
    writer.write(lineSeparator);
  }
  private void writeHeaderForSerge(Order order, Writer writer) throws IOException {
    String orderType = elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getProgram().getCode(), "SAGE_CONFIGURATIONS"
    );

    String newSupplyLine = elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getFacility().getCode(), "NEW_SUPPLY_LINE"
    );

    String newProgramSupplyLine = elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getProgram().getCode(), "NEW_PROGRAM_SUPPLY_LINE"
    );

    String codeInitial = order.getRnr().getFacility().getCode();
    String sevenPremiersCaracteres = codeInitial.substring(0, 7);
    String routeOfTheFacility = sevenPremiersCaracteres;

    String newFacilityCode;
    switch (orderType.toUpperCase()) {
      case "PSG":
        newFacilityCode = routeOfTheFacility + "4";
        break;
      case "CSO":
        newFacilityCode = routeOfTheFacility + "1";
        break;
      case "GTC":
        newFacilityCode = routeOfTheFacility + "2";
        break;
      case "PSGPN":
        newFacilityCode = routeOfTheFacility + "3";
        break;
      case "HPC":
        newFacilityCode = routeOfTheFacility + "6";
        break;
      case "PNLMM-PMNT":
        newFacilityCode = routeOfTheFacility + "7";
        break;
      default:
        newFacilityCode = codeInitial;
    }


    String facilityCode2;
//    String facilityCode2 = orderType.equalsIgnoreCase("PSG") ? "90100018" : newFacilityCode;

    if (orderType.equalsIgnoreCase("PSG")) {
      facilityCode2 = "90100018";
    }
    else if (orderType.equalsIgnoreCase("SDP")) {
      facilityCode2 = "90300500";

    } else {
      facilityCode2 = newFacilityCode;
    }
    String newSupplyingFacility = StringUtils.isNotEmpty(newProgramSupplyLine) &&
        (newProgramSupplyLine.equalsIgnoreCase("AP01") || newProgramSupplyLine.equalsIgnoreCase("AP02")) ?
        newProgramSupplyLine : order.getSupplyingFacility().getCode();

    String supplyingFacilityCode = order.getSupplyingFacility().getCode();
    String programCode = order.getRnr().getProgram().getCode().toUpperCase();
    String supplyingFacilityCode2;

    if (orderType.equalsIgnoreCase("CMUBM")) {
      supplyingFacilityCode2 = PR01;
    }
    else if (StringUtils.isNotEmpty(newSupplyLine) && orderType.equalsIgnoreCase("PSG") &&
        (newSupplyLine.equalsIgnoreCase("PG01") || newSupplyLine.equalsIgnoreCase("PG02"))) {
      supplyingFacilityCode2 = newSupplyLine;
    } else {
      switch (supplyingFacilityCode) {

        case "AG02":
          if (programCode.contains("KRHGO")) {
            supplyingFacilityCode2 = "AG04";
          } else if (programCode.contains("MAN")) {
            supplyingFacilityCode2 = "AG03";
          } else if (programCode.contains("ODNE")) {
            supplyingFacilityCode2 = "AG05";
          } else if (programCode.contains("DALOA")) {
            supplyingFacilityCode2 = "AG06";
          } else {
            supplyingFacilityCode2 = "AG02";
          }
          break;
        case "AG01":
          if (programCode.contains("ABGO")) {
            supplyingFacilityCode2 = "AG10";
          } else if (programCode.contains("ADZPE")) {
            supplyingFacilityCode2 = "AG11";
          } else if (programCode.contains("AGBO")) {
            supplyingFacilityCode2 = "AG12";
          } else if (programCode.contains("SPDRO")) {
            supplyingFacilityCode2 = "AG13";
          } else {
            supplyingFacilityCode2 = "AG01";
          }
          break;
        default:
          supplyingFacilityCode2 = "AG01";
      }
    }

    writer.write("E;");
    writer.write(!(newSupplyingFacility.isEmpty()) ? newSupplyingFacility : order.getSupplyingFacility().getCode());
    writer.write(";;CDES;");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getProgram().getCode(), "SAGE_CONFIGURATIONS"
    ));
    writer.write(";");
    writer.write(newFacilityCode + ";" + facilityCode2 + ";" + facilityCode2 + ";");
    writer.write(simpleDateFormat.format(new Date()) + ";");
    writer.write(order.getOrderNumber() + ";" + supplyingFacilityCode2 + ";XOF;");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getProgram().getCode(), "SAGE_PROGRAM_MAPPING"
    ) + ";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(
        order.getRnr().getProgram().getCode(), "SAGE_PROGRAM_CODE"
    ) + ";");
    writer.write(order.getRnr().isEmergency() ? "2" : "1");
    writer.write(lineSeparator);
  }

  private void writeHeaderForSerge1(Order order, Writer writer) throws IOException {

    String orderType = elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        "SAGE_CONFIGURATIONS");
    String facilityCode = "", sixPremiersCaracteres="",
        codeInitial = "",
        facilityCode2 = "", newFacilityCode = "";

    codeInitial = order.getRnr().getFacility().getCode();

    sixPremiersCaracteres = codeInitial.substring(0, 6);

    char eighthCharacter = codeInitial.charAt(7);

    String routeOfTheFacility = sixPremiersCaracteres + eighthCharacter;

    String supplyingFacilityCode = order.getSupplyingFacility().getCode(), supplyingFacilityCode2 = "" ;

    if (orderType.equalsIgnoreCase("PSG")) {
      //facilityCode = order.getRnr().getFacility().getCode() + "4";
      newFacilityCode = routeOfTheFacility + "4";
    } else if (orderType.equalsIgnoreCase("CSO")) {
      newFacilityCode = routeOfTheFacility + "1";
      //facilityCode = order.getRnr().getFacility().getCode() + "1";
    } else if(orderType.equalsIgnoreCase("GTC")) {
      newFacilityCode = routeOfTheFacility + "2";
    } else if (orderType.equalsIgnoreCase("RECPN")) {
      newFacilityCode = routeOfTheFacility + "3";
    } else {
      newFacilityCode = routeOfTheFacility + "0";
    }

    if (orderType.equalsIgnoreCase("PSG")) {
      facilityCode2 = "90100018";
    } else {
      facilityCode2 = newFacilityCode;
    }

    if (supplyingFacilityCode.equalsIgnoreCase("AG02")
    && order.getRnr().getProgram().getCode().equalsIgnoreCase("GTCGVKRHGO")
    ) {
      supplyingFacilityCode2 = "AG04";

    } else  if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && order.getRnr().getProgram().getCode().equalsIgnoreCase("MERECKRHGO")
    ) {
      supplyingFacilityCode2 = "AG04";

    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && order.getRnr().getProgram().getCode().equalsIgnoreCase("GTCGVMAN")
    ) {
      supplyingFacilityCode2 = "AG03";

    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && order.getRnr().getProgram().getCode().equalsIgnoreCase("MERECMAN")
    ) {
      supplyingFacilityCode2 = "AG03";
    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && order.getRnr().getProgram().getCode().equalsIgnoreCase("GTCGVODNE")
    ) {
      supplyingFacilityCode2 = "AG05";

    }
    else if (supplyingFacilityCode.equalsIgnoreCase("AG02")
        && order.getRnr().getProgram().getCode().equalsIgnoreCase("MERECODNE")
    ) {
      supplyingFacilityCode2 = "AG05";
    }
    else {
      if (supplyingFacilityCode.equalsIgnoreCase("AG02")){
        supplyingFacilityCode2 = "AG02";
      } else {
        supplyingFacilityCode2 = "AG01";
      }
    }

    writer.write("E");
    writer.write(";");
    writer.write(order.getSupplyingFacility().getCode());
    writer.write(";");
    writer.write("");
    writer.write(";");
    writer.write("CDES");
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode(),
        "SAGE_CONFIGURATIONS"));
    writer.write(";");
    writer.write(newFacilityCode);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(facilityCode2);
    writer.write(";");
    writer.write(simpleDateFormat.format(new Date()));
    writer.write(";");
    writer.write(order.getOrderNumber());
    writer.write(";");
    writer.write(supplyingFacilityCode2);
    writer.write(";");
    writer.write("XOF");
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode()
        , "SAGE_PROGRAM_MAPPING"));
    writer.write(";");
    writer.write(elmisInterfaceService.getDataSageByProgramCode(order.getRnr().getProgram().getCode()
            , "SAGE_PROGRAM_CODE"));
    writer.write(";");
    writer.write((order.getRnr().isEmergency())?"2":"1");
    writer.write(lineSeparator);
  }

  private void removeExcludedColumns(List<OrderFileColumn> orderFileColumns) {
    filter(orderFileColumns, new Predicate() {
      @Override
      public boolean evaluate(Object o) {
        return ((OrderFileColumn) o).getIncludeInOrderFile();
      }
    });
  }

  private void writeHeader(List<OrderFileColumn> orderFileColumns, Writer writer) throws IOException {
    for (OrderFileColumn column : orderFileColumns) {
      String columnLabel = column.getColumnLabel();
      if (columnLabel == null) columnLabel = "";
      writer.write(columnLabel);
      if (orderFileColumns.indexOf(column) == (orderFileColumns.size() - 1)) {
        writer.write(lineSeparator);
        break;
      }
      writer.write(",");
    }
  }

  private void writeLineItems(Order order, List<RnrLineItem> fullSupplyLineItems, List<OrderFileColumn> orderFileColumns, Writer writer) throws IOException {

    // allow the user to control what line separator to use from the administrative pages
    // this value was different between a windows and linux target systems.
    // this could have been written better.
    lineSeparator = StringEscapeUtils.unescapeJava(configSettingService.getConfigurationStringValue("CSV_LINE_SEPARATOR"));
    // setting to enclose or not to enclose values in quotes.
    encloseValuesWithQuotes = configSettingService.getBoolValue("CSV_APPLY_QUOTES");

    int counter = 1;
    for (RnrLineItem rnrLineItem : fullSupplyLineItems) {
      writer.write("L");
      writer.write(";");
      writeCsvLineItem(order, rnrLineItem, orderFileColumns, writer, counter++);
      writer.write(lineSeparator);
    }
  }

  private void writeCsvLineItem(Order order, RnrLineItem rnrLineItem, List<OrderFileColumn> orderFileColumns, Writer writer, int counter) throws IOException {

    JXPathContext orderContext = JXPathContext.newContext(order);
    JXPathContext lineItemContext = JXPathContext.newContext(rnrLineItem);
    for (OrderFileColumn orderFileColumn : orderFileColumns) {
      if (orderFileColumn.getNested() == null || orderFileColumn.getNested().isEmpty()) {
        writeValue(writer, "");
        writeSeparator(orderFileColumns, writer, orderFileColumn);
        continue;
      }
      Object columnValue = getColumnValue(counter, orderContext, lineItemContext, orderFileColumn);

      if (columnValue instanceof Date) {
        columnValue = forPattern(orderFileColumn.getFormat()).print(((Date) columnValue).getTime());
      }
      writeValue(writer, columnValue);
      writeSeparator(orderFileColumns, writer, orderFileColumn);
    }
  }

  private void writeSeparator(List<OrderFileColumn> orderFileColumns, Writer writer, OrderFileColumn orderFileColumn) throws IOException {
    if (orderFileColumns.indexOf(orderFileColumn) < orderFileColumns.size() - 1) {
      writer.write(";");
    }
  }

  private void writeValue(Writer writer, Object columnValue) throws IOException {
    // avoid null pointer exception.
    if(columnValue == null) {
      columnValue = "";
    }

    if (encloseValuesWithQuotes) {
      writer.write("\"" + (columnValue).toString() + "\"");
    } else {
      writer.write((columnValue).toString());
    }
  }

  private Object getColumnValue(int counter, JXPathContext orderContext, JXPathContext lineItemContext, OrderFileColumn orderFileColumn) {
    Object columnValue;

    switch (orderFileColumn.getNested()) {
      case STRING:
        columnValue = orderFileColumn.getKeyPath();
        break;
      case LINE_NO:
        columnValue = counter;
        break;
      case ORDER:
        columnValue = orderContext.getValue(orderFileColumn.getKeyPath());
        break;
      default:
        columnValue = lineItemContext.getValue(orderFileColumn.getKeyPath());
        break;
    }
    return columnValue;
  }
}
