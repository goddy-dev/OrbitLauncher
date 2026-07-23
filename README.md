# Nyumbani Launcher

A custom Android home-screen launcher, built from scratch with functionality inspired by
launchers like Nova Launcher — but original code, name, and design so it's yours to modify freely.

## Features included

- **Home screen app grid** — pinned apps shown on the home screen
- **App drawer** — full searchable list of every installed app
- **Folders** — group apps together, tap to expand
- **Gestures** — swipe up / swipe down are configurable (open drawer, notifications, search, or nothing)
- **Customization** — grid columns (3–8) and rows (3–10), accent color, background color, toggle icon labels
- **Backup & restore** — exports your whole setup (layout + settings) as JSON to `Downloads/nyumbani_launcher_backup.json`, and can restore it back
- **Drag to reorder** — long-press and drag icons on the home screen
- Registers itself as a real Android **HOME app** so you can set it as your default launcher

## Requirements

- A GitHub account (no Android Studio needed — GitHub Actions builds the APK for you)
- An Android phone running **Android 8.0 (API 26)** or higher, to install and test the app

## Build it via GitHub (no Android Studio needed)

This project includes a GitHub Actions workflow (`.github/workflows/build.yml`) that
compiles the app into an installable `.apk` automatically every time you push.

1. Create a new **public** repo on GitHub (private repos also work but have limited free
   Actions minutes).
2. Push this project to it:
   ```
   cd NyumbaniLauncher
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git push -u origin main
   ```
3. On GitHub, open your repo → the **Actions** tab. You'll see a "Build APK" run in progress
   (starts automatically on push, or click "Run workflow" to trigger it manually).
4. When it finishes (a few minutes the first time), open the run → scroll to **Artifacts** →
   download **nyumbani-launcher-debug-apk** (a zip containing the `.apk`).
5. Transfer the `.apk` to your Android phone (email it to yourself, use Google Drive, USB, etc.).
6. On the phone, tap the `.apk` file to install it. You'll need to allow "install from
   unknown sources" the first time — Android will prompt you for this.
7. Press the Home button → choose **Nyumbani Launcher** → optionally tap "Always" to set it
   as your default launcher.

## Optional: build locally later

If you ever do install [Android Studio](https://developer.android.com/studio), you can also
open this folder directly (File → Open) and click Run — no changes needed.

## Project structure

```
app/src/main/java/com/godwin/nyumbanilauncher/
├── model/          # AppInfo, GridItem — the data classes
├── util/           # AppRepository (PackageManager), PrefsManager (settings),
│                    # BackupManager (JSON export/import), GestureHelper (swipe detection)
├── adapter/         # RecyclerView adapters for the home grid and app drawer
└── ui/              # MainActivity (home screen), AppDrawerActivity, SettingsActivity
```

## Where to make changes

- **New gesture actions** → `PrefsManager.kt` (add an `ACTION_*` constant) + `MainActivity.handleAction()`
- **Multi-page home screen** → swap the single `RecyclerView` grid in `activity_main.xml` for a `ViewPager2`
  of grids, one page per `List<GridItem>`
- **Icon packs** → extend `HomeGridAdapter.loadIcon()` to look up icons from a chosen icon-pack package
  instead of the app's own icon
- **Widgets on home screen** → would need `AppWidgetHost` / `AppWidgetManager` integration — a bigger addition,
  happy to help scaffold it when you're ready
- **Full color picker** instead of the swatch palette → replace `setupColorSwatches()` in
  `SettingsActivity.kt` with an HSV picker dialog

## Notes

- Some OEM skins (Samsung, Xiaomi, etc.) restrict third-party launchers from expanding the
  notification shade via `expandNotificationsPanel` — that gesture action degrades gracefully
  with a toast if it's blocked.
- Backup/restore uses `MediaStore` on Android 10+ (scoped storage) and direct file I/O below that.
