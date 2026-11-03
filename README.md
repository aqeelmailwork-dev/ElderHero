# ElderHero

ElderHero connects families looking after elderly relatives with professional caretakers.
It is made of three apps that share one Firebase backend:

| Folder | What it is | Built with |
|---|---|---|
| [`caretaker-app/`](caretaker-app) | Android app for caretakers: profile, service ads, bookings, chat, document submissions | Java, Firebase Auth / Realtime Database / Storage |
| [`family-app/`](family-app) | Android app for families: browse caretakers, book services, chat, reviews, reports | Java, Firebase Auth / Realtime Database / Storage |
| [`admin-web/`](admin-web) | Admin dashboard: approve caretakers, manage users, categories, bookings, reports | HTML + JavaScript, Firebase JS SDK 9 |
| [`server/`](server) | Self-hosted Firebase Local Emulator Suite in Docker | Docker, firebase-tools |

The apps run against the **Firebase Local Emulator Suite** on your own machine or home server,
so you do not need a Firebase account. The example configs use the project ID
`demo-elderhero`; Firebase treats `demo-` projects as emulator-only.

```
 Android phone                       Server (Docker, host network)
 ┌──────────────────┐               ┌──────────────────────────────┐
 │ caretaker-app    │──┐            │ Firebase Local Emulator Suite │
 │ family-app       │──┼── HTTP ───▶│  Auth           :9099         │
 └──────────────────┘  │            │  Realtime DB    :9010         │
 Browser               │            │  Storage        :9199         │
 ┌──────────────────┐  │            │  Emulator UI    :4000         │
 │ admin-web        │──┘            └──────────────────────────────┘
 └──────────────────┘
```

The data model (every node and field) is documented in [`docs/DB_STRUCTURE.md`](docs/DB_STRUCTURE.md).

---

## Requirements

- **Server:** any Linux machine with Docker and Docker Compose (a home server, or your own PC).
  The emulator needs about 1–2 GB of RAM.
- **Android apps:** Android Studio, or the Android SDK command-line tools, with **JDK 17 or 21**.
  JDK 22+ is too new for the Android Gradle Plugin used here.
- **Admin site:** a browser and any static web server (for example VS Code Live Server,
  or `python3 -m http.server`).
- All devices on the same network as the server (or see [Access from outside your network](#access-from-outside-your-network)).

---

## 1. Start the Firebase emulator

On the machine that will run the backend:

```bash
git clone https://github.com/aqeelmailwork-dev/ElderHero.git
cd ElderHero/server
```

Find the server's LAN IP address (for example with `hostname -I`) and put it in
`firebase.json` as the database host:

```json
"database": { "host": "192.168.1.100", "port": 9010 },
```

> **This must be the server's real IP address, not `0.0.0.0`.** The Realtime Database
> emulator tells clients to reconnect to the host it is bound to. With `0.0.0.0`,
> phones reconnect to themselves and database writes silently stop after the
> connection goes idle (about 60 seconds).

Then build and start it:

```bash
docker compose up -d --build
docker compose logs -f        # wait for "All emulators ready", then Ctrl+C
```

Check it from another device: open `http://<server-ip>:4000` for the Emulator UI.

**Ports used:** 4000 (UI), 4400 (hub), 4500 (logs), 9010 (database), 9099 (auth), 9199 (storage).
The container uses host networking, so your firewall applies to it. With `ufw`, allow your
local network only:

```bash
sudo ufw allow from 192.168.1.0/24 to any port 4000,4400,4500,9010,9099,9199 proto tcp
```

Data is saved to `server/data/` when the container stops cleanly. To also back it up while
running, add a cron job:

```bash
0 3 * * * docker exec firebase-emulator firebase emulators:export /srv/data --project demo-elderhero --force
```

---

## 2. Run the Android apps

Do this for both `caretaker-app/` and `family-app/`.

1. **Firebase config.** Copy the example file:

   ```bash
   cp app/google-services.json.example app/google-services.json
   ```

2. **Emulator address.** Create `local.properties` in the app folder (it is not committed):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   emulator.host=192.168.1.100
   use.emulator=true
   ```

   `emulator.host` defaults to `10.0.2.2`, which reaches your computer from the Android
   Studio emulator.

3. **Build and install** on a phone connected with USB debugging:

   ```bash
   ./gradlew installDebug
   ```

   Or open the folder in Android Studio and press Run.

Debug builds are allowed to use plain HTTP (needed for the emulator). Release builds only
allow HTTPS.

---

## 3. Run the admin site

1. **Firebase config.** Copy the example and set your server's IP:

   ```bash
   cd admin-web
   cp API/firebase-config.example.js API/firebase-config.js
   ```

   ```js
   export const EMULATOR_HOST = "192.168.1.100";
   ```

2. **Serve the folder over HTTP**, then open `login.html`:

   ```bash
   python3 -m http.server 5500
   # open http://localhost:5500/login.html
   ```

   Opening the files directly (`file://`) does not work, and an `https://` page cannot talk to
   the emulator's plain-HTTP ports.

3. **Create the first admin.** The admin site only lets in users that also exist under
   `Admins/<uid>` in the database. Create both with the emulator's REST API (replace the IP,
   email and password):

   ```bash
   HOST=192.168.1.100
   UID_=$(curl -s -X POST -H 'Authorization: Bearer owner' -H 'Content-Type: application/json' \
     "http://$HOST:9099/identitytoolkit.googleapis.com/v1/projects/demo-elderhero/accounts" \
     -d '{"email":"admin@example.com","password":"change-me-123"}' | sed -E 's/.*"localId":"([^"]+)".*/\1/')

   curl -s -X PUT "http://$HOST:9010/Admins/$UID_.json?ns=demo-elderhero-default-rtdb" \
     -d '{"AdminEmail":"admin@example.com","AdminName":"Super Admin","AdminType":"superadmin"}'
   ```

   You can also do this by hand in the Emulator UI (Authentication → Add user, then
   Realtime Database → add `Admins/<uid>`). `admin-web/JSON/Admins.json` shows the expected shape.

---

## First run checklist

1. Log in to the admin site and add some **Categories**. Caretakers pick one when they
   advertise a service.
2. Register a caretaker in `caretaker-app`, submit documents, and approve them in the admin site.
3. Register a family in `family-app`, browse services and make a booking.

---

## Access from outside your network

The apps connect to one fixed address, the server's LAN IP. To use them away from home,
make that address reachable over [Tailscale](https://tailscale.com) instead of changing the apps:

1. Install Tailscale on the server and the phone, logged in to the same account.
2. On the server, share only its own LAN address:

   ```bash
   sudo tailscale set --advertise-routes=192.168.1.100/32
   ```

3. Approve the route in the Tailscale admin console (Machines → your server → Edit route settings).

The phone then reaches `192.168.1.100` through Tailscale wherever it is. Do not forward the
emulator ports on your router.

---

## Using a real Firebase project instead

1. Create a Firebase project with Authentication (email/password), Realtime Database and Storage.
2. Download each Android app's `google-services.json` into `app/` and set `use.emulator=false`
   in `local.properties`.
3. Put your web config in `admin-web/API/firebase-config.js` and set `USE_EMULATOR = false`.
4. Write proper security rules before letting anyone else use it.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Registration spins forever, or login does nothing | The app cannot reach the database. Check `emulator.host`, that the phone is on the same network, and the firewall. Debug logs show `PersistentConnection` lines in `adb logcat`. |
| Writes work for a minute, then stop | The database emulator is bound to `0.0.0.0`. Set the server's real IP in `server/firebase.json`. |
| `has been deactivated` in logcat | The app reached a real Firebase database, not the emulator. Make sure you did not add `FirebaseDatabase.useEmulator()`: in firebase-database 22.x it makes later `getInstance()` calls create a production instance. The apps set the emulator URL when Firebase starts instead (see `ElderHeroApp.java`). |
| `CLEARTEXT communication ... not permitted` | You are running a release build against the emulator. Use a debug build. |
| Admin login says the user is not an admin | Add the user under `Admins/<uid>` (step 3 above). |
| Gradle fails in `JdkImageTransform` / `jlink` | Your JDK is too new. Point Gradle at JDK 17 or 21 (`org.gradle.java.home` in `~/.gradle/gradle.properties`). |

---

## Security notes

This setup is for development and demos, not real users:

- The database and storage rules in `server/` allow anyone on the network to read and write everything.
- The Auth emulator stores passwords in plain text, visible in the Emulator UI. Use test passwords only.
- Never commit `google-services.json`, `local.properties` or `admin-web/API/firebase-config.js`;
  they are listed in `.gitignore`.
