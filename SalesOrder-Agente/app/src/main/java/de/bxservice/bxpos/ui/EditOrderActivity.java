/**********************************************************************
 * This file is part of FreiBier POS                                   *
 *                                                                     *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Diego Ruiz - Bx Service GmbH                                      *
 **********************************************************************/
package de.bxservice.bxpos.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.bxservice.bxpos.logic.model.idempiere.CBPartner;
import de.bxservice.bxpos.logic.model.idempiere.DefaultPosData;
import de.bxservice.bxpos.logic.model.idempiere.Table;
import de.bxservice.bxpos.logic.model.pos.POSOrder;
import de.bxservice.bxpos.logic.model.pos.POSOrderLine;
import de.bxservice.bxpos.R;
import de.bxservice.bxpos.logic.model.pos.PosProperties;
import de.bxservice.bxpos.logic.print.POSOutputDevice;
import de.bxservice.bxpos.logic.print.POSOutputDeviceValues;
import de.bxservice.bxpos.logic.tasks.PrintOrderTask;
import de.bxservice.bxpos.ui.adapter.EditPagerAdapter;
import de.bxservice.bxpos.ui.dialog.ConfirmationPinDialogFragment;
import de.bxservice.bxpos.ui.dialog.GuestNumberDialogFragment;
import de.bxservice.bxpos.ui.dialog.SelectOrderDialogFragment;
import de.bxservice.bxpos.ui.dialog.KitchenNoteDialogFragment;
import de.bxservice.bxpos.ui.dialog.RemarkDialogFragment;
import de.bxservice.bxpos.ui.dialog.SplitOrderDialogFragment;
import de.bxservice.bxpos.ui.dialog.SwitchTableDialogFragment;
import de.bxservice.bxpos.ui.dialog.VoidQuantityDialogFragment;
import de.bxservice.bxpos.ui.dialog.VoidReasonDialogFragment;
import de.bxservice.bxpos.ui.fragment.OrderedItemsFragment;
import de.bxservice.bxpos.ui.fragment.OrderingItemsFragment;

public class EditOrderActivity extends AppCompatActivity implements GuestNumberDialogFragment.GuestNumberDialogListener,
        RemarkDialogFragment.RemarkDialogListener, KitchenNoteDialogFragment.KitchenDialogListener,
        SwitchTableDialogFragment.SwitchTableDialogListener, SelectOrderDialogFragment.SelectOrderDialogListener,
        SplitOrderDialogFragment.SplitOrderDialogListener, VoidReasonDialogFragment.VoidReasonDialogListener,
        ConfirmationPinDialogFragment.ConfirmationPinDialogListener, VoidQuantityDialogFragment.VoidQuantityDialogListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private EditPagerAdapter mEditPagerAdapter;

    private static final String ORDER_STATE = "orderState";
    private static final String SPLIT_ITEMS = "splitItems";
    private static final String VOID_ITEMS  = "voidItems";
    private static final String VOID_QTY    = "voidQty";
    public final static String EXTRA_ORDER   = "de.bxservice.bxpos.ORDER";
    public final static String DRAFT_ORDER   = "de.bxservice.bxpos.DRAFT_ORDER";
    public final static String CALLER_ACTIVITY        = "de.bxservice.bxpos.CALLER";
    public final static String EXTRA_NUMBER_OF_GUESTS = "de.bxservice.bxpos.GUESTS";
    public final static String EXTRA_ASSIGNED_TABLE   = "de.bxservice.bxpos.TABLE";

    static final int ADD_ITEMS_REQUEST  = 1;  // The request code
    static final int PAY_REQUEST = 2;  // The request code
    static final int NEW_ORDER_REQUEST  = 3;  // The request code

    private View mainView;
    private POSOrder order;
    private CBPartner param_bpartner = null;
    private String   caller; //Indicates the activity that called it
    private static HashMap<FloatingActionButton, Boolean> fabVisible = new HashMap<>();

    private FloatingActionButton sendButton;
    private FloatingActionButton payButton;

    private TextView qtyTextView;
    private TextView totalTextView;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private boolean addNewItemsOnBack = false;
    private boolean orderSent = false;
    private boolean orderChanged = false;

    //Split order array
    private ArrayList<POSOrderLine> selectedItemsToSplit = null;

    //Calls for CAB menu in different fragments
    private ActionMode mActionMode;

    //Voiding items
    private ArrayList<Integer> positionItemsToVoid = null;
    private int qtyItemsToVoid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_order);

        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_order_toolbar);
        if (toolbar != null)
           setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getExtras();

        mainView = findViewById(R.id.main_layout);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mEditPagerAdapter = new EditPagerAdapter(getSupportFragmentManager(), getBaseContext(), order);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.editTabViewPager);
        mViewPager.setAdapter(mEditPagerAdapter);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.openOrderTabs);
        tabs.setViewPager(mViewPager);

        sendButton = (FloatingActionButton) findViewById(R.id.fabSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!order.getOrderingLines().isEmpty() && order.sendOrder(getApplicationContext())) {
                    orderSent = true;
                    DefaultPosData defaultPosData = DefaultPosData.get(getBaseContext());

                    //If the option is marked in POS Terminal -> Print
                    if (defaultPosData.isPrintAfterSent())
                        printOrder(false);

                    finish();
                } else
                    Snackbar.make(mainView, R.string.empty_order, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        });

        payButton = (FloatingActionButton) findViewById(R.id.fabPay);
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (order.getOrderingLines().isEmpty() && order.getOrderedLines().isEmpty())
                    Snackbar.make(mainView, R.string.empty_order, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                else if (order.getOrderingLines() != null && !order.getOrderingLines().isEmpty()) {
                    new AlertDialog.Builder(EditOrderActivity.this)
                            .setTitle(R.string.unsent_lines)
                            .setNegativeButton(R.string.remove, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface arg0, int arg1) {
                                    order.removeOrderingItems();
                                    openPaymentActivity();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null)
                            .setPositiveButton(R.string.send_items, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface arg0, int arg1) {
                                    order.sendOrder(getApplicationContext());
                                    openPaymentActivity();
                                }
                            }).create().show();
                } else
                    openPaymentActivity();
            }
        });

        //Listen to tab swipes
        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                animateFab(position);
                updateSummary(position);
                if(mActionMode != null)
                    mActionMode.finish();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };

        mViewPager.addOnPageChangeListener(onPageChangeListener);

        qtyTextView   = (TextView) findViewById(R.id.qty_textView);
        totalTextView = (TextView) findViewById(R.id.total_textView);

        setActiveTab();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_order, menu);
        MenuItem guestItem = menu.findItem(R.id.set_guests);

        if (guestItem != null) {
            if (!DefaultPosData.get(getBaseContext()).isShowGuestDialog())
                guestItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == R.id.add_item) {
            if (caller.equals("CreateOrderActivity")) {
                addNewItemsOnBack = true;
                onBackPressed();
                return true;
            } else if (caller.equals("MainActivity") ||
                    caller.equals("ViewOpenOrdersActivity")) {
                openAddItemsActivity();
                return true;
            }
        }
        if (id == R.id.set_guests) {
            showGuestNumberDialog();
            return true;
        }
        if (id == R.id.add_note) {
            showRemarkDialog();
            return true;
        }
        if (id == R.id.change_table) {
            showTransferTableDialog();
            return true;
        }
        if (id == R.id.split_order) {
            showSplitOrderDialog();
            return true;
        }
        if (id == R.id.join_orders) {
            showJoinOrdersDialog();
            return true;
        }
        if (id == R.id.new_order) {
            createNewOrder();
            return true;
        }
        if (id == R.id.print_order) {
            printOrder(true);
            return true;
        }
        if (id == R.id.void_order) {
            showVoidOrderReasonDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the active tab depending on the caller
     * From create -> ordering
     *      view open orders
     *      main Activity -> ordered
     */
    private void setActiveTab() {
        if("CreateOrderActivity".equals(caller)) {
            fabVisible.put(sendButton, true);
            payButton.hide();
            fabVisible.put(payButton, false);
            updateSummary(EditPagerAdapter.ORDERING_POSITION);
        } else {
            fabVisible.put(sendButton, false);
            sendButton.hide();
            fabVisible.put(payButton, true);
            updateSummary(EditPagerAdapter.ORDERED_POSITION);

            mViewPager.setCurrentItem(EditPagerAdapter.ORDERED_POSITION, false);

        }
    }

    private void showRemarkDialog() {
        RemarkDialogFragment remarkDialog = new RemarkDialogFragment();
        remarkDialog.setNote(order.getOrderRemark());
        remarkDialog.show(getFragmentManager(), "RemarkDialogFragment");
    }

    private void showGuestNumberDialog() {
        // Create an instance of the dialog fragment and show it
        GuestNumberDialogFragment guestDialog = new GuestNumberDialogFragment();
        guestDialog.setNumberOfGuests(order.getGuestNumber());
        guestDialog.show(getFragmentManager(), "NumberOfGuestDialogFragment");
    }

    private void showTransferTableDialog() {
        SwitchTableDialogFragment changeTableDialog = new SwitchTableDialogFragment();
        changeTableDialog.show(getFragmentManager(), "SwitchTableDialogFragment");
    }

    private void showJoinOrdersDialog() {
        //If the open orders are more than one
        if (POSOrder.getOpenOrders(getBaseContext()).size() > 1) {
            SelectOrderDialogFragment joinOrdersDialog = new SelectOrderDialogFragment();
            joinOrdersDialog.setOrder(order);
            joinOrdersDialog.setIsJoin(true);
            joinOrdersDialog.show(getFragmentManager(), "SelectOrderDialogFragment");
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.unable_join),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showSplitOrderDialog() {

        //If only one item - show error
        if(order.getOrderedLinesNoVoid().size() <= 1) {
            Snackbar.make(mainView, getString(R.string.unable_split), Snackbar.LENGTH_LONG)
                    .setAction("OK", null).show();
            return;
        }

        SplitOrderDialogFragment splitOrderDialog = new SplitOrderDialogFragment();
        splitOrderDialog.setOrder(order);
        splitOrderDialog.show(getFragmentManager(), "SplitOrderDialogFragment");
    }

    private void showKitchenNoteDialog() {
        KitchenNoteDialogFragment kitchenNoteDialog = new KitchenNoteDialogFragment();
        List<Integer> selectedItemPositions = getSelectedItems();
        if(selectedItemPositions != null && selectedItemPositions.size() == 1) {
            POSOrderLine orderLine = order.getOrderingLines().get(selectedItemPositions.get(0));
            kitchenNoteDialog.setOrderLine(orderLine);
            kitchenNoteDialog.setNote(orderLine.getProductRemark());
            kitchenNoteDialog.show(getFragmentManager(), "KitchenDialogFragment");
        }
    }

    private void showVoidQtyDialog() {
        int maxQtyVoid = 0;

        if (positionItemsToVoid != null)
            maxQtyVoid = order.getOrderedLines().get(positionItemsToVoid.get(0)).getQtyOrdered();

        if (maxQtyVoid > 1) {
            VoidQuantityDialogFragment voidQuantityDialog = new VoidQuantityDialogFragment();
            if (positionItemsToVoid != null)
                voidQuantityDialog.setMaxQty(maxQtyVoid);
            voidQuantityDialog.show(getFragmentManager(), "VoidQuantityDialogFragment");
        } else {
            showVoidReasonDialog(maxQtyVoid);
        }

    }

    private void showVoidReasonDialog(int noItems) {
        VoidReasonDialogFragment voidReasonDialog = new VoidReasonDialogFragment();
        voidReasonDialog.setVoidOrder(false);
        voidReasonDialog.setNoItems(noItems);
        voidReasonDialog.show(getFragmentManager(), "VoidReasonDialogFragment");
    }

    private void showVoidOrderReasonDialog() {
        VoidReasonDialogFragment voidReasonDialog = new VoidReasonDialogFragment();
        voidReasonDialog.setVoidOrder(true);
        voidReasonDialog.show(getFragmentManager(), "VoidReasonDialogFragment");
    }

    private void showConfirmationPINDialog(String reason, boolean voidOrder) {
        ConfirmationPinDialogFragment confirmationPinDialog = new ConfirmationPinDialogFragment();
        confirmationPinDialog.setVoidOrder(voidOrder);
        confirmationPinDialog.setReason(reason);

        if (voidOrder)
            confirmationPinDialog.setNoItems(Integer.parseInt(order.getOrderNo()));
        else if (positionItemsToVoid != null)
            confirmationPinDialog.setNoItems(positionItemsToVoid.size());

        confirmationPinDialog.show(getFragmentManager(), "ConfirmationPinDialogFragment");
    }

    /**
     * Click set on guest number dialog
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(GuestNumberDialogFragment dialog) {
        // User touched the dialog's positive button
        int guests = dialog.getNumberOfGuests();
        order.setGuestNumber(guests);
        order.updateOrder(getBaseContext());
    }

    /**
     * Click add on add remark dialog
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(RemarkDialogFragment dialog) {
        // User touched the dialog's positive button
        String note = dialog.getNote();
        order.setOrderRemark(note);
        order.updateOrder(getBaseContext());
    }

    /**
     * Click add on add kitchen note
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(KitchenNoteDialogFragment dialog) {
        // User touched the dialog's positive button
        String note = dialog.getNote();
        dialog.getOrderLine().setProductRemark(note);
        dialog.getOrderLine().updateLine(getBaseContext());
    }

    /**
     * Click add on Change table dialog
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(SwitchTableDialogFragment dialog) {

        if (dialog.getTable() != null) {

            Table originalTable = order.getTable();
            order.setTable(dialog.getTable());
            order.getTable().setServerName(order.getServerName(getBaseContext()));
            order.getTable().occupyTable(getBaseContext(), true);
            order.updateOrder(getBaseContext());

            //If table exists - free it up
            if(originalTable != null)
                originalTable.freeTable(getBaseContext(), true);

            Toast.makeText(getBaseContext(), getString(R.string.table_switched),
                    Toast.LENGTH_LONG).show();

            orderChanged = true;
        }
    }

    /**
     * Click on an item on join orders
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(final SelectOrderDialogFragment dialog) {

        if(dialog.getOrder() != null && dialog.isJoin()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.join_order_question)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.join, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            order.joinOrders(dialog.getOrder(), getBaseContext());
                            orderChanged = true;
                            EditOrderActivity.super.recreate();
                        }
                    }).create().show();
        }
        // If the option is split
        else if (!dialog.isJoin()) {
            order.splitOrder(dialog.getOrder(), selectedItemsToSplit, getBaseContext());
            orderChanged = true;
            EditOrderActivity.super.onBackPressed();
        }
    }

    /**
     * Click on split button
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(SplitOrderDialogFragment dialog) {
        if(dialog.getSelectedLines() != null && !dialog.getSelectedLines().isEmpty()) {
            selectedItemsToSplit = dialog.getSelectedLines();

            //If all the items were selected
            if(selectedItemsToSplit.size() == order.getOrderedLinesNoVoid().size()) {
                Snackbar snackbar = Snackbar
                        .make(mainView, getString(R.string.split_all_Error), Snackbar.LENGTH_LONG)
                        .setAction("OK", null);
                snackbar.show();

                return;
            }

            SelectOrderDialogFragment joinOrdersDialog = new SelectOrderDialogFragment();
            joinOrdersDialog.setOrder(order);
            joinOrdersDialog.setIsJoin(false);
            joinOrdersDialog.show(getFragmentManager(), "SelectOrderDialogFragment");
        }
    }

    /**
     * Click on void button
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(VoidReasonDialogFragment dialog) {
        if(dialog.getReason() != null) {

            DefaultPosData data = DefaultPosData.get(getBaseContext());

            if(data.getPin() != 0) {
                showConfirmationPINDialog(dialog.getReason(), dialog.isVoidOrder());
            } else {
                if(dialog.isVoidOrder())
                    voidOrder(dialog.getReason());
                else
                    voidSelectedItems(dialog.getReason());
            }
        }
        dialog.dismiss();
    }

    /**
     * Click on approve button
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(ConfirmationPinDialogFragment dialog) {
        if(dialog.getPinCode() != null && !TextUtils.isEmpty(dialog.getPinCode())) {

            DefaultPosData data = DefaultPosData.get(getBaseContext());
            int assignedPin = data.getPin();

            if(assignedPin == 0) {
                Snackbar.make(mainView, R.string.assign_password, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            if(assignedPin != Integer.parseInt(dialog.getPinCode())) {
                Snackbar.make(mainView, R.string.wrong_password, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {
                if(dialog.isVoidOrder())
                    voidOrder(dialog.getReason());
                else
                    voidSelectedItems(dialog.getReason());
            }
        }
        dialog.dismiss();
    }

    @Override
    public void onDialogPositiveClick(VoidQuantityDialogFragment dialog) {
        qtyItemsToVoid = dialog.getQty();
        showVoidReasonDialog(qtyItemsToVoid);
        dialog.dismiss();
    }

    /**
     * Get extras from the previous activity
     * - Order
     */
    private void getExtras() {

        Intent intent = getIntent();

        if (intent != null) {
            order = (POSOrder) intent.getSerializableExtra(DRAFT_ORDER);
            caller = intent.getStringExtra(CALLER_ACTIVITY);
        }
    }

    public void onBackPressed() {

        if(order != null &&
                !order.getStatus().equals(POSOrder.DRAFT_STATUS) &&
                caller.equals("CreateOrderActivity")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.discard_draft_order)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            EditOrderActivity.super.onBackPressed();
                        }
                    }).create().show();
        }
        else
            super .onBackPressed();
    }

    /**
     * On recreate keep the value of orderChanged
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ORDER_STATE, orderChanged);
        savedInstanceState.putSerializable(SPLIT_ITEMS, selectedItemsToSplit);
        savedInstanceState.putSerializable(VOID_ITEMS, positionItemsToVoid);
        savedInstanceState.putInt(VOID_QTY, qtyItemsToVoid);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        orderChanged = savedInstanceState.getBoolean(ORDER_STATE);
        selectedItemsToSplit = (ArrayList<POSOrderLine>) savedInstanceState.getSerializable(SPLIT_ITEMS);
        positionItemsToVoid = (ArrayList<Integer>) savedInstanceState.getSerializable(VOID_ITEMS);
        qtyItemsToVoid = savedInstanceState.getInt(VOID_QTY);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            onBackPressed();
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * Fab options to hide and show on each tab
     * @param position
     */
    private void animateFab(int position) {

        switch (position) {

            case EditPagerAdapter.ORDERING_POSITION:
                fabVisible.put(sendButton, true);
                fabVisible.put(payButton, false);
                sendButton.show();
                payButton.hide();
                break;

            case EditPagerAdapter.ORDERED_POSITION:
                fabVisible.put(sendButton, false);
                fabVisible.put(payButton, true);
                payButton.show();
                sendButton.hide();
                break;

            default:
                sendButton.show();
                payButton.hide();
                break;
        }
    }

    public static Boolean isFabVisible(FloatingActionButton child) {
        return fabVisible.get(child);
    }

    /**
     * Displays the qty and the total in each tab
     * @param position
     */
    private void updateSummary(int position) {

        switch (position) {

            case EditPagerAdapter.ORDERING_POSITION:
                qtyTextView.setText(getTotalQuantity(POSOrderLine.ORDERING));
                totalTextView.setText(getTotalAmount(POSOrderLine.ORDERING));
                break;

            case EditPagerAdapter.ORDERED_POSITION:
                qtyTextView.setText(getTotalQuantity(POSOrderLine.ORDERED));
                totalTextView.setText(getTotalAmount(POSOrderLine.ORDERED));
                break;

            default:
                qtyTextView.setText(getTotalQuantity(POSOrderLine.ORDERING));
                totalTextView.setText(getTotalAmount(POSOrderLine.ORDERING));
                break;
        }

    }

    /**
     * Returns total amount of items depending
     * on the tab focus
     * @param status
     * @return
     */
    public String getTotalQuantity(String status) {

        if (order == null)
            return "";

        int totalQty = 0;

        switch (status) {
            case POSOrderLine.ORDERING:
                totalQty = order.getOrderingQty();
                break;
            case POSOrderLine.ORDERED:
                totalQty = order.getOrderedQty();
        }

        return getString(R.string.quantity_summary, totalQty);
    }

    /**
     * Returns total amount to pay depending
     * on the tab focus
     * @param status
     * @return
     */
    public String getTotalAmount(String status) {

        if (order == null)
            return "";

        NumberFormat currencyFormat = PosProperties.getInstance().getCurrencyFormat();

        switch (status) {
            case POSOrderLine.ORDERING:
                return getString(R.string.subtotal_value, currencyFormat.format(order.getTotalOrderinglines(order.getCB_PriceList_ID())));
            case POSOrderLine.ORDERED:
                return getString(R.string.total_value, currencyFormat.format(order.getTotallines(order.getCB_PriceList_ID())));
            default:
                return getString(R.string.subtotal_value, currencyFormat.format(order.getTotalOrderinglines(order.getCB_PriceList_ID())));
        }

    }

    /**
     * Removes an item when swipe in the list view
     * updates the qty and subtotal
     * @param position
     */
    public void removeItem(int  position) {
        order.removeItem(position);
        updateSummary(EditPagerAdapter.ORDERING_POSITION);
    }

    /**
     * Undo the delete
     * @param position
     * @param orderLine
     */
    public void addItem(int  position, POSOrderLine orderLine) {
        order.addItem(position, orderLine);
        updateSummary(EditPagerAdapter.ORDERING_POSITION);
    }

    @Override
    public void finish() {
        //When new items want to be added - persist the changes in guests and notes
        if (caller.equals("CreateOrderActivity") && addNewItemsOnBack) {
            Intent data = new Intent();
            data.putExtra(EXTRA_ORDER, order);
            setResult(RESULT_OK, data);
        }
        else if (orderSent) {
            setResult(2); //Everything ok - the order was created and the create Activity should be closed
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
        } else if (orderChanged)
            setResult(3); //Back button without sending the order - but order changed
        else //nothing changed
            setResult(RESULT_CANCELED);


        super.finish();
    }

    /**
     * Click on add items menu item
     */
    private void openAddItemsActivity() {
        Intent intent = new Intent(this, CreateOrderActivity.class);
        intent.putExtra(CreateOrderActivity.CALLER_ACTIVITY,"EditOrderActivity");
        intent.putExtra(EXTRA_ORDER, order);

        startActivityForResult(intent, ADD_ITEMS_REQUEST);
    }

    /**
     * Click on new order menu item
     */
    private void createNewOrder() {
        Intent intent = new Intent(this, CreateOrderActivity.class);
        intent.putExtra(CreateOrderActivity.CALLER_ACTIVITY, "MainActivity");
        intent.putExtra(EXTRA_NUMBER_OF_GUESTS, 1); //Always one by default
        intent.putExtra(EXTRA_ASSIGNED_TABLE, order.getTable());

        startActivityForResult(intent, NEW_ORDER_REQUEST);
    }

    private void updateOrderingItems() {
        OrderingItemsFragment itemsFragment = (OrderingItemsFragment) getFragment(EditPagerAdapter.ORDERING_POSITION);

        if(itemsFragment != null) {
            itemsFragment.refresh(order);
        }
    }

    /**
     * When it comes back from the create order Activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ADD_ITEMS_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                order = (POSOrder) data.getExtras().get(EXTRA_ORDER);

                updateOrderingItems();
                mViewPager.setCurrentItem(EditPagerAdapter.ORDERING_POSITION, false);
                orderChanged = true;

                getIntent().putExtra(DRAFT_ORDER, order);

                this.recreate();
            }
        } else if (requestCode == PAY_REQUEST) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                super.finish();
            }
        } else if (requestCode == NEW_ORDER_REQUEST) {
            orderChanged = true;
            finish();
        }
    }

    /**
     * When Long click - shows the CAB and perform the corresponding action
     * @param idx
     */
    public void onLongPressed(int idx, int fragmentPosition){
        if(mActionMode != null) {
            myToggleSelection(idx, fragmentPosition);
            return;
        }

        if(fragmentPosition == EditPagerAdapter.ORDERING_POSITION)
            mActionMode = EditOrderActivity.this.startSupportActionMode(new OrderingActionBarCallBack());
        else if(fragmentPosition == EditPagerAdapter.ORDERED_POSITION)
            mActionMode = EditOrderActivity.this.startSupportActionMode(new OrderedActionBarCallBack());

        myToggleSelection(idx, fragmentPosition);
    }

    /**
     * Only works when long click was performed previously
     * @param idx
     */
    public void onClickPressed(int idx, int fragmentPosition){
        if(mActionMode != null) {
            myToggleSelection(idx, fragmentPosition);
        }
    }

    private void myToggleSelection(int idx, int fragmentPosition) {

        switch(fragmentPosition) {

            case EditPagerAdapter.ORDERING_POSITION:
                OrderingItemsFragment orderingFragment = (OrderingItemsFragment) getFragment(fragmentPosition);

                if(orderingFragment != null) {
                    orderingFragment.getmAdapter().toggleSelection(idx);
                    if(orderingFragment.getmAdapter().getSelectedItemCount() == 0) {
                        mActionMode.finish();
                        return;
                    }
                    String title = getString(R.string.selected_count, orderingFragment.getmAdapter().getSelectedItemCount());
                    mActionMode.setTitle(title);
                }
                break;

            case EditPagerAdapter.ORDERED_POSITION:
                OrderedItemsFragment orderedFragment = (OrderedItemsFragment) getFragment(fragmentPosition);

                if(orderedFragment != null) {
                    orderedFragment.getAdapter().toggleSelection(idx);
                    if(orderedFragment.getAdapter().getSelectedItemCount() == 0) {
                        mActionMode.finish();
                        return;
                    }
                    String title = getString(R.string.selected_count, orderedFragment.getAdapter().getSelectedItemCount());
                    mActionMode.setTitle(title);
                }
                break;

        }

    }

    private void clearSelections(int fragmentPosition) {

        switch(fragmentPosition) {

            case EditPagerAdapter.ORDERING_POSITION:
                OrderingItemsFragment orderingFragment = (OrderingItemsFragment) getFragment(fragmentPosition);
                if(orderingFragment != null)
                    orderingFragment.getmAdapter().clearSelections();
                break;

            case EditPagerAdapter.ORDERED_POSITION:
                OrderedItemsFragment orderedFragment = (OrderedItemsFragment) getFragment(fragmentPosition);
                if(orderedFragment != null)
                    orderedFragment.getAdapter().clearSelections();
                break;

        }


    }

    private void deleteSelectedItems() {
        OrderingItemsFragment itemsFragment = (OrderingItemsFragment) getFragment(EditPagerAdapter.ORDERING_POSITION);

        if(itemsFragment != null) {
            itemsFragment.getmAdapter().removeSelectedItems();
        }
    }

    private void copySelectedItems() {
        OrderingItemsFragment itemsFragment = (OrderingItemsFragment) getFragment(EditPagerAdapter.ORDERING_POSITION);

        if(itemsFragment != null) {
            itemsFragment.getmAdapter().copySelectedItems();
        }
    }

    private void setItemsAsComplimentary() {
        final List<Integer> selectedItemPositions = getSelectedItems();
        new AlertDialog.Builder(this)
                .setTitle(R.string.give_complimentary_product)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.give, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        if(selectedItemPositions != null && !selectedItemPositions.isEmpty()) {
                            for(int i = 0; i< selectedItemPositions.size(); i++) {
                                POSOrderLine orderLine = order.getOrderingLines().get(selectedItemPositions.get(i));
                                if (orderLine.getProduct().isComplimentaryAllow(order.getCB_PriceList_ID())) {
                                    orderLine.setComplimentaryProduct(true);
                                    orderLine.updateLine(getBaseContext());
                                }
                                else {
                                    Toast.makeText(getBaseContext(), getString(R.string.no_allow_complimentary, orderLine.getProduct().getProductName()),
                                            Toast.LENGTH_LONG).show();
                                }
                                EditOrderActivity.this.recreate();
                            }
                        }
                    }
                }).create().show();
    }

    /**
     * Check all the lines selected and see if they can be voided
     * @return
     */
    private boolean isValidVoidAttempt() {
        OrderedItemsFragment itemsFragment = (OrderedItemsFragment) getFragment(EditPagerAdapter.ORDERED_POSITION);

        if(itemsFragment != null) {
            positionItemsToVoid = itemsFragment.getAdapter().getSelectedItems();

            if (positionItemsToVoid == null || positionItemsToVoid.isEmpty()) {
                positionItemsToVoid = null;
                return false;
            }

            if (!DefaultPosData.get(getBaseContext()).isSeparateOrderItems() && positionItemsToVoid.size() > 1)
                return false;

            for (int i = positionItemsToVoid.size() - 1; i >= 0; i--) {
                if(!order.getOrderedLines().get(positionItemsToVoid.get(i)).isVoidable()) {
                    positionItemsToVoid = null;
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private void voidOrder(String reason) {
        order.voidOrder(getBaseContext(), reason);
        orderChanged = true;
        finish();
    }

    private void voidSelectedItems(String reason) {
        if(positionItemsToVoid != null) {

            if (qtyItemsToVoid > 0 && positionItemsToVoid.size() == 1) {
                order.voidLine(positionItemsToVoid.get(0), reason, qtyItemsToVoid, getBaseContext());
            } else {
                for (int i = positionItemsToVoid.size() - 1; i >= 0; i--) {
                    order.voidLine(positionItemsToVoid.get(i), reason, getBaseContext());
                }
            }

            this.recreate(); //TODO: Improve fragment recreation
        }
        qtyItemsToVoid = 0;
    }

    private List<Integer> getSelectedItems() {
        OrderingItemsFragment itemsFragment = (OrderingItemsFragment) getFragment(EditPagerAdapter.ORDERING_POSITION);

        if(itemsFragment != null) {
            return itemsFragment.getmAdapter().getSelectedItems();
        }

        return null;
    }

    public Fragment getFragment(int position) {
        return getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + mViewPager.getId() + ":"
                        + position);
    }

    private void openPaymentActivity() {
        Intent intent = new Intent(this, PayOrderActivity.class);
        intent.putExtra(PayOrderActivity.COMPLETED_ORDER, order);
        startActivityForResult(intent, PAY_REQUEST);
    }

    /**
     * Print the order items
     * @param showMessage - flag to decide if info message should be displayed
     */
    private void printOrder(boolean showMessage) {

        //Check if there are configured printers
        if(POSOutputDevice.getDevice(getBaseContext(), POSOutputDeviceValues.TARGET_KITCHEN) == null &&
                POSOutputDevice.getDevice(getBaseContext(), POSOutputDeviceValues.TARGET_BAR) == null) {
            if(showMessage)
                Toast.makeText(getBaseContext(), getString(R.string.no_printing_device),
                        Toast.LENGTH_LONG).show();

            return;
        }

        //Check if there are items to print
        if(order.getPrintKitchenLines(getBaseContext()).size() == 0 &&
                order.getPrintBarLines(getBaseContext()).size() == 0) {
            if(showMessage)
                Toast.makeText(getBaseContext(), getString(R.string.no_print_items),
                        Toast.LENGTH_LONG).show();

            return;
        }

        POSOutputDevice printOrderDevice = POSOutputDevice.getDevice(getBaseContext(), POSOutputDeviceValues.TARGET_KITCHEN);

        //If kitchen printer is configured -> print kitchen
        if(printOrderDevice != null &&
                printOrderDevice.getDeviceType().equalsIgnoreCase(POSOutputDeviceValues.DEVICE_PRINTER) &&  //If it is a printer
                order.getPrintKitchenLines(getBaseContext()).size() > 0) { //If there are lines to print
            PrintOrderTask createOrderTask = new PrintOrderTask(this, printOrderDevice);
            createOrderTask.execute(order);
        }

        printOrderDevice = POSOutputDevice.getDevice(getBaseContext(), POSOutputDeviceValues.TARGET_BAR);

        //If bar printer is configured -> print bar
        if(printOrderDevice != null &&
                printOrderDevice.getDeviceType().equalsIgnoreCase(POSOutputDeviceValues.DEVICE_PRINTER) &&  //If it is a printer
                order.getPrintBarLines(getBaseContext()).size() > 0) { //If there are lines to print
            PrintOrderTask createOrderTask = new PrintOrderTask(this, printOrderDevice);
            createOrderTask.execute(order);
        }

    }

    /**
     * Class in charge of the CAB for the ordering tab
     */
    class OrderingActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_ordering_menu, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            clearSelections(EditPagerAdapter.ORDERING_POSITION);
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.ctx_item_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.ctx_item_copy:
                    copySelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.ctx_item_note:
                    showKitchenNoteDialog();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.ctx_complimentary_item:
                    setItemsAsComplimentary();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

    }

    /**
     * Class in charge of the CAB for the ordered tab
     */
    class OrderedActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_ordered_menu, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            clearSelections(EditPagerAdapter.ORDERED_POSITION);
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.ctx_item_void:
                    if (isValidVoidAttempt()) {
                        if (DefaultPosData.get(getBaseContext()).isSeparateOrderItems())
                            showVoidReasonDialog(positionItemsToVoid.size());
                        else
                            showVoidQtyDialog();
                    }
                    else
                        Toast.makeText(getBaseContext(), getString(R.string.already_voided_item),
                                Toast.LENGTH_LONG).show();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

    }

}
