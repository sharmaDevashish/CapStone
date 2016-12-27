package com.devashishsharma.capstone;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.devashishsharma.capstone.data.DummyDataGen;
import com.devashishsharma.capstone.data.ExpensesContract;
import com.devashishsharma.capstone.data.pojo.Expense;
import com.devashishsharma.capstone.data.pojo.SummaryPoint;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements  ExpenseFragment.ExpenseDialogListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";

    private static final String EXPENSE_DIALOG_TAG="EXPENSE_DIALOG";
    private static final String SUMMARY_DIALOG_TAG="SUMMARY_DIALOG";

    public static final String EXTRA_EMAIL = "email";
    public static final String ACTION_ADD_EXPENSE = "add_expense";

    private static final int EXPENSES_LOADER_ID = 1007;
    private static final int SPENT_LOADER_ID = 2008;

    private String mEmail;

    private double mBudget;
    private double mSpent;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.list)
    RecyclerView mListRecycler;
    @BindView(R.id.search_expense)
    SearchView mSearchView;

    @BindView(R.id.budget_progress)
    RoundCornerProgressBar mBudgetBar;
    @BindView(R.id.budget_spent)
    TextView mBudgetSpent;
    @BindView(R.id.budget_available) TextView mBudgetAvailable;

    private long edited = -1;

    private Tracker mTracker;
    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        Intent intent = getIntent();
        boolean addExpense = false;
        if (ACTION_ADD_EXPENSE.equals(intent.getAction())) {
            //TODO fix this by requesting sign in at this point
            mEmail = "sharma.devashish001@gmail.com";
            addExpense = true;
        } else {
            mEmail = intent.getStringExtra(EXTRA_EMAIL);
            Log.i(TAG, "Signed in as " + mEmail);
        }

        getLoaderManager().initLoader(EXPENSES_LOADER_ID, null, this);
        getLoaderManager().initLoader(SPENT_LOADER_ID, null, this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        mBudget = Double.parseDouble( sharedPref.getString(SettingsActivity.PREF_BUDGET, "4000"));

        setupBudgetView();
        initSearchView();

        // Obtain the shared Tracker instance.
        ExpensesApplication application = (ExpensesApplication) getApplication();
        mTracker = application.getDefaultTracker();

        if (addExpense) {
            showAddDialog();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader " + id);
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);

        if (id == EXPENSES_LOADER_ID) {
            return new CursorLoader(this,
                    ExpensesContract.buildGetAllExpensesUri(mEmail, month, year), null, null, null, null);
        } else {
            return new CursorLoader(this,
                    ExpensesContract.buildSpent(mEmail, month, year), null, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished " + loader.getId());
        if (loader.getId() == EXPENSES_LOADER_ID) {
            mAdapter = new Adapter(data);
            mAdapter.setHasStableIds(true);
            mListRecycler.setAdapter(mAdapter);
            mListRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            if (mAdapter.getItemCount() == 0) {
                Log.d(TAG, "Generating dummy data");
                new DummyDataGen(this, mEmail).generateExpenses();
            }

        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mBudget = Double.parseDouble( sharedPref.getString(SettingsActivity.PREF_BUDGET, "4000"));

            if (data.moveToFirst()) {
                mSpent = data.getDouble(0);
            }

            mBudgetBar.setProgress((float) mSpent);
            mBudgetSpent.setText(Utils.formatCurrency(mSpent));
            mBudgetAvailable.setText(Utils.formatCurrency(mBudget - mSpent));

            updateWidget();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset " + loader.getId());
        if (loader.getId() == EXPENSES_LOADER_ID) {
            mListRecycler.setAdapter(null);
            mAdapter = null;
        } else {
            mBudgetBar.setProgress(0);
            mBudgetSpent.setText(Utils.formatCurrency(0));
            mBudgetAvailable.setText(Utils.formatCurrency(mBudget));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "shared preference changed");
        getLoaderManager().restartLoader(SPENT_LOADER_ID, null, this);
    }

    private void updateWidget() {
        Log.d(TAG, "updating widget spent = " + mSpent);
        Intent intent = new Intent(this, ExpenseAppWidgetProvider.class);
        intent.setAction(ExpenseAppWidgetProvider.ACTION_UPDATE_TOTAL_SPENT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[0]);
        intent.putExtra(ExpenseAppWidgetProvider.EXTRA_EMAIL, mEmail);
        sendBroadcast(intent);
    }

    private void initSearchView() {
        mSearchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        mSearchView.clearFocus();
                        return true;
                    }
                    @Override
                    public boolean onQueryTextChange(String query) {
                        if (mAdapter != null) {
                            mAdapter.filter(query);
                        }
                        return true;
                    }
                }
        );
    }

    private void setupBudgetView() {
        mBudgetBar.setMax((float) mBudget);
        mBudgetBar.setProgress(0);
        mBudgetSpent.setText(Utils.formatCurrency(0));
        mBudgetAvailable.setText(Utils.formatCurrency(mBudget));
    }

    private List<SummaryPoint> getExpensesSummary() {

        final Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);

        Cursor results = getContentResolver().query(
                ExpensesContract.buildSummaryUri(mEmail, month, year), null, null, null, null);

        List<SummaryPoint> summary = new ArrayList<>();
        if (results != null) {
            results.moveToFirst();
            while (!results.isAfterLast()) {
                summary.add(SummaryPoint.of(results));
                results.moveToNext();
            }
            results.close();
        }

        return summary;
    }

    @OnClick(R.id.budget_progress)
    public void budgetBarClick() {
        FragmentManager fm = getFragmentManager();
        List<SummaryPoint> summary = getExpensesSummary();
        SummaryFragment dialog = SummaryFragment.newInstance(summary.toArray(new SummaryPoint[summary.size()]));
        dialog.show(fm, SUMMARY_DIALOG_TAG);

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Budget")
                .setAction("Click")
                .build());
    }

    @OnClick(R.id.fab)
    public void fabClick() {
        showAddDialog();
    }

    private void showAddDialog() {
        FragmentManager fm = getFragmentManager();
        ExpenseFragment dialog = ExpenseFragment.newInstance();
        dialog.show(fm, EXPENSE_DIALOG_TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            Intent signOutIntent = new Intent(this, SignInActivity.class);
            signOutIntent.setAction(SignInActivity.ACTION_SIGNOUT);
            startActivity(signOutIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOk(double amount, String description) {
        if (edited == -1) {
            Log.i(TAG, "Added new Expense(" + amount + ", " + description + ")");

            Calendar cal = Calendar.getInstance();
            getContentResolver().insert(ExpensesContract.ExpensesEntry.CONTENT_URI,
                    Utils.expenseValues(mEmail, description, amount,
                            cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)));

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Expense")
                    .setAction("Add")
                    .build());
        } else {
            Log.i(TAG, "Edited existing Expense(" + amount + ", " + description + ")");

            ContentValues values = new ContentValues();
            values.put(ExpensesContract.ExpensesEntry.COLUMN_DESC, description);
            values.put(ExpensesContract.ExpensesEntry.COLUMN_AMOUNT, amount);

            getContentResolver().update(ExpensesContract.buildExpenseUri(edited), values, null, null);

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Expense")
                    .setAction("Edit")
                    .build());
        }

        edited = -1;
    }

    @Override
    public void onDelete() {
        if (BuildConfig.DEBUG) {
            if (edited == -1) throw new IllegalStateException("We are trying to delete a new expense");
        }

        Log.i(TAG, "Deleting expense");
        getContentResolver().delete(ExpensesContract.buildExpenseUri(edited), null, null);
        edited = -1;

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Expense")
                .setAction("Delete")
                .build());
    }

    static class ExpenseHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.expense_description) TextView description;
        @BindView(R.id.expense_amount) TextView amount;

        public ExpenseHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }


    private class Adapter extends RecyclerView.Adapter<ExpenseHolder> {
        private List<Expense> expenses;
        private List<Expense> filtered;

        public Adapter(Cursor cursor) {
            expenses = new ArrayList<>();
            filtered = new ArrayList<>();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Expense expense = Expense.from(cursor);
                expenses.add(expense);
                filtered.add(expense);
                cursor.moveToNext();
            }
        }

        @Override
        public long getItemId(int position) {
            return filtered.get(position).getId();
        }

        @Override
        public ExpenseHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.expenselist_item, parent, false);
            final ExpenseHolder eh = new ExpenseHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = eh.getAdapterPosition();
                    edited = Adapter.this.getItemId(position);
                    Expense expense = filtered.get(position);

                    final FragmentManager fm = getFragmentManager();
                    ExpenseFragment dialog = ExpenseFragment.newInstance(expense);
                    dialog.show(fm, EXPENSE_DIALOG_TAG);
                }
            });
            return eh;
        }

        @Override
        public void onBindViewHolder(ExpenseHolder holder, int position) {
            Expense expense = filtered.get(position);
            holder.description.setText(expense.getDescription());
            holder.amount.setText(expense.getFormattedAmount());


        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        public void filter(String text) {
            String query = text.toLowerCase();
            filtered.clear();
            for (Expense expense : expenses) {
                String all = expense.getDescription().toLowerCase() + " " + expense.getFormattedAmount().toLowerCase();
                if (all.contains(query)) {
                    filtered.add(expense);
                }
            }
            notifyDataSetChanged();
        }
    }

}
