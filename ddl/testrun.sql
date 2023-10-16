--
-- Remember to run this with "--stop-on-error=false" if piping to sqlcmd.
--
-- e.g sqlcmd --stop-on-error=false < testrun.sql
--
-- Or you could start a sqlcmd session and cut and paste the commands..
--

echo Test 1: user does not exist
exec SpendMoney 1000 1 1 10 1;

echo Test 2: zip code does not exist
exec SpendMoney 4 1 1 10 1;

echo Test 3: transaction is too expensive
exec SpendMoney 3 1 1 10 10000;

echo Test 4: A valid purchase
exec SpendMoney 1 1 1 10 1;

echo Test 5: Query customer 1
exec QueryCustomer 1 1 1;

echo Test 6: Another valid purchase
exec SpendMoney 2 50 1 10 1;

echo Test 7: Query customer 2
exec QueryCustomer 2 2 2 ;

echo Test 8: show sales by zip
SELECT * FROM purchase_history_by_zip;


