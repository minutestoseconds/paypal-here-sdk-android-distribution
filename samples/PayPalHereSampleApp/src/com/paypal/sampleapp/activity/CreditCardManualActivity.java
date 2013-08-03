/**
 * PayPalHereSDK
 * <p/>
 * Created by PayPal Here SDK Team.
 * Copyright (c) 2013 PayPal. All rights reserved.
 */

package com.paypal.sampleapp.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import com.paypal.merchant.sdk.PayPalHereSDK;
import com.paypal.merchant.sdk.TransactionManager;
import com.paypal.merchant.sdk.TransactionManager.PaymentResponse;
import com.paypal.merchant.sdk.domain.DefaultResponseHandler;
import com.paypal.merchant.sdk.domain.DomainFactory;
import com.paypal.merchant.sdk.domain.ManualEntryCardData;
import com.paypal.merchant.sdk.domain.PPError;
import com.paypal.sampleapp.R;
import com.paypal.sampleapp.util.CommonUtils;

import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * This activity is meant to take a payment and complete a transaction with the customer entering in their card
 * details.
 */
public class CreditCardManualActivity extends MyActivity {

    private static final String LOG = "PayPalHere.CreditCardManual";
    /**
     * Implementing a PaymentResponseHandler to handle the response status of a
     * transaction.
     */
    DefaultResponseHandler<PaymentResponse, PPError<TransactionManager.PaymentErrors>> mPaymentResponseHandler = new
            DefaultResponseHandler<PaymentResponse, PPError<TransactionManager.PaymentErrors>>() {
                public void onSuccess(PaymentResponse response) {
                    updateUIForPurchaseSuccess(response);
                    mAnotherTransButton.setVisibility(View.VISIBLE);
                    purchaseButtonClicked(false);
                }

                @Override
                public void onError(PPError<TransactionManager.PaymentErrors> e) {
                    updateUIForPurchaseError(e);
                    mAnotherTransButton.setVisibility(View.VISIBLE);
                    mReUseShoppingCartButton.setVisibility(View.VISIBLE);
                    purchaseButtonClicked(false);

                }
            };
    private TextView mExpDate;
    private String mCCInfo;
    private String mCCVInfo;
    private String mPostalCode;
    private Button mAnotherTransButton;
    private Button mPurchaseButton;
    private Button mReUseShoppingCartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_card_manual);

        // Find and set the edit text for retrieving the entered expiration date on the credit card.
        mExpDate = ((TextView) findViewById(R.id.exp_date));

        // Find and set the button when the current transaction is complete and
        // the user wants to perform another.
        mAnotherTransButton = (Button) findViewById(R.id.another_transaction_button_manual);
        mAnotherTransButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                performAnotherTransaction();
            }
        });
        mAnotherTransButton.setVisibility(View.GONE);

        // Find and set the button when the merchant wants to take a payment from the entered credit card details.
        mPurchaseButton = (Button) findViewById(R.id.credit_card_purchase_button);
        mPurchaseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Check if the entered credit card information is valid.
                if (isInputValid()) {
                    takePayment();
                }
            }

        });

        mReUseShoppingCartButton = (Button) findViewById(R.id.reuse_cart_button_manual);
        mReUseShoppingCartButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reInstateShoppingCart();
                mPurchaseButton.setVisibility(View.VISIBLE);
            }
        });

        mReUseShoppingCartButton.setVisibility(View.GONE);

        // Find and set the button when the customer wants to select the expiration date of their credit card.
        Button b = (Button) findViewById(R.id.exp_date_button);
        b.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                // Display the date picker fragment.
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(CreditCardManualActivity.this.getFragmentManager(), "datePicker");
            }
        });

    }

    /**
     * Method to check in the entered credit card info is valid.
     *
     * @return
     */
    private boolean isInputValid() {
        return (isValidCreditCardInfo() && isValidCCVInfo() && isValidPostalCode());
    }

    /**
     * Method to check if the entered credit card number is valid.
     *
     * @return
     */
    private boolean isValidCreditCardInfo() {
        mCCInfo = CommonUtils.getString((EditText) findViewById(R.id.credit_card_number));
        if (CommonUtils.isNullOrEmpty(mCCInfo))
            return false;

        if (mCCInfo.length() != 16)
            return false;

        return true;
    }

    /**
     * Method to check in the entered cc number is valid.
     *
     * @return
     */
    private boolean isValidCCVInfo() {
        mCCVInfo = CommonUtils.getString((EditText) findViewById(R.id.ccv));
        if (CommonUtils.isNullOrEmpty(mCCVInfo))
            return false;

        if (mCCVInfo.length() < 2 || mCCVInfo.length() > 4)
            return false;

        return true;
    }

    /**
     * Method to check if the postal code entered is valid.
     *
     * @return
     */
    private boolean isValidPostalCode() {
        mPostalCode = CommonUtils.getString((EditText) findViewById(R.id.postal_code));
        if (CommonUtils.isNullOrEmpty(mPostalCode))
            return false;

        if (mPostalCode.length() != 5)
            return false;

        return true;
    }

    /**
     * Method to take a payment from the keyed in credit card info.
     */
    private void takePayment() {

        purchaseButtonClicked(true);

        // Create a ManualEntryCardData obj by providing the credit card into.
        ManualEntryCardData manualEntryCardData = DomainFactory.newManualEntryCardData(mCCInfo,
                mExpDate.getText().toString(), mCCVInfo);
        // Set the Card Holder's name.
        manualEntryCardData.setCardHoldersName("John Doe");

        // Hide the purchase button
        mPurchaseButton.setVisibility(View.GONE);

        displayPaymentState("Taking Payment...");
        // Call the SDK to take a payment.

        // **NOTE**: The transaction state i.e., the shopping cart, the transaction extras,
        // previously read credit cards etc would be kept intact only between the begin - finalize payments. If the
        // payment goes through successfully or if it returns back with a failure,
        // all the above mentioned objects are removed and the app would need to call beginPayment once again to
        // re-init, set the shopping cart back (which they would be holding onto) and try again.
        PayPalHereSDK.getTransactionManager().finalizePayment(manualEntryCardData, mPaymentResponseHandler);

    }

    /**
     * This method updates the UI screen in case of any errors during the
     * transaction.
     *
     * @param e
     */
    private void updateUIForPurchaseError(PPError<TransactionManager.PaymentErrors> e) {
        TransactionManager.PaymentErrors error_type = e.getErrorCode();

        if (TransactionManager.PaymentErrors.PaymentDeclined == error_type) {
            displayPaymentState("Payment declined!  Payment cycle complete.  Please start again");
        } else if (TransactionManager.PaymentErrors.NetworkTimeout == error_type) {
            displayPaymentState("Payment timed out at network level.");
        } else if (TransactionManager.PaymentErrors.NoDeviceForCardPresentPayment == error_type) {
            displayPaymentState("No Device connected.  Connect your device.");
        } else if (TransactionManager.PaymentErrors.NoCardDataPresent == error_type) {
            displayPaymentState("We can't take card payment ... no card has been scanned.");
        } else if (TransactionManager.PaymentErrors.TransactionCanceled == error_type) {
            displayPaymentState("Payment Canceled after takePayment.  No more payment");
        } else if (TransactionManager.PaymentErrors.TimeoutWaitingForSwipe == error_type) {
            displayPaymentState("Payment Canceled.  Expecting card swipe but no swipe ever happened");
        } else if (TransactionManager.PaymentErrors.BadConfiguration == error_type) {
            displayPaymentState("Payment Canceled.  Incorrect Usage / Bad Configuration " + e.getDetailedMessage());
        } else if (TransactionManager.PaymentErrors.EmptyShoppingCart == error_type) {
            displayPaymentState("You've got an empty shopping cart, or a cart with zero value.  Can't process payment");
        } else {
            displayPaymentState("Unhandled error: " + e.getDetailedMessage());
        }
    }

    /**
     * This method updates the UI screen with the transaction id in case of a successful transaction.
     *
     * @param response
     */
    private void updateUIForPurchaseSuccess(PaymentResponse response) {
        displayPaymentState("Payment completed successfully!  TransactionId: " + response.getTransactionRecord()
                .getTransactionId());

        mAnotherTransButton.setVisibility(View.VISIBLE);
    }

    /**
     * This method is meant to display messages on the UI screen.
     *
     * @param state
     */
    private void displayPaymentState(String state) {
        Log.d(LOG, "state: " + state);
        TextView tv = (TextView) findViewById(R.id.purchase_status_manual);
        tv.setText(state);
    }

    /**
     * This method is called to take another payment after the completion of the current one.
     */
    private void performAnotherTransaction() {
        Log.d(LOG, "Peforming another transaction");
        Intent intent = new Intent(CreditCardManualActivity.this, BillingTypeTabActivity.class);
        startActivity(intent);
    }

    /**
     * This class is meant to display a date picker fragment from which the customers could select an expiration date
     * of their card.
     */
    @SuppressLint({"NewApi", "ValidFragment"})
    public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @SuppressLint("NewApi")
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, year, month, day);

            try {
                Field[] datePickerDialogFields = dpd.getClass().getDeclaredFields();
                for (Field datePickerDialogField : datePickerDialogFields) {
                    if (datePickerDialogField.getName().equals("mDatePicker")) {
                        datePickerDialogField.setAccessible(true);
                        DatePicker datePicker = (DatePicker) datePickerDialogField.get(dpd);
                        Field datePickerFields[] = datePickerDialogField.getType().getDeclaredFields();
                        for (Field datePickerField : datePickerFields) {
                            if ("mDayPicker".equals(datePickerField.getName())) {
                                datePickerField.setAccessible(true);
                                Object dayPicker = new Object();
                                dayPicker = datePickerField.get(datePicker);
                                ((View) dayPicker).setVisibility(View.GONE);
                            }
                        }
                    }

                }
            } catch (Exception ex) {
                Log.e(LOG, "error while removing the date part of the date picker!!!");
            }

            return dpd;
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            CreditCardManualActivity.this.mExpDate.setText(String.format("%d%d", (month + 1), year));
        }
    }

    @Override
    protected void onResume() {
       super.onResume();
        if(isPurchaseClicked()) {
            mPurchaseButton.setVisibility(View.GONE);
            mAnotherTransButton.setVisibility(View.GONE);
        }
    }
}
