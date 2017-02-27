package com.devashishsharma.capstone;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import com.devashishsharma.capstone.data.ExpensesContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by devashish.sharma on 12/27/2016.
 */
public class ExpenseAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = ExpenseAppWidgetProvider.class.getSimpleName();

    public static final String ACTION_UPDATE_TOTAL_SPENT = "update.spent";
    public static final String EXTRA_EMAIL = "email";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG, "onUpdate called on " + Arrays.toString(appWidgetIds));

        for (int widgetId : appWidgetIds) {
            String email = WidgetConfigureActivity.loadEmailPref(context, widgetId);
            updateAppWidget(context, appWidgetManager, widgetId, email);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (ACTION_UPDATE_TOTAL_SPENT.equals(action)) {
            String email = intent.getStringExtra(EXTRA_EMAIL);
            Log.i("AppWidgetProvider", "update widget for " + email);

            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, ExpenseAppWidgetProvider.class);
            int[] appWidgetIds = mgr.getAppWidgetIds(cn);

            // filter widgets and only keep ones related to passed email
            List<Integer> filteredWidgetIds = new ArrayList<>();
            for (int appWidgetId : appWidgetIds) {
                if (email.equals(WidgetConfigureActivity.loadEmailPref(context, appWidgetId))) {
                    filteredWidgetIds.add(appWidgetId);
                }
            }

            appWidgetIds = new int[filteredWidgetIds.size()];
            int idx = 0;
            for (int appWidgetId : filteredWidgetIds) {
                appWidgetIds[idx++] = appWidgetId;
                updateAppWidget(context, mgr, appWidgetId, email);
            }
        }

        super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, String email) {
        Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " email=" + email);
        new SpentTask(context, appWidgetManager, appWidgetId, email).execute((Void[]) null);
    }

    public static class SpentTask extends AsyncTask<Void, Void, Double> {

        private final Context context;
        private final AppWidgetManager appWidgetManager;
        private final int appWidgetId;
        private final String email;

        private RemoteViews remoteViews;

        SpentTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                  String email) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
            this.email = email;
        }

        @Override
        protected void onPreExecute() {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.expense_appwidget);
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(MainActivity.ACTION_ADD_EXPENSE);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
        }

        @Override
        protected Double doInBackground(Void... emails) {
            double totalSpent = 0.0D;

            final Calendar cal = Calendar.getInstance();
            final int month = cal.get(Calendar.MONTH);
            final int year = cal.get(Calendar.YEAR);

            
            Cursor spentCursor = context.getContentResolver().query(
                    ExpensesContract.buildSpent(email, month, year), null, null, null, null);

            if (spentCursor == null) return totalSpent;

            if (spentCursor.moveToFirst()) {
                totalSpent = spentCursor.getDouble(0);
            }

            spentCursor.close();

            return totalSpent;
        }

        @Override
        protected void onPostExecute(Double result) {
            remoteViews.setTextViewText(R.id.widget_text, Utils.formatCurrency(result));
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }
}
