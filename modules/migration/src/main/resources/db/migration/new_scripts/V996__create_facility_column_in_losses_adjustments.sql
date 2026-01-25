ALTER TABLE requisition_line_item_losses_adjustments
ADD facilityId INT null references facilities(id);

ALTER TABLE requisition_line_item_losses_adjustments
   ADD reason varchar(200);