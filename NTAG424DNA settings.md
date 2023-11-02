# Settings and data on a NTAG424DNA tag

## fabric settings:

Below are the settings and data of a tag with fabric settings:

```plaintext
fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: E0
accessRights R  | W:   EE
accessRights RW:       14
accessRights CAR:      0
accessRights R:        14
accessRights W:        14
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 30
accessRights R  | W:   23
accessRights RW:       3
accessRights CAR:      0
accessRights R:        2
accessRights W:        3
fileSize: 128

---------------------
step 0x: read the contents of files 01, 02 and 03 *1)

content file 01: length: 32 
data: 001720010000ff0406e104010000000506e10500808283000000000000000000
content file 02: length: 256 
data: 00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
content file 03: length: 128
data: 007e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

*1) reading of file 03 requires an authentication with application keys 2 (Read key) or key 3 (Read & Write key)
```

## settings after personalization:

Below are the settings and data of a tag with personalized settings:

```plaintext
fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: E0
accessRights R  | W:   EE
accessRights RW:       14
accessRights CAR:      0
accessRights R:        14
accessRights W:        14
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 12
accessRights R  | W:   34
accessRights RW:       1
accessRights CAR:      2
accessRights R:        3
accessRights W:        4
fileSize: 128

---------------------
step 0x: read the contents of files 01, 02 and 03 *1)

content file 01: length: 32 
data: 
data: 001720010000ff0406e104010000000506e10500808283000000000000000000

content file 02: length: 256 
data:
0021d1011d5402656e73616d706c6520746578742042616c616e6365203132332c34350000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

content file 03: length: 128
data: 
data: 007e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

*1) reading of file 03 requires an authentication with application keys 2 (Read key) or key 3 (Read & Write key)
```
