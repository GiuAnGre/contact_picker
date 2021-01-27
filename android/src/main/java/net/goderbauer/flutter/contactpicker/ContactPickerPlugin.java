// Copyright 2017 Michael Goderbauer. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package net.goderbauer.flutter.contactpicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.HashMap;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.app.Activity.RESULT_OK;

public class ContactPickerPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "contact_picker");
        ContactPickerPlugin instance = new ContactPickerPlugin(registrar.activity());
        registrar.addActivityResultListener(instance);
        channel.setMethodCallHandler(instance);
    }

    private ContactPickerPlugin(Activity activity) {
        this.activity = activity;
    }

    private static int PICK_PHONE = 2015;
    private static int PICK_EMAIL = 2016;

    private Activity activity;
    private Result pendingResult;

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("selectPhone")) {
            if (pendingResult != null) {
                pendingResult.error("multiple_requests", "Cancelled by a second request.", null);
                pendingResult = null;
            }
            pendingResult = result;

            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            activity.startActivityForResult(i, PICK_PHONE);
        } else if (call.method.equals("selectEmail")) {
            if (pendingResult != null) {
                pendingResult.error("multiple_requests", "Cancelled by a second request.", null);
                pendingResult = null;
            }
            pendingResult = result;

            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
            activity.startActivityForResult(i, PICK_EMAIL);

        } else {
            result.notImplemented();
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            pendingResult.success(null);
            pendingResult = null;
            return true;
        }
        if (requestCode == PICK_PHONE) {
            Uri contactUri = data.getData();
            Cursor cursor = activity.getContentResolver().query(contactUri, null, null, null, null);
            cursor.moveToFirst();

            int phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            String customLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
            String label = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(activity.getResources(), phoneType, customLabel);
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            HashMap<String, Object> phoneNumber = new HashMap<>();
            phoneNumber.put("number", number);
            phoneNumber.put("label", label);

            HashMap<String, Object> contact = new HashMap<>();
            contact.put("fullName", fullName);
            contact.put("phoneNumber", phoneNumber);

            pendingResult.success(contact);
            pendingResult = null;
            return true;
        } else if (requestCode == PICK_EMAIL) {
            Uri contactUri = data.getData();
            Cursor cursor = activity.getContentResolver().query(contactUri, null, null, null, null);
            cursor.moveToFirst();

            int emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
            String customLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
            String label = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(activity.getResources(), emailType, customLabel);
            String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            String fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            HashMap<String, Object> emailAddress = new HashMap<>();
            emailAddress.put("address", address);
            emailAddress.put("label", label);

            HashMap<String, Object> contact = new HashMap<>();
            contact.put("fullName", fullName);
            contact.put("emailAddress", emailAddress);

            pendingResult.success(contact);
            pendingResult = null;
            return true;
        } else {
            return false;
        }
    }
}
