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
package de.bxservice.bxpos.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.bxservice.bxpos.R;
import de.bxservice.bxpos.logic.model.idempiere.MProduct;
import de.bxservice.bxpos.logic.model.idempiere.ProductCategoryinBusinessPartner;
import de.bxservice.bxpos.logic.model.pos.NewOrderGridItem;
import de.bxservice.bxpos.logic.model.idempiere.ProductCategory;
import de.bxservice.bxpos.logic.model.pos.PosProperties;
import de.bxservice.bxpos.ui.CreateOrderActivity;
import de.bxservice.bxpos.ui.adapter.GridOrderViewAdapter;

/**
 * A placeholder fragment containing a simple view.
 * Created by Diego Ruiz on 18/12/15.
 */
public class ProductMenuFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private GridView grid;
    private ArrayList<NewOrderGridItem> mGridData;
    private GridOrderViewAdapter mGridAdapter;
    private List<ProductCategory> productCategoryList;
    private List<ProductCategoryinBusinessPartner> productCategoryListinBP;
    private HashMap<NewOrderGridItem, MProduct> itemProductHashMap;

    public ArrayList<NewOrderGridItem> getmGridData() {
        return mGridData;
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProductMenuFragment newInstance(int sectionNumber) {
        ProductMenuFragment fragment = new ProductMenuFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ProductMenuFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_create_order_activity, container, false);

        int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);

        grid = (GridView) rootView.findViewById(R.id.create_order_gridview);

     //   productCategoryList = ProductCategory.getAllCategories(getActivity().getBaseContext());

        //     ProductCategory productCategory = productCategoryList.get(sectionNumber);

        int bpartner_id = ((CreateOrderActivity) getActivity()).getBP_Partnert_ID();

        productCategoryListinBP = ProductCategoryinBusinessPartner.getAllCategories(getActivity().getBaseContext(), bpartner_id);

        ProductCategoryinBusinessPartner productCategoryinBP = null;

        if (!productCategoryListinBP.isEmpty()) {
            productCategoryinBP = productCategoryListinBP.get(sectionNumber);
        }

        mGridData = new ArrayList<>();
        itemProductHashMap = new HashMap<>();

        NumberFormat currencyFormat = PosProperties.getInstance().getCurrencyFormat();

        NewOrderGridItem item;
        int qtyOrdered;
        if (!productCategoryListinBP.isEmpty()) {
            for (MProduct product : productCategoryinBP.getProducts()) {

                if (product.getProductPrice(getContext(), ((CreateOrderActivity) getActivity()).getBP_PriceList_ID()) != null) {
                    item = new NewOrderGridItem();
                    item.setName(product.getProductName());
                    item.setKey(product.getProductKey());

                    if (!product.askForPrice(((CreateOrderActivity) getActivity()).getBP_PriceList_ID()))
                        item.setPrice(currencyFormat.format(product.getProductPriceValue(((CreateOrderActivity) getActivity()).getBP_PriceList_ID())));
                    else
                        item.setPrice("");

                    //When you navigate through the tabs it paints again everything - this lets the number stay
                    qtyOrdered = ((CreateOrderActivity) getActivity()).getProductQtyOrdered(product);
                    if (qtyOrdered != 0)
                        item.setQty("x" + Integer.toString(qtyOrdered));
                    else
                        item.setQty("");

                    mGridData.add(item);
                    itemProductHashMap.put(item, product);
                }
            }
        }

        grid.setGravity(Gravity.CENTER_HORIZONTAL);
        mGridAdapter = new GridOrderViewAdapter(this.getContext(), R.layout.food_menu_grid_item_layout, mGridData);

        grid.setAdapter(mGridAdapter);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                //Get item at position
                NewOrderGridItem item = (NewOrderGridItem) parent.getItemAtPosition(position);
                MProduct product = itemProductHashMap.get(item);

                ((CreateOrderActivity) getActivity()).addOrderItem(product);

                int productQty = ((CreateOrderActivity) getActivity()).getProductQtyOrdered(product);

                if (productQty != 0)
                    updateQtyOnClick(position, productQty);
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

                //Get item at position
                NewOrderGridItem item = (NewOrderGridItem) parent.getItemAtPosition(position);
                MProduct product = itemProductHashMap.get(item);

                if (!product.askForPrice(((CreateOrderActivity) getActivity()).getBP_PriceList_ID()))
                    ((CreateOrderActivity) getActivity()).addMultipleItems(product);

                return true;
            }
        });

        return rootView;
    }

    public void updateQtyOnClick(int position, int quantity) {
        if (mGridData == null || mGridAdapter == null)
            return;
        mGridData.get(position).setQty("x"+Integer.toString(quantity));
        mGridAdapter.setGridData(mGridData);
    }

    /**
     * Refresh the quantity in the orderItems
     */
    public void refreshAllQty() {
        if (mGridData == null || mGridAdapter == null)
            return;

        for (int i = 0; i < mGridData.size(); i++) {
            MProduct product = itemProductHashMap.get(mGridData.get(i));
            int productQty = ((CreateOrderActivity) getActivity()).getProductQtyOrdered(product);
            if (productQty != 0)
                mGridData.get(i).setQty("x"+Integer.toString(productQty));
            else
                mGridData.get(i).setQty("");
        }
        mGridAdapter.setGridData(mGridData);
    }

}