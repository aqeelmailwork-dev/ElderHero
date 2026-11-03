# Database Structure — `com.example.elderhero_caretakers`

Read-only analysis of the app source under `app/src/main/java`. Only nodes and fields
that the code actually touches are listed.

## 1. Technology

| Item | Evidence |
|---|---|
| **Firebase Realtime Database** (not Firestore, Room or SQLite) | `libs.firebaseDatabase = "22.0.1"` in `gradle/libs.versions.toml`; `implementation libs.firebase.database` in `app/build.gradle`; 40 `FirebaseDatabase.getInstance().getReference(...)` sites; no `@Entity`, `@Dao`, `SQLiteOpenHelper`, `FirebaseFirestore`, `collection(` or `document(` anywhere. |
| Database URL | `app/google-services.json`: `"firebase_url": "https://demo-elderhero-default-rtdb.firebaseio.com"`, project `demo-elderhero`. |
| Authentication | Firebase Auth (`firebase-auth 24.0.1`): `createUserWithEmailAndPassword` (`RegisterActivity.java:306`), `signInWithEmailAndPassword` (`LoginActivity.java:107`), `sendPasswordResetEmail` (`ForgotPasswordActivity.java:72`), `signOut` (`CaretakerDrawer.java:105`). |
| File storage | Firebase Storage (`firebase-storage 22.0.1`): profile images, submission PDFs, ad images, chat images/audio (see §6). |
| Shared backend | `google-services.json` contains two Android clients: `com.example.elderhero` (family app) and `com.example.elderhero_caretakers` (this app), so both apps use the same database. |
| Security rules | No `database.rules.json` / `firebase.json` in the repository. |

## 2. ASCII tree

```
demo-elderhero-default-rtdb.firebaseio.com
├── Admins/
│   └── {adminId}/
│       └── AdminName : String
├── Advertisements/
│   └── {caretakerUid}/
│       └── {adId}/
│           ├── AdvertisementID          : String  (= adId)
│           ├── CaretakerID              : String  (= caretakerUid)
│           ├── CategoriesID             : String
│           ├── SubCategoriesID          : List<String>
│           ├── AdvertisementAvailability: String
│           ├── AdvertisementMinRate     : String
│           ├── AdvertisementMaxRate     : String
│           ├── AdvertisementLocation    : String
│           ├── AdvertisementURL         : String
│           ├── AdvertisementDate        : String  ("yyyy-MM-dd")
│           └── AdvertisementTime        : String  ("HH:mm")
├── Bookings/
│   └── {bookingId}/
│       ├── BookingID        : String
│       ├── CaretakerID      : String  -> Caretakers/{uid}
│       ├── FamiliyElderlyID : String  -> FamilyElderly/{uid}   (spelling as in code)
│       ├── AdvertismentID   : String  -> Advertisements/{CaretakerID}/{adId} (spelling as in code)
│       ├── BookingDate      : String
│       ├── BookingTime      : String
│       ├── BookingDuration  : String
│       ├── BookingNote      : String
│       ├── BookingStatus    : String  ("Pending" | "Accepted" | "Rejected" | "Completed")
│       └── BookingTimeStamp : long
├── CaretakerBookings/
│   └── {caretakerUid}/
│       └── {bookingId}/              (index entry, no fields read)
├── CaretakerManagement/
│   └── {uid}/
│       ├── CaretakerManagementStatus : String  ("Active" | "Warning" | "Suspended")
│       └── CaretakerManagementReason : String
├── Caretakers/
│   └── {uid}/
│       ├── CaretakerID          : String  (= uid)
│       ├── CaretakerName        : String
│       ├── CaretakerEmail       : String
│       ├── CaretakerPhoneNo     : String
│       ├── CaretakerProfession  : String
│       ├── CaretakerExperience  : String
│       ├── CaretakerProfileUrl  : String  (Storage download URL)
│       ├── CaretakerRace        : String
│       ├── CaretakerLanguage    : List<String>
│       ├── CaretakerGender      : String
│       └── CaretakerAge         : String  (age range text)
├── Categories/
│   └── {categoryId}/
│       ├── CategoryName : String
│       └── SubCategories/
│           └── {subCategoryId}/
│               └── SubCategoryName : String
├── ChatThreads/
│   └── {chatId}/
│       ├── chatId         : String  (= chatId)
│       ├── caretakerId    : String  -> Caretakers/{uid}
│       ├── familyElderlyId: String  -> FamilyElderly/{uid}
│       ├── lastMessage    : String
│       └── lastTimestamp  : long
├── FamilyElderly/
│   └── {uid}/
│       ├── FamilyElderlyName   : String
│       ├── FamilyElderlyAge    : number/String (read untyped)
│       ├── FamilyElderlyGender : String
│       ├── FamilyElderlyPhone  : String
│       ├── FamilyElderlyType   : String
│       ├── FamilyElderlyURL    : String
│       ├── username            : String  (legacy fallback)
│       └── imageUrl            : String  (legacy fallback)
├── Messages/
│   └── {chatId}/
│       └── {messageId}/
│           ├── messageId      : String  (= messageId)
│           ├── chatId         : String  (= chatId)
│           ├── senderId       : String
│           ├── receiverId     : String
│           ├── messageType    : String  ("text" | "image" | "voice")
│           ├── messageText    : String | null
│           ├── messageImageURL: String | null
│           ├── messageAudioURL: String | null
│           └── messageTimeStamp: long
├── Reports/
│   └── {bookingId}/
│       └── {reportId}/
│           ├── ReportID          : String  (= reportId)
│           ├── BookingID         : String
│           ├── ReportCategory    : String
│           ├── ReportDetails     : String
│           ├── ReportStatus      : String  ("Pending" written here)
│           ├── ReportTime        : long
│           ├── CaretakerID       : String
│           ├── FamiliyElderlyID  : String
│           ├── AdminID           : null/String
│           ├── ReporterID        : String
│           └── ReporterRole      : String  ("Caretaker")
├── Reviews/
│   └── {bookingId}/
│       ├── CaretakerID   : String  (used by query; never read directly here)
│       ├── ReviewRating  : number
│       └── ReviewDetails : String
└── Submissions/
    └── {submissionId}/
        ├── SubmissionID     : String  (= submissionId)
        ├── CaretakerID      : String  -> Caretakers/{uid}
        ├── SubmissionURL    : String  (Storage download URL)
        ├── SubmissionStatus : String  ("Pending" | "Approved" | "Rejected")
        ├── SubmissionDate   : String  ("yyyy-MM-dd")
        ├── SubmissionTime   : String  ("HH:mm")
        ├── SubmissionRemark : String
        └── AdminID          : String  -> Admins/{adminId} ("" until reviewed)
```

## 3. Per-node details

### 3.1 `Caretakers/{uid}` — model `Caretaker.java`

All fields are `String` except `CaretakerLanguage` (`List<String>`). Keyed by Firebase Auth UID.

| Field | Type | Written by | Read by |
|---|---|---|---|
| `CaretakerID` | String | `RegisterActivity.java:320-334` (`setValue` of whole object) | — (never read; duplicates key) |
| `CaretakerName` | String | same | `CaretakerHomeActivity.java:77`; `CaretakerDrawer.java:185`; `CaretakerProfileActivity.java:89`; `EditCaretakerProfileActivity.java:87`; `BookingAdapter.java:143-145` |
| `CaretakerEmail` | String | same | `CaretakerProfileActivity.java:90` |
| `CaretakerPhoneNo` | String | same; `EditCaretakerProfileActivity.java:137,198` | `CaretakerProfileActivity.java:91`; `EditCaretakerProfileActivity.java:88` |
| `CaretakerProfession` | String | same; `EditCaretakerProfileActivity.java:141,198` | `CaretakerProfileActivity.java:95`; `EditCaretakerProfileActivity.java:92` |
| `CaretakerExperience` | String | same; `EditCaretakerProfileActivity.java:138,198` | `CaretakerProfileActivity.java:96`; `EditCaretakerProfileActivity.java:89` |
| `CaretakerProfileUrl` | String | `RegisterActivity.java:327`; `EditCaretakerProfileActivity.java:189,198` | `CaretakerDrawer.java:186`; `CaretakerProfileActivity.java:97`; `EditCaretakerProfileActivity.java:108` |
| `CaretakerRace` | String | same | `CaretakerProfileActivity.java:94`; `EditCaretakerProfileActivity.java:90` |
| `CaretakerLanguage` | List&lt;String&gt; | `RegisterActivity.java:329`; `EditCaretakerProfileActivity.java:153,198` | `CaretakerProfileActivity.java:101-105`; `EditCaretakerProfileActivity.java:97-103` |
| `CaretakerGender` | String | same | `CaretakerProfileActivity.java:93`; `EditCaretakerProfileActivity.java:91` |
| `CaretakerAge` | String | same | `CaretakerProfileActivity.java:92`; `EditCaretakerProfileActivity.java:93` |

Whole-node read is also used as an existence / authorization check on login:
`LoginActivity.java:55` (`getReference("Caretakers")`) and `:120-136`.

### 3.2 `CaretakerManagement/{uid}` — no model class

Written outside this app (admin side); only read here.

| Field | Type | Read by |
|---|---|---|
| `CaretakerManagementStatus` | String | `LoginActivity.java:152-153` (switch: `"Suspended"`, `"Warning"`, default/`"Active"`) |
| `CaretakerManagementReason` | String | `LoginActivity.java:155-156` (shown in dialog) |

Reference: `LoginActivity.java:56` (`getReference("CaretakerManagement")`), read at `:138-180`.

### 3.3 `Advertisements/{caretakerUid}/{adId}` — model `Advertisement.java`

All fields are `String` except `SubCategoriesID` (`List<String>`). `adId` is a push key.

| Field | Type | Written by | Read by |
|---|---|---|---|
| `AdvertisementID` | String | `AdvertiseServiceActivity.java:277` (`:251` push, `:289` `setValue`) | `MyAdvertisementsActivity.java:75` → `getAdvertisementID()` used for edit/delete (`MyAdvertisementsActivity.java:108-124`) |
| `CaretakerID` | String | `AdvertiseServiceActivity.java:278` | `BookingAdapter.java:152,175` (uses `Booking.CaretakerID`, not this field) |
| `CategoriesID` | String | `AdvertiseServiceActivity.java:279`; `EditAdvertisementActivity.java:287,297` | `EditAdvertisementActivity.java:205`; `BookingAdapter.java:181`; `MyAdAdapter.java:70-76` |
| `SubCategoriesID` | List&lt;String&gt; | `AdvertiseServiceActivity.java:280`; `EditAdvertisementActivity.java:288` | `EditAdvertisementActivity.java:171-174`; `BookingAdapter.java:183-184` |
| `AdvertisementAvailability` | String | `AdvertiseServiceActivity.java:281`; `EditAdvertisementActivity.java:289` | `EditAdvertisementActivity.java:203`; `MyAdAdapter.java:52` |
| `AdvertisementMinRate` | String | `AdvertiseServiceActivity.java:282`; `EditAdvertisementActivity.java:290` | `EditAdvertisementActivity.java:192`; `BookingAdapter.java:157-159`; `MyAdAdapter.java:51` |
| `AdvertisementMaxRate` | String | `AdvertiseServiceActivity.java:283`; `EditAdvertisementActivity.java:291` | `EditAdvertisementActivity.java:193`; `BookingAdapter.java:158-159`; `MyAdAdapter.java:51` |
| `AdvertisementLocation` | String | `AdvertiseServiceActivity.java:284`; `EditAdvertisementActivity.java:292` | `EditAdvertisementActivity.java:194`; `MyAdAdapter.java:53` |
| `AdvertisementURL` | String | `AdvertiseServiceActivity.java:285`; `EditAdvertisementActivity.java:293` | `EditAdvertisementActivity.java:196-199`; `MyAdAdapter.java:60-64` |
| `AdvertisementDate` | String | `AdvertiseServiceActivity.java:286`; `EditAdvertisementActivity.java:294` | `MyAdAdapter.java:56` |
| `AdvertisementTime` | String | `AdvertiseServiceActivity.java:287`; `EditAdvertisementActivity.java:295` | `MyAdAdapter.java:57` |

Other access:
- Whole-list read: `MyAdvertisementsActivity.java:60,70-76`.
- Whole-object read: `EditAdvertisementActivity.java:77-80,181-183` (`getValue(Advertisement.class)`).
- Count only: `CaretakerHomeActivity.java:170-175`.
- Delete: `MyAdvertisementsActivity.java:124` (`removeValue`).
- SubCategories/Categories lookups: `BookingAdapter.java:175-205`.
- Rate display: `BookingAdapter.java:152-165`.

### 3.4 `Categories/{categoryId}` — no model class

Read-only in this app; assumed to be maintained by an admin/family app.

| Field | Type | Read by |
|---|---|---|
| `CategoryName` | String | `AdvertiseServiceActivity.java:151`; `AdvertiseServicePreviewActivity.java:135`; `EditAdvertisementActivity.java:117`; `MyAdAdapter.java:74`; `BookingAdapter.java:192` |
| `SubCategories/{subCategoryId}/SubCategoryName` | String | `AdvertiseServiceActivity.java:158-160`; `AdvertiseServicePreviewActivity.java:142-147`; `EditAdvertisementActivity.java:125-127`; `BookingAdapter.java:195-199` |

References: `AdvertiseServiceActivity.java:104`, `AdvertiseServicePreviewActivity.java:123`,
`EditAdvertisementActivity.java:111`, `MyAdAdapter.java:72`, `BookingAdapter.java:188`.

### 3.5 `Submissions/{pushId}` — model `Submission.java`

All fields `String`. `pushId` is a push key and equals `SubmissionID`.

| Field | Type | Written by | Read by |
|---|---|---|---|
| `SubmissionID` | String | `SubmissionActivity.java:148` (`:136` push, `:157` `setValue`) | `SubmissionHistoryActivity.java:68-73` (falls back to key) |
| `CaretakerID` | String | `SubmissionActivity.java:149` | Query at `SubmissionActivity.java:176`; `SubmissionHistoryActivity.java:61`; `CaretakerDrawer.java:215` |
| `SubmissionURL` | String | `SubmissionActivity.java:150` | `SubmissionAdapter.java:79-80` |
| `SubmissionStatus` | String | `SubmissionActivity.java:151` (`"Pending"`) | `SubmissionActivity.java:216`; `CaretakerDrawer.java:231,244-248`; `SubmissionAdapter.java:50-63` |
| `SubmissionDate` | String | `SubmissionActivity.java:152` (`yyyy-MM-dd`) | `SubmissionActivity.java:191`; `CaretakerDrawer.java:229`; `SubmissionHistoryActivity.java:82-84`; `SubmissionAdapter.java:45` |
| `SubmissionTime` | String | `SubmissionActivity.java:153` (`HH:mm`) | `SubmissionActivity.java:192`; `CaretakerDrawer.java:230`; `SubmissionHistoryActivity.java:82-84`; `SubmissionAdapter.java:46` |
| `SubmissionRemark` | String | `SubmissionActivity.java:154` (`""` initially) | `SubmissionActivity.java:217`; `SubmissionAdapter.java:47` |
| `AdminID` | String | `SubmissionActivity.java:155` (`""` initially) | `SubmissionAdapter.java:67-74` → `Admins/{AdminID}/AdminName` |

### 3.6 `Admins/{adminId}` — no model class

Read-only. `AdminName : String` read at `SubmissionAdapter.java:70-74`.

### 3.7 `Bookings/{bookingId}` — model `Booking.java`

The app never creates a booking; it only reads bookings and updates `BookingStatus`.
All fields `String` except `BookingTimeStamp` (`long`).

| Field | Type | Written by | Read by |
|---|---|---|---|
| `BookingID` | String | — | Used as node key elsewhere: `BookingAdapter.java:268,299,329`; `CaretakerBookingListActivity.java:114-115` loads by key; `ReportBookingActivity.java:93,109` |
| `CaretakerID` | String | — | `CaretakerHomeActivity.java:96` (`orderByChild`), `:111` model; `BookingAdapter.java:74,143,152,175` |
| `FamiliyElderlyID` | String | — | `BookingAdapter.java:73,92-94,99,104,247` |
| `AdvertismentID` | String | — | `BookingAdapter.java:151,152,169,175` |
| `BookingDate` | String | — | `BookingAdapter.java:63-65`; `CaretakerHomeActivity.java:115,125-141` |
| `BookingTime` | String | — | `BookingAdapter.java:64-65`; `CaretakerHomeActivity.java:126-141` |
| `BookingDuration` | String | — | `BookingAdapter.java:68` |
| `BookingNote` | String | — | `BookingAdapter.java:70` |
| `BookingStatus` | String | `BookingAdapter.java:328-330` (`"Accepted"`, `"Rejected"`); `ScanQRCodeActivity.java:139-144` (`"Completed"`) | `BookingAdapter.java:81-127,216-229`; `CaretakerHomeActivity.java:118,124`; `CaretakerBookingListActivity.java:152-165` |
| `BookingTimeStamp` | long | — | `CaretakerBookingListActivity.java:122,188` (sort) |

Other access: direct query `Bookings.orderByChild("CaretakerID").equalTo(uid)`
(`CaretakerHomeActivity.java:96`), per-ID fetch (`CaretakerBookingListActivity.java:92,115`),
single-field update (`BookingAdapter.java:329-330`, `ScanQRCodeActivity.java:141-144`).

### 3.8 `CaretakerBookings/{caretakerUid}/{bookingId}` — index node

No fields are read. `CaretakerBookingListActivity.java:90-104` reads the child keys as a
list of booking IDs, then fetches each `Bookings/{bookingId}`
(`CaretakerBookingListActivity.java:114-115`). Nothing in this app writes this node.

### 3.9 `Reviews/{bookingId}` — no model class

Read-only in this app; one review per booking key (no push nesting used).

| Field | Type | Read by |
|---|---|---|
| `CaretakerID` | String | `CaretakerHomeActivity.java:182` (`orderByChild("CaretakerID").equalTo(uid)`) |
| `ReviewRating` | number | `CaretakerHomeActivity.java:190-198` (`getValue()` → `Float.parseFloat`); `BookingAdapter.java:286-289` (`getValue(Float.class)`) |
| `ReviewDetails` | String | `BookingAdapter.java:287-290` |

Existence checks: `BookingAdapter.java:268-275,299-306`; `CaretakerBookingListActivity.java:175-181`.

### 3.10 `FamilyElderly/{uid}` — no model class

Read-only in this app.

| Field | Type | Read by |
|---|---|---|
| `FamilyElderlyName` | String | `BookingAdapter.java:134-138,252`; `CaretakerMessagesActivity.java:131-132` (fallback `username` at `:133`) |
| `FamilyElderlyAge` | number/String | `BookingAdapter.java:253` (untyped `getValue()`) |
| `FamilyElderlyGender` | String | `BookingAdapter.java:254` |
| `FamilyElderlyPhone` | String | `BookingAdapter.java:255` |
| `FamilyElderlyType` | String | `BookingAdapter.java:256` |
| `FamilyElderlyURL` | String | `BookingAdapter.java:257-258`; `CaretakerMessagesActivity.java:136-137` (fallback `imageUrl` at `:138`) |
| `username` | String | `CaretakerMessagesActivity.java:133` (legacy fallback) |
| `imageUrl` | String | `CaretakerMessagesActivity.java:138` (legacy fallback) |

### 3.11 `ChatThreads/{chatId}` — model `ChatThread.java`

`chatId = (uidA < uidB ? uidA + "_" + uidB : uidB + "_" + uidA)` — deterministic,
lexicographically sorted pair of Firebase UIDs (`ChatActivity.java:94-97`,
`ChatFragment.java:93-96`).

| Field | Type | Written by | Read by |
|---|---|---|---|
| `chatId` | String | `ChatActivity.java:133-141` (thread create); `ChatFragment.java:127-134` | `CaretakerMessagesActivity.java:119-123` (`getValue(ChatThread.class)`, used as `thread.chatId`) |
| `caretakerId` | String | `ChatActivity.java:135` (= current caretaker uid); `ChatFragment.java:129` (its `caretakerUid` argument) | `CaretakerMessagesActivity.java:112` (`orderByChild("caretakerId").equalTo(currentUid)`) |
| `familyElderlyId` | String | `ChatActivity.java:136` (= receiver uid); `ChatFragment.java:130` (= current uid) | `CaretakerMessagesActivity.java:122,126` (→ `FamilyElderly/{id}`) |
| `lastMessage` | String | `ChatActivity.java:328-332`; `ChatFragment.java:296-301` | `CaretakerMessagesActivity.java:124` |
| `lastTimestamp` | long | `ChatActivity.java:326-332`; `ChatFragment.java:298-301` | — (never read in this app) |

Thread creation: `ChatActivity.java:122-147`; `ChatFragment.java:117-140`.
Reply updates: `ChatActivity.java:325-332`; `ChatFragment.java:295-301`.

### 3.12 `Messages/{chatId}/{messageId}` — model `MessageModel.java`

`messageId` is a push key. Text/image/voice messages share the node; unused URL fields are
passed as `null` (`ChatActivity.java:307-321`, `ChatFragment.java:280-290`).

| Field | Type | Written by | Read by |
|---|---|---|---|
| `messageId` | String | `ChatActivity.java:312`; `ChatFragment.java:281` | — (never read; duplicates key) |
| `chatId` | String | `ChatActivity.java:313`; `ChatFragment.java:282` | — (never read; duplicates key) |
| `senderId` | String | `ChatActivity.java:314`; `ChatFragment.java:283` | `ChatActivity.java:372` (adapter `isMe`) |
| `receiverId` | String | `ChatActivity.java:315`; `ChatFragment.java:284` | — (never read) |
| `messageType` | String | `ChatActivity.java:316`; `ChatFragment.java:285` | `ChatActivity.java:387,399,411` |
| `messageText` | String/null | `ChatActivity.java:317`; `ChatFragment.java:286` | `ChatActivity.java:390` |
| `messageImageURL` | String/null | `ChatActivity.java:318`; `ChatFragment.java:287` | `ChatActivity.java:406` |
| `messageAudioURL` | String/null | `ChatActivity.java:319`; `ChatFragment.java:288` | `ChatActivity.java:421` |
| `messageTimeStamp` | long | `ChatActivity.java:320`; `ChatFragment.java:289` | `ChatActivity.java:164,390`; `ChatFragment.java:152` |

Writes: `ChatActivity.java:100,302-323`; `ChatFragment.java:99,275-292`.
Reads: `ChatActivity.java:153-168`; `ChatFragment.java:144-156`.

### 3.13 `Reports/{bookingId}/{reportId}` — model `Report.java`

`reportId` is a push key under the booking key. All fields `String` except `ReportTime` (`long`).

| Field | Type | Written by | Read by |
|---|---|---|---|
| `ReportID` | String | `ReportBookingActivity.java:95-107` (`:93` push, `:109-110` `setValue`) | — (never read; duplicates key) |
| `BookingID` | String | `ReportBookingActivity.java:97` | `CaretakerReportAdapter.java:39` |
| `ReportCategory` | String | `ReportBookingActivity.java:98` | `CaretakerReportAdapter.java:40` |
| `ReportDetails` | String | `ReportBookingActivity.java:99` | `CaretakerReportAdapter.java:41` |
| `ReportStatus` | String | `ReportBookingActivity.java:100` (`"Pending"`) | `CaretakerReportAdapter.java:42,50-55` (expects `"Resolved"`/`"InReview"` from admin) |
| `ReportTime` | long | `ReportBookingActivity.java:91,101` (epoch millis) | `CaretakerReportAdapter.java:44-47` |
| `CaretakerID` | String | `ReportBookingActivity.java:102` (= current uid) | — (never read directly) |
| `FamiliyElderlyID` | String | `ReportBookingActivity.java:103` (intent `familyId`) | — (never read directly) |
| `AdminID` | null/String | `ReportBookingActivity.java:104` (passed `null`) | — (never read) |
| `ReporterID` | String | `ReportBookingActivity.java:105` (= current uid) | `MyReportsActivity.java:80,84` (client-side filter) |
| `ReporterRole` | String | `ReportBookingActivity.java:106` (`"Caretaker"`) | `MyReportsActivity.java:81,84-86` |

Read pattern: `MyReportsActivity.java:61,69-89` reads the entire `Reports` tree,
iterates every booking child and every report child, and filters in memory.

## 4. Relationships

```
FirebaseAuth uid
   ├── Caretakers/{uid}                     (profile; login gate)
   ├── CaretakerManagement/{uid}            (admin status gate; read-only here)
   ├── Advertisements/{uid}/{adId}          (ad owner)
   ├── Submissions.CaretakerID
   ├── Reports.CaretakerID / Reports.ReporterID
   ├── ChatThreads.caretakerId
   └── Messages.senderId

Bookings/{bookingId}
   ├── .CaretakerID      -> Caretakers/{uid}
   ├── .FamiliyElderlyID -> FamilyElderly/{uid}
   ├── .AdvertismentID   -> Advertisements/{CaretakerID}/{adId}
   ├── CaretakerBookings/{CaretakerID}/{bookingId}   (index)
   ├── Reviews/{bookingId}
   └── Reports/{bookingId}/{reportId}

Advertisements/{caretakerUid}/{adId}
   ├── .CaretakerID    -> Caretakers/{uid}
   ├── .CategoriesID   -> Categories/{categoryId}
   └── .SubCategoriesID[] -> Categories/{CategoriesID}/SubCategories/{subCategoryId}

Chats
   chatId = min(uidA, uidB) + "_" + max(uidA, uidB)
   ├── ChatThreads/{chatId}
   │      ├── .caretakerId     -> Caretakers/{uid}
   │      └── .familyElderlyId -> FamilyElderly/{uid}
   └── Messages/{chatId}/{messageId}
          ├── .senderId   -> FirebaseAuth uid
          └── .receiverId -> FirebaseAuth uid

Submissions/{submissionId}
   ├── .CaretakerID -> Caretakers/{uid}
   └── .AdminID     -> Admins/{adminId}
```

### Auth linkage details

- Every node written by this app is keyed or stamped with
  `FirebaseAuth.getInstance().getCurrentUser().getUid()` (or `mAuth.getUid()`):
  `Caretakers/{uid}` (`RegisterActivity.java:310,334`), `Advertisements/{uid}/...`
  (`AdvertiseServiceActivity.java:250-251`), `Submissions.CaretakerID`
  (`SubmissionActivity.java:116,149`), `Reports.ReporterID` (`ReportBookingActivity.java:43,105`),
  `ChatThreads.caretakerId` / `Messages.senderId` (`ChatActivity.java:92,125,299`,
  `ChatFragment.java:88,120,272`).
- Login flow is gated on database contents: the Auth account must have a
  `Caretakers/{uid}` node (`LoginActivity.java:120-136`), then
  `CaretakerManagement/{uid}` may suspend or warn the account (`:138-180`).
- There is no `Users` node in this app; the family app's data is reached through
  `FamilyElderly`, `Bookings` and `Reviews`, which this app only reads.

## 5. Inconsistencies and issues

1. **Misspelled shared fields.** `Bookings` uses `FamiliyElderlyID` and `AdvertismentID`
   (`Booking.java:7-8`), while `Report` also uses `FamiliyElderlyID` (`Report.java:13`) but
   `Messages` uses `receiverId` and the actual ad field is `AdvertisementID`
   (`Advertisement.java:7`). These spellings must match the family app exactly; any rename
   would break joins at `BookingAdapter.java:152,175,247`.
2. **Two different booking-access paths.** `CaretakerHomeActivity.java:96` queries
   `Bookings` by `CaretakerID`, while `CaretakerBookingListActivity.java:90-115` reads the
   `CaretakerBookings/{caretakerUid}` index first. No code in this app writes
   `CaretakerBookings`, so the list screen depends on the family/admin app maintaining that
   index; the dashboard does not. The two screens can disagree if the index is stale.
3. **Read-only nodes with no producer in this repo.** `Categories`, `Admins`,
   `FamilyElderly`, `Reviews`, `CaretakerBookings`, `CaretakerManagement`, and the creation
   of `Bookings` are never written by this app. Their schemas are assumptions imported from
   the family/admin app, so they cannot be verified from this codebase alone.
4. **Same field written with different empty values.** `ChatThreads/{chatId}/lastMessage` is
   created as `null` by `ChatActivity.java:136` but as `""` by `ChatFragment.java:132` for
   the same node.
5. **Redundant key-duplicating fields never read.** `Caretaker.CaretakerID`,
   `ChatThread.chatId`, `MessageModel.messageId`, `MessageModel.chatId`,
   `MessageModel.receiverId`, `Report.ReportID`, `Report.CaretakerID`,
   `Report.FamiliyElderlyID` and `Report.AdminID` are written but have no read site in this
   app. They duplicate the key or carry unreferenced data.
6. **`AdvertisementDate`/`AdvertisementTime` change meaning on edit.**
   `AdvertiseServiceActivity.java:254-255` writes creation date/time, but
   `EditAdvertisementActivity.java:283-295` overwrites both with the edit time.
   `MyAdAdapter.java:56-57` labels them "Date"/"Time" without saying creation vs edit.
7. **Report status vocabulary mismatch.** The app writes only `"Pending"`
   (`ReportBookingActivity.java:100`), while the adapter styles `"Resolved"` and
   `"InReview"` (`CaretakerReportAdapter.java:50-55`); nothing in this app writes those
   values. Similarly, `Reviews` is read with `ReviewRating` as both untyped
   (`CaretakerHomeActivity.java:190`) and `Float` (`BookingAdapter.java:286`).
8. **`FamilyElderly` has two possible field sets.** `CaretakerMessagesActivity.java:131-138`
   falls back from `FamilyElderlyName`/`FamilyElderlyURL` to `username`/`imageUrl`, while
   `BookingAdapter.java:252-258` reads only the `FamilyElderly*` names. Data written as the
   legacy pair will show blank elsewhere.
9. **`Reports` is scanned in full.** `MyReportsActivity.java:69-89` downloads the whole
   `Reports` node (all bookings, all reports) and filters client-side; there is no query by
   reporter. This is an efficiency/scalability issue and relies on unrestricted read access.
10. **Submission "latest" fallback is not chronological.**
    `SubmissionActivity.java:194` uses `child.getKey().hashCode()` when date/time are
    missing; push-key hash codes are not time-ordered. `CaretakerDrawer.java:233-245` instead
    leaves the timestamp at `0`, so approval checks can compare ties incorrectly.
11. **`AdminID` uses two empty representations.** `Submissions.AdminID` is written as `""`
    (`SubmissionActivity.java:155`) while `Reports.AdminID` is passed as `null`
    (`ReportBookingActivity.java:104`); `SubmissionAdapter.java:67` treats empty string as
    pending, but there is no equivalent report-side handling.
12. **Storage path style inconsistency (same result).**
    `RegisterActivity.java:312` uses `caretaker_profiles/{uid}/profile.jpg`, while
    `EditCaretakerProfileActivity.java:185` uses `storageRef.child(uid + "/profile.jpg")`
    from the same root. Both produce the same object path.
13. **Hardcoded path and status strings.** Node names (`"Caretakers"`, `"Bookings"`,
    `"Submissions"`, ...) and status values (`"Pending"`, `"Approved"`, `"Rejected"`,
    `"Accepted"`, `"Completed"`, `"Suspended"`, `"Warning"`, `"Active"`, `"Caretaker"`) are
    string literals scattered across activities and adapters; there are no constants or
    schema/validation layer.
14. **Shared database project.** `app/google-services.json` registers both
    `com.example.elderhero` and `com.example.elderhero_caretakers` under project
    `demo-elderhero`, and no database rules file is checked in, so schema and authorization
    live outside this repository.
15. **Model naming convention differs by domain.** DB fields are PascalCase for
    caretakers/ads/bookings/submissions/reports, but camelCase for chats
    (`chatId`, `caretakerId`, `familyElderlyId`, `lastMessage`, `lastTimestamp` and all
    `MessageModel` fields). Any query/index must use the exact case:
    `CaretakerMessagesActivity.java:112` queries `caretakerId`.
16. **`ChatFragment` can create a thread with caretaker/family roles reversed.**
    The fragment is only used by the caretaker app (tablet split view), and
    `CaretakerMessagesActivity.java:67-70` passes the selected **family** UID as the
    fragment's `caretakerUid` argument. `ChatFragment.java:120-130` then writes
    `caretakerId = caretakerUid` (the family UID) and `familyElderlyId = currentUid`
    (the caretaker UID). If the fragment creates `ChatThreads/{chatId}` before
    `ChatActivity` does, the thread is indexed under the wrong UID and disappears from
    `CaretakerMessagesActivity.java:112`'s `orderByChild("caretakerId").equalTo(currentUid)`
    query. `Messages` sender/receiver values in `ChatFragment.java:271-273` are still
    caretaker/family respectively, so only the thread index is affected. When
    `ChatActivity` creates the thread first, the deterministic `chatId` means the
    fragment finds it and does not overwrite the fields.

## 6. Firebase Storage references (non-database, listed for completeness)

| Storage path | Written by | Referenced from DB as |
|---|---|---|
| `caretaker_profiles/{uid}/profile.jpg` | `RegisterActivity.java:82,312`; `EditCaretakerProfileActivity.java:77,185-189` | `Caretakers/{uid}/CaretakerProfileUrl` |
| `submissions/{uid}/{uid}_{timestamp}.pdf` | `SubmissionActivity.java:63,117-119` | `Submissions/{id}/SubmissionURL` |
| `advertisements/{uid}/{adId}.jpg` | `AdvertiseServiceActivity.java:103,258`; `EditAdvertisementActivity.java:82,264` | `Advertisements/{uid}/{adId}/AdvertisementURL` |
| `ChatFiles/images/{timestamp}.jpg` | `ChatActivity.java:101,195`; `ChatFragment.java:100,258` | `Messages/{chatId}/{id}/messageImageURL` |
| `ChatFiles/audio/voice_{timestamp}.m4a` | `ChatActivity.java:101,283`; `ChatFragment.java:100,246` | `Messages/{chatId}/{id}/messageAudioURL` |

The DB stores only the download URL string returned by `getDownloadUrl()`; there are no
storage paths stored in the database.

## 7. Appendix — every `getReference(...)` site (47 total)

Database references (40):

| File:line | Path |
|---|---|
| `authenticate/RegisterActivity.java:81` | `Caretakers` |
| `authenticate/LoginActivity.java:55` | `Caretakers` |
| `authenticate/LoginActivity.java:56` | `CaretakerManagement` |
| `CaretakerHomeActivity.java:73` | `Caretakers` (then `.child(uid)`) |
| `CaretakerHomeActivity.java:93` | root (`getReference()`) |
| `CaretakerDrawer.java:180` | `Caretakers` (then `.child(uid)`) |
| `CaretakerDrawer.java:213` | `Submissions` |
| `CaretakerProfileActivity.java:69` | `Caretakers` (then `.child(uid)`) |
| `EditCaretakerProfileActivity.java:75` | `Caretakers` (then `.child(uid)`) |
| `MyReportsActivity.java:61` | `Reports` |
| `ReportBookingActivity.java:71` | `Reports` |
| `SubmissionActivity.java:64` | root (`getReference()`), then `Submissions` at `:135,176` |
| `SubmissionHistoryActivity.java:53` | `Submissions` |
| `SubmissionAdapter.java:26` | root (`getReference()`), then `Admins` at `:70` |
| `ScanQRCodeActivity.java:139` | root (`getReference()`), then `Bookings` at `:141` |
| `advertisement/AdvertiseServiceActivity.java:102` | `Advertisements` |
| `advertisement/AdvertiseServiceActivity.java:104` | `Categories` |
| `advertisement/AdvertiseServicePreviewActivity.java:123` | `Categories` |
| `advertisement/EditAdvertisementActivity.java:78` | `Advertisements/{uid}/{adId}` |
| `advertisement/EditAdvertisementActivity.java:111` | `Categories` |
| `advertisement/MyAdAdapter.java:72` | `Categories` |
| `advertisement/MyAdvertisementsActivity.java:60` | `Advertisements/{uid}` |
| `booking/BookingAdapter.java:134` | `FamilyElderly/{uid}` |
| `booking/BookingAdapter.java:143` | `Caretakers/{uid}` |
| `booking/BookingAdapter.java:152` | `Advertisements/{caretakerId}/{adId}` |
| `booking/BookingAdapter.java:175` | `Advertisements/{caretakerId}/{adId}` |
| `booking/BookingAdapter.java:188` | `Categories/{categoryId}` |
| `booking/BookingAdapter.java:247` | `FamilyElderly/{uid}` |
| `booking/BookingAdapter.java:268` | `Reviews/{bookingId}` |
| `booking/BookingAdapter.java:299` | `Reviews/{bookingId}` |
| `booking/BookingAdapter.java:329` | `Bookings/{bookingId}` |
| `booking/CaretakerBookingListActivity.java:91` | `CaretakerBookings/{caretakerId}` |
| `booking/CaretakerBookingListActivity.java:92` | `Bookings` |
| `booking/CaretakerBookingListActivity.java:175` | `Reviews` |
| `chats/CaretakerMessagesActivity.java:48` | `ChatThreads` |
| `chats/CaretakerMessagesActivity.java:49` | `FamilyElderly` |
| `chats/ChatActivity.java:99` | `ChatThreads/{chatId}` |
| `chats/ChatActivity.java:100` | `Messages/{chatId}` |
| `chats/ChatFragment.java:98` | `ChatThreads/{chatId}` |
| `chats/ChatFragment.java:99` | `Messages/{chatId}` |

Storage references (7, `FirebaseStorage.getInstance().getReference(...)`):

| File:line | Path |
|---|---|
| `authenticate/RegisterActivity.java:82` | `caretaker_profiles` |
| `EditCaretakerProfileActivity.java:77` | `caretaker_profiles` |
| `SubmissionActivity.java:63` | `submissions` |
| `advertisement/AdvertiseServiceActivity.java:103` | `advertisements` |
| `advertisement/EditAdvertisementActivity.java:82` | `advertisements` |
| `chats/ChatActivity.java:101` | `ChatFiles` |
| `chats/ChatFragment.java:100` | `ChatFiles` |

All 47 are accounted for above. `CaretakerHomePreviewActivity.java`, `SplashActivity.java`
and `ForgotPasswordActivity.java` contain no Realtime Database access;
`ForgotPasswordActivity` uses Firebase Auth only.
