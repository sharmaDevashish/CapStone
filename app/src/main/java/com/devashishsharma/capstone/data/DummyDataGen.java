package com.devashishsharma.capstone.data;

import android.content.Context;

import com.devashishsharma.capstone.Utils;
import com.devashishsharma.capstone.data.ExpensesContract.ExpensesEntry;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by devashish.sharma on 12/27/2016.
 */
public class DummyDataGen {

    private final Context context;
    private final String email;

    public DummyDataGen(Context context, String email) {
        this.context = context;
        this.email = email;
    }

    private void insertExpense(String desc, double amount, int month, int year) {
        context.getContentResolver().insert(ExpensesEntry.CONTENT_URI,
                Utils.expenseValues(email, desc, amount, month, year));
    }

    private void insertMonth(int month, int year, double totalExpenses) {
        int num = 10;
        double expense = totalExpenses/num;

        for (int i = 0; i < num; i++) {
            insertExpense("expense #"+i, expense, month, year);
        }
    }

    public void generateExpenses() {
        Calendar calendar = Calendar.getInstance();
        Random rng = new Random();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int minExpenses = 1;
        int maxExpenses = 4;

        for (int i = 0; i < 6 && month-i >= 0; i++) {
            insertMonth(month-i, year, 1000*(rng.nextInt(maxExpenses-minExpenses)+minExpenses));
        }
    }
}
