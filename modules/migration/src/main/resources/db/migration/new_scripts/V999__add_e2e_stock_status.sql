-- View: public.vw_e2e_stock_status

-- DROP VIEW public.vw_e2e_stock_status;

CREATE OR REPLACE VIEW public.vw_e2e_stock_status
AS
SELECT d.facilityid,
       d.facilityname,
       d.geographiczoneid,
       d.geographiczonename AS districtname,
       d.programcode,
       d.programname,
       date_part('year'::text, d.periodstartdate) AS reportyear,
       date_part('month'::text, d.periodstartdate) AS reportmonth,
       (date_part('month'::text, d.periodstartdate) / 4::double precision)::integer + 1 AS reportquarter,
       d.createddate::date AS reporteddate,
       d.periodid,
       d.processingperiodname AS periodname,
       d.productid,
       d.productcode,
       d.productprimaryname AS productname,
       d.openingbalance,
       d.quantityreceived AS received,
       d.dispensed AS issues,
       d.adjustment,
       d.soh AS stockonhand,
       d.amc,
       d.mos,
       d.stocking AS stockstatus,
       d.stockoutdays,
       d.quantityordered,
       d.quantityshipped AS quantitysupplied,
       d.dateordered,
       d.dateshipped AS datesupplied,
       d.rmnch,
       d.quantityRequested,
       d.quantityApproved
FROM dw_orders d;

ALTER TABLE public.vw_e2e_stock_status
    OWNER TO postgres;

