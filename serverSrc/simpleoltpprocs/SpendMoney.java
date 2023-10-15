package simpleoltpprocs;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2023 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class SpendMoney extends VoltProcedure {

    // @formatter:off

    public static final SQLStmt getUser = new SQLStmt(
            "SELECT customer_zipcode, customer_balance FROM customer WHERE customer_id = ?;");

    public static final SQLStmt getZip = new SQLStmt(
            "SELECT t.tax_rate_pct FROM Sales_taxes t, customer c WHERE c.customer_id = ? AND c.customer_zipcode = t.zip_code;");

    public static final SQLStmt updBalance = new SQLStmt("UPDATE customer SET customer_balance = customer_balance - ? WHERE customer_id = ?;");

    public static final SQLStmt recordPurchase = new SQLStmt("INSERT INTO purchase (Customer_id, Transaction_id, Item_id, Qty , Item_Price, Item_salestax ,Purchase_value ) "
            + "VALUES (?,?,?,?,?,?,?);");

    public static final SQLStmt recordPurchaseInStream = new SQLStmt("INSERT INTO purchase_stream (Customer_id, Transaction_id, Item_id, Qty , Item_Price, Item_salestax ,Purchase_value, Zip_code ) "
            + "VALUES (?,?,?,?,?,?,?,?);");


	// @formatter:on

    /**
     * This Volt Procedure allows a customer to record a purchase if, and only if:
     * 
     * 1. The customer exists
     * 2. We know how to calculate the sales tax for this transaction
     * 3. The customer has enough money.
     * 
     * @param customerId
     * @param qty
     * @param transaction_id
     * @param item_id
     * @param item_price
     * @return nothing if it works, a VoltAbortException otherwise.
     * @throws VoltAbortException errors if conditions 1, 2 or 3 are met.
     */
    public VoltTable[] run(long customerId, int qty, int transaction_id, int item_id, double item_price)
            throws VoltAbortException {

        // See if this user is real, and what their sales tax would be..
        voltQueueSQL(getUser, customerId);
        voltQueueSQL(getZip, customerId);
        VoltTable[] initialResults = voltExecuteSQL();
        VoltTable customerRecord = initialResults[0];
        VoltTable salestaxRecord = initialResults[1];

        // Sanity check: Does this user exist?
        if (!customerRecord.advanceRow()) {
            throw new VoltAbortException("User " + customerId + " does not exist");
        }

        final long customerZipcode = customerRecord.getLong("customer_zipcode");
        final double customerBalance = customerRecord.getDouble("customer_balance");

        // Sanity check: Does this zip have a tax record?
        if (!salestaxRecord.advanceRow()) {
            throw new VoltAbortException("Zip " + customerZipcode + " does not exist");
        }

        final double salesTaxRate = salestaxRecord.getDouble("tax_rate_pct");

        // Calculate sales tax
        final double itemSalesTax = salesTaxRate * item_price;
        double costOfThisTransaction = qty * (item_price + itemSalesTax);
        
        // costOfThisTransaction could have more than two decimal places, which is awkward...limit to 2.  
        costOfThisTransaction = (double)Math.round(costOfThisTransaction * 100d) / 100d;

        // Error if total cost is greater than available credit
        if (costOfThisTransaction > customerBalance) {
            throw new VoltAbortException("Transaction " + transaction_id + " is too expensive. Cost is "
                    + costOfThisTransaction + " balance is " + customerBalance);
        }

        // Update customer balance
        voltQueueSQL(updBalance, costOfThisTransaction, customerId);

        // record purchase
        voltQueueSQL(recordPurchase, customerId, transaction_id, item_id, qty, item_price, itemSalesTax,
                costOfThisTransaction);

        // send purchase to stream
        voltQueueSQL(recordPurchaseInStream, customerId, transaction_id, item_id, qty, item_price, itemSalesTax,
                costOfThisTransaction, customerZipcode);

        // Note that transaction is now 'official'
        return voltExecuteSQL();

    }

}
