// Copyright 2017 Michael Goderbauer. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ContactPickerPlugin.h"
@import ContactsUI;

@interface ContactPickerPlugin ()<CNContactPickerDelegate>
@end

@implementation ContactPickerPlugin {
  FlutterResult _result;
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar {
  FlutterMethodChannel *channel =
      [FlutterMethodChannel methodChannelWithName:@"contact_picker"
                                  binaryMessenger:[registrar messenger]];
  ContactPickerPlugin *instance = [[ContactPickerPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    NSArray *myMethods = [NSArray arrayWithObjects: @"selectPhone", @"selectContact", @"selectEmail", nil];
    // Check if the method call exists.
    if ([myMethods containsObject:call.method])
    {
        if (_result)
        {
            //Making custom error Flutter reply to method called
            _result([FlutterError errorWithCode:@"multiple_requests"
                                  message:@"Cancelled by a second request."
                                  details:nil]);
            _result = nil;
        }
        
        _result = result;

        CNContactPickerViewController *contactPicker = [[CNContactPickerViewController alloc] init];
        contactPicker.delegate = self;
      
        //Check what method was selected.
        if([@"selectEmail" isEqualToString:call.method])
        {
            contactPicker.displayedPropertyKeys = @[ CNContactEmailAddressesKey ];
        } else
        {
            contactPicker.displayedPropertyKeys = @[ CNContactPhoneNumbersKey ];
        }
      
        UIViewController *viewController = [UIApplication sharedApplication].delegate.window.rootViewController;
        [viewController presentViewController:contactPicker animated:YES completion:nil];
  }
    else
    {
        result(FlutterMethodNotImplemented);
    }
}

- (void)contactPicker:(CNContactPickerViewController *)picker
    didSelectContactProperty:(CNContactProperty *)contactProperty {
    //Getting Contact Name
  NSString *fullName = [CNContactFormatter stringFromContact:contactProperty.contact
                                                       style:CNContactFormatterStyleFullName];
    
    //Defining variables and setting default values
    NSString *contactInfoType = @"number";
    NSString *contactDictionaryType = @"phoneNumber";
    NSString *contactInfo = nil;
    NSString *contactLabel = nil;
    NSDictionary *contactDictionary = nil;
    
    //Getting contact Property type. email or phone.
    NSString *contactType = contactProperty.key;
    
     //Check if contact is an email or phone
  if([contactType rangeOfString:@"email"].location != NSNotFound){
      // Changing default values
     contactInfoType = @"address";
     contactDictionaryType = @"emailAddress";
      
     //Creating new object with a contact property and label;
     contactInfo = contactProperty.contact.emailAddresses.firstObject.value;
     contactLabel = contactProperty.contact.emailAddresses.firstObject.label;
 }
    //Creating new object with a contact property and label;
  else{
      contactInfo = contactProperty.contact.phoneNumbers.firstObject.value.stringValue;
      contactLabel = contactProperty.label;
      }
    
    //Creating a String Dictionary with [emailAddress : address, emailLabel : label];
   contactDictionary = [NSDictionary dictionaryWithObjectsAndKeys: contactInfo, contactInfoType,  [CNLabeledValue localizedStringForLabel:contactLabel], @"label", nil];

    //Saving fullName and emailAddress Dictionary into result
    _result([NSDictionary
        dictionaryWithObjectsAndKeys:fullName, @"fullName", contactDictionary, contactDictionaryType, nil]);
 
  _result = nil;
}

- (void)contactPickerDidCancel:(CNContactPickerViewController *)picker {
  _result(nil);
  _result = nil;
}

@end
