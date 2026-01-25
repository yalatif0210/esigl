-- View: public.vw_e2e_stock_status

DROP VIEW IF EXISTS public.vw_e2e_stock_status;

CREATE OR REPLACE VIEW public.vw_e2e_stock_status
AS
SELECT f.id facilityid,
       f.name facilityname,
       f.geographiczoneid,
       dis.district_name AS districtname,
       p.code programcode,
       p.name programname,
       date_part('year'::text, pp.startdate) AS reportyear,
       date_part('month'::text, pp.startdate) AS reportmonth,
       (date_part('month'::text, pp.startdate) / 4::double precision)::integer + 1 AS reportquarter,
       r.createddate::date AS reporteddate,
       r.periodid,
       pp.name AS periodname,
       p.id productid,
       d.productcode,
       pr.primaryname AS productname,
       d.beginningbalance openingbalance,
       d.quantityreceived AS received,
       d.quantityDispensed AS issues,
       d.totallossesandadjustments adjustment,
       d.stockinhand AS stockonhand,
       d.amc,
       CASE d.amc when 0 then 0 else ROUND((d.stockinhand::NUMERIC / d.amc)::NUMERIC,2) end mos,
       0 AS stockstatus,
       d.stockoutdays,
       d.calculatedOrderQuantity quantityordered,
       0 AS quantitysupplied,
       d.modifieddate dateordered,
       d.quantityRequested,
       d.quantityApproved
FROM requisition_line_items d
         JOIN requisitions r on d.rnrId = r.id
         JOIN facilities f On r.facilityId = f.id
         JOIN vw_districts dis on f.geographiczoneid = dis.district_id
         JOIN programs p On r.programId = P.ID
         JOIN processing_periods pp on r.periodid = pp.id
         JOIN products pr ON d.productcode::text = pr.code::text

WHERE r.status not in ('INITIATED') AND D.skipped = false;
ALTER TABLE public.vw_e2e_stock_status
    OWNER TO postgres;
