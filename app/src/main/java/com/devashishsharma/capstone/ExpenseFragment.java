package com.devashishsharma.capstone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.devashishsharma.capstone.data.pojo.Expense;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by devashish.sharma on 12/27/2016.
 */
public class ExpenseFragment extends DialogFragment {
    private static final String ARG_DESC = "desc";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_NEW = "new";

    private String mDescription;
    private double mAmount;
    private boolean mNewExpense;

    @BindView(R.id.edit_expense_amount)
    EditText mAmountTxt;
    @BindView(R.id.edit_expense_description) EditText mDescriptionText;

    private ExpenseDialogListener mListener;

    public ExpenseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ExpenseFragment.
     */
    private static ExpenseFragment newInstance(String description, double amount, boolean newExpense) {
        ExpenseFragment fragment = new ExpenseFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_AMOUNT, amount);
        args.putString(ARG_DESC, description);
        args.putBoolean(ARG_NEW, newExpense);
        fragment.setArguments(args);
        return fragment;
    }

    public static ExpenseFragment newInstance() {
        return newInstance("", 0, true);
    }

    public static ExpenseFragment newInstance(Expense expense) {
        return newInstance(expense.getDescription(), expense.getAmount(), false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDescription = getArguments().getString(ARG_DESC);
            mAmount = getArguments().getDouble(ARG_AMOUNT);
            mNewExpense = getArguments().getBoolean(ARG_NEW);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_expense, null);

        ButterKnife.bind(this, view);

        mDescriptionText.setText(mDescription);
        mAmountTxt.setText(String.valueOf(mAmount));

        if (ExpenseDialogListener.class.isInstance(getActivity())) {
            mListener = ExpenseDialogListener.class.cast(getActivity());
        } else {
            throw new RuntimeException(getActivity().toString()
                    + " must implement ExpenseDialogListener");
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because it's going in the dialog layout
        builder.setView(view)
                // add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final double amount = Double.parseDouble(mAmountTxt.getText().toString());
                        final String description = mDescriptionText.getText().toString();

                        if (amount != mAmount || !description.equals(mDescription)) {
                            mListener.onOk(amount, description);
                        } else {
                            ExpenseFragment.this.getDialog().cancel();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ExpenseFragment.this.getDialog().cancel();
                    }
                });
        if (!mNewExpense) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mListener.onDelete();
                }
            });
        }
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (ExpenseDialogListener.class.isInstance(context)) {
            mListener = ExpenseDialogListener.class.cast(context);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ExpenseDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ExpenseDialogListener {
        void onOk(double amount, String description);
        void onDelete();
    }
}
