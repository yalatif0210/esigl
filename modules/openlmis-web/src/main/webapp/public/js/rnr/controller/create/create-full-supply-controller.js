/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

function CreateFullSupplyController($scope, messageService, FacilitiesLight, $filter) {
  $scope.currentRnrLineItem = undefined;

  $scope.getId = function (prefix, parent) {
    return prefix + "_" + parent.$parent.$parent.$index;
  };

  FacilitiesLight.get(function(data) {
    $scope.facilities = $filter('orderBy')(data.facilityLightList, 'name');
  });

  $scope.saveLossesAndAdjustmentsForRnRLineItem = function () {
    $scope.modalError = '';

    if (!$scope.currentRnrLineItem.validateLossesAndAdjustments()) {
      $scope.modalError = messageService.get('error.correct.highlighted');
      return;
    }

    $scope.currentRnrLineItem.reEvaluateTotalLossesAndAdjustments();
    $scope.clearAndCloseLossesAndAdjustmentModal();
  };

  $scope.cancelAndCloseLossesAndAdjustmentModal = function () {

    // restore the old losses and adjustments because the changes have been canceled
    $scope.currentRnrLineItem.lossesAndAdjustments = $scope.oldLossAndAdjustment;
    $scope.currentRnrLineItem.reEvaluateTotalLossesAndAdjustments();
    $scope.clearAndCloseLossesAndAdjustmentModal();
  };

  $scope.clearAndCloseLossesAndAdjustmentModal = function () {
    $scope.lossAndAdjustment = undefined;
    $scope.lossesAndAdjustmentsModal = false;
    $('#' + $scope.currentLinkId).focus();
  };

  $scope.resetModalErrorAndSetFormDirty = function () {
    $scope.modalError = '';
    $scope.saveRnrForm.$dirty = true;
  };

  $scope.showLossesAndAdjustments = function (lineItem) {
    // keep a copy of the old losses an adjustments for just in case the dialog box is canceled;

    $scope.oldLossAndAdjustment = angular.copy(lineItem.lossesAndAdjustments);
    $scope.currentRnrLineItem = lineItem;
    updateLossesAndAdjustmentTypesToDisplayForLineItem(lineItem);
    $scope.lossesAndAdjustmentsModal = true;
  };

  $scope.removeLossAndAdjustment = function (lossAndAdjustmentToDelete) {
    $scope.currentRnrLineItem.removeLossAndAdjustment(lossAndAdjustmentToDelete);
    updateLossesAndAdjustmentTypesToDisplayForLineItem($scope.currentRnrLineItem);
    $scope.resetModalErrorAndSetFormDirty();
  };

  $scope.addLossAndAdjustment = function (newLossAndAdjustment) {
    $scope.currentRnrLineItem.addLossAndAdjustment(newLossAndAdjustment);
    updateLossesAndAdjustmentTypesToDisplayForLineItem($scope.currentRnrLineItem);
    $scope.saveRnrForm.$dirty = true;
  };

  $scope.showAddSkippedProductsModal = function(){
    $scope.addSkippedProductsModal = true;
  };

  $scope.unskipProducts = function(rnr){

    var selected = _.where(rnr.skippedLineItems, {unskip: true});
    angular.forEach(selected, function(lineItem){
      lineItem.skipped = false;
      $scope.$parent.page[$scope.$parent.visibleTab].push( new RegularRnrLineItem(lineItem, rnr.numberOfMonths, rnr.programRnrColumnList, rnr.status, rnr.period, rnr.facility));
    });

    $scope.saveRnrForm.$dirty = true;
    $scope.$parent.saveRnr();

    if(rnr.fullSupplyLineItems.length === 0){
      rnr.fullSupplyLineItems = $scope.$parent.page[$scope.$parent.visibleTab];
    }

    angular.forEach(rnr.skippedLineItems, function(li){
      if(!li.unskip){
        li.unskip = false;
      }
    });

    rnr.skippedLineItems = _.where(rnr.skippedLineItems, {unskip: false});
    window.window.adjustHeight();
    // all is said and done, close the dialog box
    $scope.addSkippedProductsModal = false;
  };

  function updateLossesAndAdjustmentTypesToDisplayForLineItem(lineItem) {

    var TRANSFER_ENTRANT_OBJECT = _.findWhere($scope.lossesAndAdjustmentTypes, {'name': "TRANSFERT_ENTRANT"});
    var TRANSFER_SORTANT_OBJECT = _.findWhere($scope.lossesAndAdjustmentTypes, {'name': "TRANSFERT_SORTANT"});

    var lossesAndAdjustmentTypesForLineItem = _.pluck(_.pluck(lineItem.lossesAndAdjustments, 'type'), 'name');

    var lossesAndAdjustmentTypesToDisplayV = $.grep($scope.lossesAndAdjustmentTypes, function (lAndATypeObject) {
      return $.inArray(lAndATypeObject.name, lossesAndAdjustmentTypesForLineItem) == -1;
    });
    var searchTransferIn = [], searchTransferOut = [];
     searchTransferIn = _.where(lossesAndAdjustmentTypesToDisplayV, {'name':"TRANSFERT_ENTRANT"});
     searchTransferOut = _.where(lossesAndAdjustmentTypesToDisplayV, {'name':"TRANSFERT_SORTANT"});
    if(searchTransferIn.length === 0) {
      lossesAndAdjustmentTypesToDisplayV.push(TRANSFER_ENTRANT_OBJECT);
    }

    if(searchTransferOut.length ===  0) {
      lossesAndAdjustmentTypesToDisplayV.push(TRANSFER_SORTANT_OBJECT);
    }

    $scope.lossesAndAdjustmentTypesToDisplay = lossesAndAdjustmentTypesToDisplayV;

  }
}
