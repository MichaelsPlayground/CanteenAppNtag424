# Canteen application with NXP's NTAG424DNA tag

This sample application shows how to run a complex app on a NFC tag that is not designed for this kind of application. 
The NTAG 424 DNA is produced by NXP and has a user memory of 416 bytes that is not available in one block but divided 
in 3 **Standard Files**:
- file 01 with a memory of 32 bytes,
- file 02 with a memory of 256 bytes and 
- file 03 with a memory of 128 bytes.

The access to the the files can get key secured - the tag offers an AES-128 encrypted protection.

It's nice to have a pre defined file structure but two file types are missing for a payment application: a **Value File** 
for holding the deposits and a **Cyclic Record File** for a transaction log management. But the file 02 is large enough to 
store a virtual value and cyclic records file in the memory (for a deeper description see below).

But I need to place a warning note: If you would use a Mifare DESFire EVx tag instead you could finalize the transaction 
with a **commit command** that is not applicable on Standard files. That means you cannot control the file status when 
the user moves the the tag out of NFC reader field. My recommendation is: the user should lay the tag in a bowl until the 
transaction is finished.

This app has 4 fragments showing the different handling of data on the tag:

## 1 home fragment

The **home fragment** is the client's screen providing the (remaining) value on the tag and the transaction log file.

## 2 cashier fragment

This is the main workplace for the cashier that is selling goods. The cashier can be a vending machine of course.

The app is using a very simple goods and and machine logging system (use 256 different product groups and
cashier machine numbers).

As the deposit is hosted in a **Virtual Value file** there is no real amount but only "units" available. This 
app is not using a "currency" field. For example, 100 "units" are 100 cent meaning 1 USD or 1 Euro. 
The **maximum debit value is 99999 units** (e.g. 999,99 USD / Euro), the **minimum debit value is 1 unit**. 
When (re-) charging the card there is a **maximum deposit amount of 9999999 units** (e.g. 99999,99 USD /Euro).

## 3 charge fragment

The canteen card is empty after issuing and needs to get (re-) charged or credited. For security reasons it is a good practise 
to separate this functionality from the cashier position.

## 4 personalization fragment

This fragment prepares the tag for the usage as canteen card:

1) setting of indiviual application keys
- read data (cyclic record file a standard file)
- write data (virtual value file, cyclic record file a standard file)
- read and write data ( virtual value file, cyclic record file a standard file)
2) formats the 3 **Standard files** on the tag for usage as NDEF compatibility container, NDEF data, card holders data, value file, cyclic record file.
3) setup the **Virtual Cyclic Records File** for the confirmed transaction log
4) setup the **Virtual Value File** for usage.
5) The file numbers 00 and 02 are in plain communication mode, file number 01 will operate in full enciphered communication.

Please note that the tag requires a NTAG424DNA tag with fabric settings, especially regarding the application keys.

# Virtual Value File

The NTAG424DNA is a tag with a predefined memory usage - it has 3 standard files of different sizes. Unfortunately 
there is no value file available for this NFC tag type (e.g. on a DESFire Light or EVx tag you can setup the tag for such a file 
in an application).

For this reason I'm using a **Virtual Value File** that can be placed in one of the standard files and acts like a 
"real" one - you can credit or debit the value and get the balance. All **writing access** is secured by a key using 
a key derivation (PBKDF2, 10000 iterations, 16 bytes resulting key length, algorithm PBKDF2WithHmacSHA1). 
All **data is secured by a 12 bytes long checksum based on a SHA-256 hash** calculation but shortened to have a complete
(exported) file size of 48 bytes.

# Virtual Cyclic Records File

As the NTAG424DNA tag does not provide a Cyclic Records File I'm using a **Virtual Cyclic Records File** that can be placed 
in a Standard File and acts like a real Cyclic Records File. On file creation you set a key and maximum number of records.  

Each record has a fixed size of 16 bytes and a maximum number of records of 10 entries.

You can add a record, show the last record, get a list with all records (sorted by storage time) and clear the complete file.

All **writing access** is secured by a key using a key derivation (PBKDF2, 10000 iterations, 16 bytes resulting key length, 
algorithm PBKDF2WithHmacSHA1).

All **data is secured by a 12 bytes long checksum based on a SHA-256 hash** calculation but shortened to a multiple of 16 bytes export 
size.

# data security

This app is a sample application showing the functionality **but not optimized for security**. For example the 
application keys are not stored securely in a secured keystore but placed in plain static data in source code. 
Do not use this in a real world application.

# Storage management

As the free user memory on a NTAG424DNA tag is limited this is the usage of the 3 available Standard Files: 

## file number 00 (size: 32 bytes): usage a NDEF compatibility container

```plaintext
original content (compatibility container):
001720010000ff0406e104010000000506e10500808283000000000000000000

0017 cclen = 23 bytes
    20 mapping version 2.0
      0100 MLe 256 bytes
          00ff MLc 255 bytes
              NDEF-File Ctrl TLV
              04 indicates the NDEF-File_Ctrl_TLV
                06 6 bytes
                  e104 NDEF File Identifier = E104h 
                      0100 NDEF-File Size = 0100h 256 bytes
                          00 NDEF-File READ Access Condition = 00h, i.e. READ access granted without any security
                            00 NDEF-File WRITE Access Condition = 00h, i.e. WRITE access granted without any security
                              Proprietary-File_Ctrl_TLV
                              05 indicates the Proprietary-File_Ctrl_TLV
                                06 6 bytes
                                  e105 Proprietary-File File Identifier = E105h
                                      0080 Proprietary-File Size = 0080h 128 bytes
                                          82 Proprietary-File READ Access Condition = 82h, i.e. Limited READ access, granted
                                             based on proprietary methods, after authentication with key 2h
                                            83 Proprietary-File WRITE Access Condition = 83h, i.e. Limited READWRITE access,
                                               granted based on proprietary methods, after authentication with key 3h.
                                              000000000000000000 empty

modified content (compatibility container) for file 02 = E104h with 192 bytes ndef memory:
00172000c000bf0406e10400c000000000000000000000000000000000000000 free write access
00172000c000bf0406e10400c000800000000000000000000000000000000000 write access prohibited

0017 cclen = 23 bytes
    20 mapping version 2.0
      00C0 MLe 192 bytes
          00bf MLc 191 bytes
              NDEF-File Ctrl TLV
              04 indicates the NDEF-File_Ctrl_TLV
                06 6 bytes
                  e104 NDEF File Identifier = E104h 
                      0080 NDEF-File Size = 0080h 128 bytes
                      00c0 NDEF-File Size = 00c0h 192 bytes
                          00 NDEF-File READ Access Condition = 00h, i.e. READ access granted without any security
                            00 NDEF-File WRITE Access Condition = 00h, i.e. WRITE access granted without any security
                            80 NDEF-File WRITE Access Condition = 80h, i.e. WRITE access locked 
                               000000000000000000 empty
 

modified content (compatibility container) for file 03 = E105h:
0017200080007f0406e105008000000000000000000000000000000000000000

0017 cclen = 23 bytes
    20 mapping version 2.0
      0080 MLe 128 bytes
          007f MLc 127 bytes
              NDEF-File Ctrl TLV
              04 indicates the NDEF-File_Ctrl_TLV
                06 6 bytes
                  e105 NDEF File Identifier = E105h 
                      0080 NDEF-File Size = 0080h 128 bytes
                          00 NDEF-File READ Access Condition = 00h, i.e. READ access granted without any security
                            00 NDEF-File WRITE Access Condition = 00h, i.e. WRITE access granted without any security
                               000000000000000000 empty
 
```
 
## file number 01 (size: 256 bytes): usage as card holders data, settings and extended log file

 
## file number 02 (size: 128 bytes): usage as NDEF placeholder for value and last transaction record and a digital signature



## Project status: not started yet

Datasheet for NTAG424DNA:

Application note 12xxxx Features and Hints: https://www.nxp.com/docs/en/application-note/AN12196.pdf


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

