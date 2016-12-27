package com.devashishsharma.capstone;

import android.content.ContentValues;

import com.devashishsharma.capstone.data.ExpensesContract;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;

/**
 * Created by devashish.sharma on 12/27/2016.
 */
public class Utils {
    private static String[] months = new DateFormatSymbols().getMonths();

    public static String formatMonth(int month) {
        return months[month];
    }

    public static String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance().format(amount);
    }

    public static ContentValues expenseValues(String email, String desc, double amount, int month, int year) {
        ContentValues values = new ContentValues();
        values.put(ExpensesContract.ExpensesEntry.COLUMN_DESC, desc);
        values.put(ExpensesContract.ExpensesEntry.COLUMN_AMOUNT, amount);
        values.put(ExpensesContract.ExpensesEntry.COLUMN_MONTH, month);
        values.put(ExpensesContract.ExpensesEntry.COLUMN_YEAR, year);
        values.put(ExpensesContract.ExpensesEntry.COLUMN_EMAIL, email);
        return values;
    }
}
