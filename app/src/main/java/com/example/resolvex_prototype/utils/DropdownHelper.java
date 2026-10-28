package com.example.resolvex_prototype.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Arrays;
import java.util.List;

/**
 * Sets up a read-only dropdown (exposed dropdown menu).
 * A normal ArrayAdapter hides the other options once one is selected (it filters by the text);
 * this adapter always shows the full list.
 */
public class DropdownHelper {

    public interface OnItemPicked {
        void onPicked(int index);
    }

    public static void setup(Context context, MaterialAutoCompleteTextView view, String[] items,
                             int selectedIndex, OnItemPicked callback) {
        final List<String> data = Arrays.asList(items);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_dropdown_item_1line, data) {

            private final Filter showAll = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = data;
                    results.count = data.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    // nothing to do: the adapter always keeps the full list
                }
            };

            @Override
            public Filter getFilter() {
                return showAll;
            }
        };

        view.setAdapter(adapter);

        if (selectedIndex >= 0 && selectedIndex < items.length) {
            view.setText(items[selectedIndex], false);
        }

        view.setOnItemClickListener((parent, v, position, id) -> {
            if (callback != null) {
                callback.onPicked(position);
            }
        });
    }
}
