# Style preview and control fixes

User supplied a concrete correction list; implement directly in no-plugins, then test, commit, push and build per AGENTS.md. No additional design approval needed.

- [x] Move the entire General appearance section to the top, Freezgram style first inside it.
- [x] Split composer layout (Android/iOS) from Liquid Glass, preserve legacy selections and export the new preference.
- [x] Make preview material follow its opt-in, show all previews initially, preserve wallpaper on header refresh and show native icons.
- [x] Fix native message cells suppressing text due to MessageObject.preview; add readable sample messages.
- [x] Add live native menu/side-button/attachment-tab previews to their switches.
- [x] Inline snow/motion selectors in the style list; label smooth transitions Freezgram (smooth).
- [x] Show enhance-colors/wallpaper-dimming as direct switches, remove Advanced disclosure per follow-up.
- [x] Synchronize popup separators with material and row animation alpha/geometry.
- [x] Target regressions, full tests, Java/XML checks and read-only review.
- [ ] Commit/push and verified APK (tracked in the task response).

Diagnosis: ChatHeaderPreview.refresh removed BackgroundView via removeAllViews; normal ChatMessageCell.onDraw invokes drawContent(canvas,false), but preview used MessageObject.preview=true. Navigation editor effectsEnabled checked only device/power capability, not opt-in. Popup separator overlay used visibility=1 irrespective of backAlpha, child alpha or expanding background bounds.

Read-only review identified two issues, both addressed: clip dividers to all revealed popup segments and reuse the themed native popup background when glass is off. Final verification: 93 passed, 1 skipped, 2675 subtests passed; 17 Java files parsed, XML/resource references valid, git diff --check clean. The old header fails the wallpaper regression and the corrected header passes. APK build pending push.
