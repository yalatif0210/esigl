ALTER TABLE regimen_line_items
    ADD adultsInRegimenFor1MonthMale INT NULL,
   ADD adultsInRegimenFor1MonthFemale INT NULL,
   ADD childrenInRegimenFor1MonthMale INT NULL,
   ADD childrenInRegimenFor1MonthFemale INT NULL,
   ADD adultsInRegimenFor2MonthMale INT NULL,
   ADD adultsInRegimenFor2MonthFemale INT NULL,
   ADD childrenInRegimenFor2MonthMale INT NULL,
   ADD childrenInRegimenFor2MonthFemale INT NULL,
   ADD adultsInRegimenFor3MonthMale INT NULL,
   ADD adultsInRegimenFor3MonthFemale INT NULL,
   ADD childrenInRegimenFor3MonthMale INT NULL,
   ADD childrenInRegimenFor3MonthFemale INT NULL,
   ADD adultsInRegimenFor4To5MonthsMale INT NULL,
   ADD adultsInRegimenFor4To5MonthsFemale INT NULL,
   ADD childrenInRegimenFor4To5MonthsMale INT NULL,
   ADD childrenInRegimenFor4To5MonthsFemale INT NULL,
   ADD adultsInRegimenFor6MonthsMale INT NULL,
   ADD adultsInRegimenFor6MonthsFemale INT NULL,
   ADD childrenInRegimenFor6MonthsMale INT NULL,
   ADD childrenInRegimenFor6MonthsFemale INT NULL;

INSERT INTO master_regimen_columns
    (name, label, visible, datatype, displayorder)
VALUES ('adultsInRegimenFor1MonthMale', 'adults  In Regimen For1 Month  Male', true,
        'regimen.reporting.dataType.numeric', 15),
       ('adultsInRegimenFor1MonthFemale', 'adults  In Regimen For1 Month  Female', true,
        'regimen.reporting.dataType.numeric', 16),
       ('childrenInRegimenFor1MonthMale', 'children  In Regimen For1 Month  Male', true,
        'regimen.reporting.dataType.numeric', 17),
       ('childrenInRegimenFor1MonthFemale', 'children  In Regimen For1 Month  Female', true,
        'regimen.reporting.dataType.numeric', 18),
       ('adultsInRegimenFor2MonthMale', 'adults  In Regimen For2 Month  Male', true,
        'regimen.reporting.dataType.numeric', 19),
       ('adultsInRegimenFor2MonthFemale', 'adults  In Regimen For2 Month  Female', true,
        'regimen.reporting.dataType.numeric', 20),
       ('childrenInRegimenFor2MonthMale', 'children  In Regimen For2 Month  Male', true,
        'regimen.reporting.dataType.numeric', 21),
       ('childrenInRegimenFor2MonthFemale', 'children  In Regimen For2 Month  Female', true,
        'regimen.reporting.dataType.numeric', 22),
       ('adultsInRegimenFor3MonthMale', 'adults  In Regimen For3 Month  Male', true,
        'regimen.reporting.dataType.numeric', 23),
       ('adultsInRegimenFor3MonthFemale', 'adults  In Regimen For3 Month  Female', true,
        'regimen.reporting.dataType.numeric', 24),
       ('childrenInRegimenFor3MonthMale', 'children  In Regimen For3 Month  Male', true,
        'regimen.reporting.dataType.numeric', 25),
       ('childrenInRegimenFor3MonthFemale', 'children  In Regimen For3 Month  Female', true,
        'regimen.reporting.dataType.numeric', 26),
       ('adultsInRegimenFor4To5MonthsMale', 'adults  In Regimen For4 To5 Months  Male', true,
        'regimen.reporting.dataType.numeric', 27),
       ('adultsInRegimenFor4To5MonthsFemale', 'adults  In Regimen For4 To5 Months  Female', true,
        'regimen.reporting.dataType.numeric', 28),
       ('childrenInRegimenFor4To5MonthsMale', 'children  In Regimen For4 To5 Months  Male', true,
        'regimen.reporting.dataType.numeric', 29),
       ('childrenInRegimenFor4To5MonthsFemale', 'children  In Regimen For4 To5 Months  Female',
        true, 'regimen.reporting.dataType.numeric', 30),
       ('adultsInRegimenFor6MonthsMale', 'adults  In Regimen For6 Months  Male', true,
        'regimen.reporting.dataType.numeric', 31),
       ('adultsInRegimenFor6MonthsFemale', 'adults  In Regimen For6 Months  Female', true,
        'regimen.reporting.dataType.numeric', 32),
       ('childrenInRegimenFor6MonthsMale', 'children  In Regimen For6 Months  Male', true,
        'regimen.reporting.dataType.numeric', 33),
       ('childrenInRegimenFor6MonthsFemale', 'children  In Regimen For6 Months  Female', true,
        'regimen.reporting.dataType.numeric', 34);

insert into program_regimen_columns
    (programid, name, label, visible, datatype, displayorder)
select programid, m.name, m.label, false, m.datatype, m.displayorder
from master_regimen_columns m
         join
         (select distinct programid from program_regimen_columns) a
         on a.programid = a.programid
where displayorder >= 15;

alter table requisition_line_items alter COLUMN previousnormalizedconsumptions type varchar(2000);