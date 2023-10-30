# Canteen application with NXP's NTAG424DNA tag

This app has 4 fragments showing the different handling of data on the tag:

## 1 home fragment

The **home fragment** is the clients screen providing the (remaining) value on the tag and the transaction log file.

## 2 cashier fragment

This is the main workplace for the cashier that is selling goods. The cashier can be a vending machine of course.

The app is using a very simple goods and and machine logging system (use 256 different product groups and
cash register numbers).

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
All data is secured by a 12 bytes long checksum based on a SHA-256 hash calculation but shortened to have a complete
(exported) file size of 48 bytes.

# Virtual Cyclic Records File

As the NTAG424DNA tag does not provide a Cyclic Records File I'm using a **Virtual Cyclic Records File** that can be placed 
in a Standard File and acts like a real Cyclic Records File. On file creation you set a key and maximum number of records.  

Each record has a fixed size of 16 bytes and a maximum number of records of 10 entries.

You can add a record, show the last record, get a list with all records (sorted by storage time) and clear the complete file.

All **writing access** is secured by a key using a key derivation (PBKDF2, 10000 iterations, 16 bytes resulting key length, 
algorithm PBKDF2WithHmacSHA1).

All data is secured by a 12 bytes long checksum based on a SHA-256 hash calculation but shortened to a multiple of 16 bytes export 
size.

# data security

This app is a sample application showing the functionality **but not optimized for security**. For example the 
application keys are not stored securely in a secured keystore but placed in plain static data in source code. 
Do not use this in a real world application.

# Storage management

As the free user memory on a NTAG424DNA tag is limited this is the usage of the 3 available Standard Files: 

## file number 00 (size: 32 bytes): usage a NDEF compatibility container

 
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

