# ResolveX - Phase 3: the new design

Everything except the splash screen now follows the reference design (warm off-white background,
bordered white cards, one orange accent, near-black buttons, tinted status pills, mono IDs).

## Where things live
- `res/values/colors_rx.xml` - the colours (design tokens). `res/values/themes_rx.xml` - the theme `Theme.Rx`
  and all shared styles (`Rx.Text.*`, `Rx.Chip`, `Widget.Rx.Button*`, `Widget.Rx.TextField`, ...).
- `res/font/` - Hanken Grotesk (text) and JetBrains Mono (issue IDs). Licences: `licenses/`.
- `utils/UiUtils.java` - status/priority pills, the 6-step progress bar, header + bell, stat cards, system-bar spacing.
- `utils/BottomBar.java` - the bottom tabs for each role (students also get the raised "+").
- `utils/AssignDialog.java` - choose a staff member (assign / reassign), used by the queue and the issue screen.
- `res/layout/view_*.xml` - shared pieces: header, top bar, stat card, search box, filter panel, bottom bar.
- Light mode only. The splash screen keeps its original look (it still uses `Theme.ResolveX`).

## Screens
- Student: home, report an issue (one screen, category dropdown), my issues, issue detail, alerts, profile.
- Staff: home (stats, "needs attention first", queue with quick chips + filters), issue detail, alerts, profile.
- Admin: overview (stats, weekly chart, category bars, feedback, recurring, activity), issue queue
  (chips with counts, inline Assign/Reassign, CSV), manage (resolvers with open-work count, students), alerts, profile.
- Login, register, help, feedback list, recurring-issue detail and all dialogs.

## New behaviour (small)
- Admin can reassign an issue (history shows "Reassigned", both staff are notified).
- Overview numbers come from new read-only queries: weekly counts, category counts, staff workload, unassigned count.
- Bottom bar: going to a tab no longer piles up screens; Back from a tab returns to the home tab.

## Follow-up: admin buttons and no pop-up dialogs
- The app no longer uses pop-up dialogs. What used to be a dialog is now a screen or an inline control:
  - Person details (tap a resolver or student in Manage, or tap the reporter / assigned staff on an issue): `UserDetailActivity`
    (contact info, their issues, reset password).
  - Add staff: `AddStaffActivity`.
  - Assign / Reassign: `AssignStaffActivity`.
  - Change priority (admin): Low / Medium / High chips on the issue screen.
  - Student feedback: the rating form appears on the issue screen right after "Verify".
  - Log out: tap twice (the button changes to "Tap again to log out").
- "Help and guidelines" is removed from the profile for every role (screen, asset and manifest entry deleted).
