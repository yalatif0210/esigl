/*
 * Electronic Logistics Management Information System (eLMIS) is a supply chain management system for health commodities in a developing country setting.
 *
 * Copyright (C) 2015  John Snow, Inc (JSI). This program was produced for the U.S. Agency for International Development (USAID). It was prepared under the USAID | DELIVER PROJECT, Task Order 4.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openlmis.restapi.controller;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.db.categories.UnitTests;
import org.openlmis.report.model.dto.StockStatusDTO;
import org.openlmis.report.service.StockStatusService;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@Category(UnitTests.class)
@RunWith(MockitoJUnitRunner.class)
public class StockStatusControllerTest {

  @Mock
  private StockStatusService service;

  @Mock
  private Principal principal;

  @InjectMocks
  private StockStatusController controller;

  @Test
  public void shouldReturnStockStatusWithNewFieldsForQuarterly() throws Exception {
    // Given
    String program = "TEST_PROGRAM";
    Long year = 2024L;
    Long quarter = 1L;
    Long userId = 1L;

    List<StockStatusDTO> stockStatusList = new ArrayList<>();
    StockStatusDTO stockStatus = new StockStatusDTO();
    stockStatus.setEsiglCode("FAC001");
    stockStatus.setRegion("Test Region");
    stockStatus.setIdDhis2("DHIS2_DATASET_001");
    stockStatusList.add(stockStatus);

    when(principal.getName()).thenReturn("testuser");
    when(service.getStockStatusByQuarter(program, year, quarter, userId)).thenReturn(stockStatusList);

    // When
    ResponseEntity response = controller.getStockStatusQuarterly(quarter, year, program, principal);

    // Then
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  public void shouldReturnStockStatusWithNewFieldsForMonthly() throws Exception {
    // Given
    String program = "TEST_PROGRAM";
    Long year = 2024L;
    Long month = 3L;
    Long userId = 1L;

    List<StockStatusDTO> stockStatusList = new ArrayList<>();
    StockStatusDTO stockStatus = new StockStatusDTO();
    stockStatus.setEsiglCode("FAC002");
    stockStatus.setRegion("Test Region 2");
    stockStatus.setIdDhis2("DHIS2_DATASET_002");
    stockStatusList.add(stockStatus);

    when(principal.getName()).thenReturn("testuser");
    when(service.getStockStatusByMonth(program, year, month, userId)).thenReturn(stockStatusList);

    // When
    ResponseEntity response = controller.getStockStatusMonthly(month, year, program, principal);

    // Then
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
  }
} 