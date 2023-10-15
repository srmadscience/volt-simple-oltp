

load classes ../jars/volt-simple-oltp-server.jar;

file -inlinebatch END_OF_BATCH



CREATE TABLE Customer
(Customer_id bigint not null primary key
,Customer_zipcode int not null
,Customer_balance float not  null);

PARTITION TABLE Customer ON COLUMN Customer_id;

CREATE TABLE purchase
(Customer_id bigint not null 
,Transaction_id bigint not null
,Item_id bigint not null 
,Qty bigint not null 
,Item_Price float not null 
,Item_salestax float not null 
,Purchase_value float not null 
,Transaction_date timestamp default now
,primary key (Customer_id, Transaction_id));

PARTITION TABLE purchase ON COLUMN Customer_id;

CREATE TABLE Sales_taxes
(Zip_code int not null primary key
,Tax_rate_pct float not null);

CREATE STREAM purchase_stream 
EXPORT TO TOPIC purchase_topic 
WITH KEY (Customer_id)
partition on column Customer_id
(Customer_id bigint not null 
,Transaction_id bigint not null
,Item_id bigint not null 
,Qty bigint not null 
,Item_Price float not null 
,Item_salestax float not null 
,Purchase_value float not null 
,Zip_code int not null 
,Transaction_date timestamp default now);

CREATE VIEW purchase_history 
AS 
SELECT Customer_id, sum(Purchase_value) Purchase_value, count(*) how_many 
from purchase_stream
group by Customer_id;

CREATE VIEW purchase_history_by_zip 
AS 
SELECT Zip_code, sum(Purchase_value) Purchase_value, count(*) how_many 
from purchase_stream
group by Zip_code;

    

    
CREATE PROCEDURE 
   PARTITION ON TABLE Customer COLUMN Customer_id
   FROM CLASS simpleoltpprocs.SpendMoney;  
   


CREATE PROCEDURE QueryCustomer 
    PARTITION ON TABLE Customer COLUMN Customer_id 
    AS BEGIN
    SELECT * FROM Customer WHERE customer_id = ?;
    SELECT * FROM Purchase WHERE customer_id = ? ORDER BY transaction_id;
    SELECT * FROM Purchase_history WHERE customer_id = ?;
    END;

END_OF_BATCH

INSERT INTO Sales_taxes (Zip_code, Tax_rate_pct) VALUES (01730,0.045);
INSERT INTO Sales_taxes (Zip_code, Tax_rate_pct) VALUES (94596,0.08);
INSERT INTO Sales_taxes (Zip_code, Tax_rate_pct) VALUES (59717,0);

INSERT INTO Customer (Customer_id,Customer_zipcode,Customer_balance) 
VALUES 
(1,01730,1000);

INSERT INTO Customer (Customer_id,Customer_zipcode,Customer_balance) 
VALUES 
(2,94596,200);

INSERT INTO Customer (Customer_id,Customer_zipcode,Customer_balance) 
VALUES 
(3,59717,1000);

INSERT INTO Customer (Customer_id,Customer_zipcode,Customer_balance) 
VALUES 
(4,20151,1000);




