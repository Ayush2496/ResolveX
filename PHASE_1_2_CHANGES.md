# ResolveX - Phase 1 + 2 changes

Demo logins (created on a fresh install):

| Role    | Email                   | Password   |
|---------|-------------------------|------------|
| Student | student@nmims.in        | student123 |
| Student | student2@nmims.in       | student123 |
| Staff   | staff@nmims.in          | staff123   |
| Staff   | elec@nmims.in           | staff123   |
| Staff   | maint@nmims.in          | staff123   |
| Admin   | admin@nmims.in          | admin123   |

Seed data only appears on a fresh install (uninstall the app or "Clear storage").
An existing install is upgraded in place (database version 5 -> 6) and keeps its data.

## Phase 1 - reliability
- Camera: removed the unused CAMERA permission (it made "Take photo" crash). The camera app now saves a
  full-size photo through the FileProvider. Photos survive screen rotation.
- Notifications: the app now asks for notification permission (Android 13+) and shows a phone
  notification when there are new unread updates.
- Verified-but-not-closed dead end: the feedback dialog can be dismissed, and a VERIFIED issue shows
  "Give feedback & close". Rating must be 1-5.
- Database v6: fixes the wrong foreign key on issue_verification, hashes old plain-text passwords, adds indexes.
- Timestamps are created in one place (TimeUtils), local time, Locale.US.
- Location is a campus-area dropdown plus an optional room, so recurring-issue detection groups reliably
  (comparison ignores capitals and extra spaces).
- Security basics: salted PBKDF2 password hashes, allowBackup=false, FileProvider limited to two folders,
  CSV cells starting with = + - @ are neutralised, photos are shrunk before display and rotated by EXIF.

## Phase 2 - features from the spec
- Filters (status, category, priority, date) for students, staff and admin; search also finds "RX-0007".
- Admin: change priority (recorded in the history), view students, reset any user's password,
  notifications screen ("Alerts" tab), CSV export now exports exactly what the filters show.
- Staff can only open issues assigned to them.
- "Resolution verified" now also notifies the admin.
- Phone numbers must be 10 digits.
- Demo data now tells a full story (closed, resolved, reopened, in progress, recurring).

## Removed
- app/src/main/res/AndroidManifest.xml (unused duplicate manifest).
