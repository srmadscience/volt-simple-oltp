file -inlinebatch END_OF_BATCH

DROP PROCEDURE SpendMoney IF EXISTS;
DROP PROCEDURE QueryCustomer IF EXISTS;
DROP view purchase_history_by_zip IF EXISTS;
DROP view purchase_history IF EXISTS;
DROP STREAM purchase_stream IF EXISTS;
DROP table Sales_taxes IF EXISTS;
DROP table Customer IF EXISTS;
DROP table Purchase IF EXISTS;

END_OF_BATCH
