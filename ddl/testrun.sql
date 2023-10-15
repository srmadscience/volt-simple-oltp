--
-- Remember to run this with "--stop-on-error=false" if piping to sqlcmd.
--
-- e.g sqlcmd --stop-on-error=false < tetrun.sql
--
-- Or you could start a sqlcmd session and cut and paste the commands..
--

echo user does not exist
exec SpendMoney 1000 1 1 10 1;

echo zip code does not exist
exec SpendMoney 4 1 1 10 1;

echo transacton is too expensive
exec SpendMoney 3 1 1 10 10000;

echo A valid purchase
exec SpendMoney 1 1 1 10 1;

echo Query customer 1
exec QueryCustomer 1 1 1;

echo Another valid purchase
exec SpendMoney 2 50 1 10 1;

echo Query customer 2
exec QueryCustomer 2 2 2 ;


echo show sales by zip
SELECT * FROM purchase_history_by_zip;


