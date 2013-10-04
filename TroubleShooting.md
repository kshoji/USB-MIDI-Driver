TroubleShooting on connecting an USB MIDI device
================================================

When the sample application can't detect my USB MIDI device:
------------------------------------------------------------

### Step 1. Check host's connectivity with the tool app.

Check with using the [USB Device Info](https://play.google.com/store/apps/details?id=aws.apps.usbDeviceEnumerator) tool.

- Disconnect the USB MIDI device.
- Open the `USB Device Info` app, and **select `Android` tab. Not `Linux` tab.**
- At first, push `Refresh` button once. Then connect the USB MIDI device.
- Push `Refresh` button again.
- Now, if the device information has been appended, the Android may connect the USB MIDI device.
    - If nothing has changed, the Android can not detect the device...

### Step 2. Check the USB device's Interface

Continue seeing the `USB Device Info` app's `Interface`'s `Class` information.

**The `Audio Device (0x1)` Class must be existed one or more.**

If you found only the other Classes(CDC, HID or something), the device seems not an standard USB MIDI device.
The library can connect with non standard USB MIDI device, if the device using USB MIDI compatible protocol.

### Step 3. Check the USB device's Endpoint

Seek all `Endpoint` information.
The USB MIDI compatible device have one or more `Endpoint`s.

And check these conditions.

- **Type is `Bulk` or `Interrupt`.**
- **Direction is `Inbound` or `Outbound`, or both of them.**

If the `Endpoint` informations matches above conditions, the device can be detected with this driver.
Then, write down the `VendorId` information of the device.

### Step 4. Modify the `device_filter.xml` file

- Git clone this project. And import into Eclipse workspace.
- Choose the library project, and open `res/xml/device_filter.xml` file.

You can see the line like this.

```xml
    <usb-device vendor-id="1177" />
```

**Add similar line in the xml file, and change `vendor-id` to the device's `VendorId`.**

*Caution: The app's displaying id value is **Hexadecimal**, but the xml's value must be **decimal**.*

### Step 5. Test the xml modification

#### Test device detection

- Uninstall original sample application from the Android device.
- Build the sample app, and run with Android device.
- When app has launched, then connect the USB MIDI device.
- If the configuration succeed, Sample application will detect the device.

#### Test data transferring

- Send some MIDI messages from the device, and check the informations are shown correctly.
- Send some MIDI messages to the device, and check the device works correctly.

### Step 6. Contribute to the project

If you could connect and use the device, please email me the device `VendorId` information, or do `fork`, modify the xml file, and `pull request` on github please.
