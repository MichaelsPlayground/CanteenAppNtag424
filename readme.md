# Canteen application with NXP's NTAG424DNA tag

This app has 4 fragments showing the different handling of data on the tag:

## 1 home fragment

The **home fragment** is the clients screen providing the (remaining) value on the tag and the transaction log file.

## 2 cashier fragment

This is the main workplace for the cashier that is selling goods. The cashier can be a vending machine of course.

The app is using a very simple goods and and machine logging system (use 256 different product groups and
cash register numbers).

As the deposit is hosted in a **Value file** there is no real amount but only "units" available. This 
app is not using a "currency" field. For example, 100 "units" are 100 cent meaning 1 USD or 1 Euro. 
The **maximum debit value is 99999 units** (e.g. 999,99 USD / Euro), the **minimum debit value is 1 unit**. 
When (re-) charging the card there is a **maximum deposit amount of 9999999 units** (e.g. 99999,99 USD /Euro).

## 3 charge fragment

The canteen card is empty after issuing and needs to get (re-) charged or credited. For security reasons it is a good practise 
to separate this functionality from the cashier position.

## 4 personalization fragment

This fragment prepares the tag for the usage as canteen card:

1) setting of indiviual application keys
- read data (value file, cyclic record file a standard file)
- write data (value file, cyclic record file a standard file)
- read and write data (value file, cyclic record file a standard file)
2) formats the 3 **Standard files** on the tag for usage:
- file number 00 (size: 32 bytes): usage a NDEF compatibility container
- file number 01 (size: 256 bytes): usage as card holders data, settings and extended log file
- file number 02 (size: 128 bytes): usage as NDEF placeholder for value and last transaction record and a digital signature
3) empties the cyclic record file for the confirmed transaction log
4) The file numbers 00 and 02 are in plain communication mode, all other files will operate in full enciphered communication.

Please note that the tag requires a NTAG424DNA tag with fabric settings, especially regarding the application keys.

# data security

This app is a sample application showing the functionality **but not optimized for security**. For example the 
application keys are not stored securely in a secured keystore but placed in plain static data in source code. 
Do not use this in a real world application.


## Note: the following description is for a total different app beause I copied the application

This app is having just one purpose - it prepares **NTAG21x tags** for the usage with **NFC Storage Management** app.

The general idea is a combined system for a smartphone based storage management that uses 3 main components to work with:

1) **user's Android smartphone**: identifying a storage good by tapping the smartphone to the good (or cardboard) with a 
NFC sticker (the NTAG21x). When the tag was registered before the smartphone "knows" the good and you are been able to 
assign a cardboxes content list or photos of the good or content to the entry. All this information is send to the second 
component.
2) **backend internet server**: The smartphone sends the data to the backend server (like a synchronization) where they are 
additionally stored. The actual reason for using an internet server is the access by a personal computer.
3) **personal computer**: As it is a challenge to enter or edit a lot of text a personal computer access is the better way 
to handle and enter all the information regarding the storing goods. Maybe you are having a Word or Pages document with the 
content information and now you can easily copy and paste the data 



## Bulk Registration of tag UIDs

The backend server works on registered tags only. The most common way to register a new tag - after the tag was personalized - 
is to simply tap the tag to the smartphone with enabled internet connection. It will open a browser and calls a page that 
registers the tag UID on the backend server.

This workflow may be good for some new tags but when setting up a new storage management with a lot of existing goods and 
tags you better use this activity. By tapping the personalized tags to the smartphone one after another an internal list with 
tagUIDs fills up and when you're finished press the **EXPORT REGISTRATION LIST button**. A file chooser opens and directs 
to a directory in external storage like "Download" with a predefined filename "bulk.dat". After pressing "save" in the chooser 
a new file is created, containing all tagUIDs separated by a 'new line' character. Use this list as a source for the 
"upload tagUID list function" in the backend server menu.

If the same tag is tapped twice this is noticed and the tag is not added to the list. This is working on the tag data registered 
with this activity lifetime. There is NO check here if the tag was previously registered on the backend server, but this check 
is done during backend server import.

If you leave the activity by returning to home menu the tags registered so far gets deleted.  

## Note on this description

This is the description of the project but not of this app - I will edit this at a later point !


This is a Storage Management Application that helps in managing all cartons and goods stored in a Deposit room. 
The app is using NFC tags of type NXP NTAG 21x to easily identify each position by simply reading the tag with 
an NFC reader embedded in most modern Android smartphones.

There are 3 different tag types available for the NTAG 21x family:
- NTAG213 with 144 bytes of freely available user memory, 137 bytes NDEF capacity
- NTAG215 with 504 bytes of freely available user memory, 480 bytes NDEF capacity
- NTAG216 with 888 bytes of freely available user memory, 868 bytes NDEF capacity

The tags do have a build-in NDEF capability so they are read- and writable with common readers. The provide 
a 7-byte serial number UID) that was programmed by the  manufacturer and are immutable. As additional 
feature the tags can **mirror the UID and a reader counter** into user memory so the can get part of an NDEF   
message. The tags, especially the NTAG213, is available for less as a sticker so it can be easily attached 
to the carton.

As this is a complex system some parts of the work are done using the smartphone and other via Webinterface, 
so will need to available space on a webserver you own. To connect the tag with smartphone and webserver  
I'm using the UID as identifier for all datasets. 

The webserver-URL is coded in the NDEF message as a link like the following example:

https://www.example.com/storage/ident&uid=0123456789ABCDEFx112233

http://fluttercrypto.bplaced.net/apps/ntag/get_reg3.php?uid=01020304050607&mac=32bbe378

http://fluttercrypto.bplaced.net/apps/ntag/get_reg3.php?uid=1d424ab9950000&mac=371f46bc (verified)



The workflow for the management is as follows:

**Preparation before usage*
1 The tags are written for the first usage with an NDEF message that contains the link to a webpage. The mirror 
function is enabled for the UID and the counter, after that the tag get write disabled
2 The tag is identified by the app and an empty webspace file is created (internet connection necessary)
**Usage workflow at storage place**
3 The tag is attached to a carton and read by the app. The user manually adds a carton number (usually something 
written in big letters at the carton), this information is stored in the app internal database
4 The user can make up to 3 photos of the content with the smartphone's camera
5 edit the dataset by manually type in the content (not recommended)
**Usage workflow at the office**
6 Using an internet connection the app is uploading some data to the webspace like cartons content (if collected) 
and the photos
7 Edit the content file for each carton to provide more information about the content using the webspace editor
8 download the content from the webspace to the internal database on the smartphone to have an offline source

Some minor actions can happen: 
- delete an entry because the carton is permanently removed
- mark an entry as absent because the carton is temporary removed
- add/modify/delete some photos

Enhancements:
- encrypt the data on webspace
- use two or more tags to identify the same cartoon (because the tag is attached to a carton side that it not more 
accessible due to storage place)
- add an information where the storage place is in detail (e.g. "row last on left side, 2nd from botton")
- use a multi-user/multi-app system

## Project status: not started yet

Datasheet for NTAG21x: https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf

Icons: https://www.freeiconspng.com/images/nfc-icon

Nfc Simple PNG Transparent Background: https://www.freeiconspng.com/img/20581

<a href="https://www.freeiconspng.com/img/20581">Nfc Png Simple</a>

Icon / Vector editor: https://editor.method.ac/

Minimum SDK is 26 (Android 8)

android:inputType="text|textNoSuggestions"

## Dependencies

```plaintext
// https://mvnrepository.com/artifact/com.google.code.gson/gson
implementation group: 'com.google.code.gson', name: 'gson', version: '2.10.1'

```

```plaintext
tag is writable
templateUrlString: http://fluttercrypto.bplaced.net/apps/ntag/get_reg3.php?uid=11111111111111&mac=22222222
NFC tag discovered
ndef is connected
 onsize change changed 
templateUrlString written to the tag
ndef was connected, trying to close ndef
ndef is closed
connected to the  tag using NfcA technology
tagUid length: 7 data: 1d424ab9950000
checkNtagType
tagIdentifierResponse length: 16 data: 1d424a9db99500002ca30000e1101200
getVersion length: 8 data: 0004040201000f03
tag is of type NTAG213/215/216
The configuration is starting in page 41
nfcA is connected
reading page 41: 020000ff000000000000000000000000
UID length: 7 data: 1d424ab9950000
response page 41: 0a
All mirroring was disabled
 onsize change changed 
ntagMemory length: 144 data: 0103a00c340355d101515503666c757474657263727970746f2e62706c616365642e6e65742f617070732f6e7461672f6765745f726567332e7068703f7569643d3131313131313131313131313131266d61633d3232323232323232fe000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
uidMatchString: ?uid=
macMatchString: &mac=
positionUidMatch: 60 positionMacMatch: 79
positive matching positions, now enable mirroring
pageOfConfiguration: 41 positionOfUid: 60
reading page 41: 020000ff000000000000000000000000
response page 41: 0a
UID mirror was enabled on position: 60
shortenedHash length: 4 data: 371f46bc
writeMacToNdef textView is NOT NULL
The configuration is starting in page 41
pageOfConfiguration: 41 shortenedMacToWrite length: 4 data: 371f46bc macPosition: 79
reading page 41: 420013ff000000000000000000000000
reading page 41: 420013ff000000000000000000000000
reading page 23: 313131266d61633d3232323232323232
response page 23: 0a
response page 24: 0a
SUCCESS on reading page 41 response: 020000ff000000000000000000000000
reading page 25: 3232323232323232fe00000000000000
response page 25: 0a
MAC was written with success on position: 79
The tag was personalized with SUCCESS
Called vibrate(int, String, VibrationEffect, AudioAttributes) API - PUID: 10508, PackageName: de.androidcrypto.nfcstoragemanagementtagpersonalization
semVibrate - PUID: 10508, PackageName: de.androidcrypto.nfcstoragemanagementtagpersonalization, token: android.os.Binder@d79280b, effect: OneShot{mTiming=150, mAmplitude=10}, AudioAttr: null, Mag: -1, TYPE_EXTRA
mirrorPageByte: 00 (= 0 dec)
MIRROR content old:  00000010
SUCCESS on writing page 41 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
preferenceString: ?uid=
Do full code cache collection, code=249KB, data=176KB
After code cache collection, code=247KB, data=142KB
preferenceString: &mac=
positionUidMatch: 60 || positionMacMatch: 79
positive matching positions, now enable mirroring
SUCCESS on reading page 41 response: 020000ff000000000000000000000000
mirrorPageByte: 00 (= 0 dec)
MIRROR content old:  00000010
Do partial code cache collection, code=251KB, data=146KB
After code cache collection, code=251KB, data=146KB
Increasing code cache capacity to 1024KB
newPage: 19
positionInPage: 0
readPageResponse: 420013ff000000000000000000000000
SUCCESS on writing page 41 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
shortenedHash length: 4 data: 371f46bc
SUCCESS on reading page 41 response: 420013ff000000000000000000000000
mirrorPageByte: 13 (= 19 dec)
MIRROR content old:  01000010
newMacPage: 23
positionInPage: 3
positionInPage section 3
SUCCESS on reading page 23 response: 313131266d61633d3232323232323232
SUCCESS on writing page 23 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
SUCCESS on writing page 24 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
SUCCESS on reading page 25 response: 3232323232323232fe00000000000000
SUCCESS on writing page 25 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a

```
```plaintext
NFC tag is Nfca compatible
getVersion length: 8 data: 0004040201000f03
tagUid length: 7 data: 1d424ab9950000
nfcaMaxTransceiveLength: 253
pageOfConfiguration: 41 positionOfUid: 65
reading page 41: 020000ff000000000000000000000000
raw data of NTAG213
number of pages: 36 total memory: 144 bytes
tag ID: 1d424ab9950000
tag ID: 643058778653
maxTranceiveLength: 253 bytes
response page 41: 0a
writeMacToNdef textView is NOT NULL
pageOfConfiguration: 41 shortenedMacToWrite length: 4 data: 371f46bc macPosition: 84
reading page 41: 520014ff000000000000000000000000
response page 25: 0a
ntagDataString:
�4U�QUfluttercrypto.bplaced.net/apps/ntag/get_reg3.php?uid=11111111111111&mac=22222222�??????????????????????????????????????????????????????????????????????????????????????????????????????
response page 26: 0a
preferenceString: ?uid=
preferenceString: &mac=
Called vibrate(int, String, VibrationEffect, AudioAttributes) API - PUID: 10508, PackageName: de.androidcrypto.nfcstoragemanagementtagpersonalization
semVibrate - PUID: 10508, PackageName: de.androidcrypto.nfcstoragemanagementtagpersonalization, token: android.os.Binder@d79280b, effect: OneShot{mTiming=150, mAmplitude=10}, AudioAttr: null, Mag: -1, TYPE_EXTRA
positionUidMatch: 65 || positionMacMatch: 84
positive match positions, now enable mirroring
SUCCESS on reading page 41 response: 020000ff000000000000000000000000
mirrorPageByte: 00 (= 0 dec)
MIRROR content old:  00000010
newPage: 20
positionInPage: 1
readPageResponse: 520014ff000000000000000000000000
SUCCESS on writing page 41 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
Enabling the UID mirror: SUCCESS
tagUid: 1d424ab9950000
shortenedMAC: 371f46bc
SUCCESS on reading page 41 response: 520014ff000000000000000000000000
mirrorPageByte: 14 (= 20 dec)
MIRROR content old:  01010010
newMacPage: 25
positionInPage: 0
positionInPage section 0
SUCCESS on writing page 25 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
SUCCESS on writing page 26 response: 0a
write page to tag: 0a
SUCCESS: enabling the UID mirror with response: 0a
writing the MAC: SUCCESS

```

