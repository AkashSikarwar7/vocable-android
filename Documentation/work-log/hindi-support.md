# Hindi (हिन्दी) support

No GitHub issue yet. Per the repo workflow, open one with `/create-ticket` before this lands, and rename this file to `<issue-number>-hindi-support.md`.

## Why

Hindi already shows up as a language: `values-hi-rIN/strings.xml` exists and Crowdin writes to it. But a Hindi-language device still got an almost entirely English app:

| Area | State before this work |
|---|---|
| UI strings (`values-hi-rIN/strings.xml`) | 165 of 174 translatable keys present, but only **9** actually in Hindi. The other 156 were English copies, and 13 keys (all the reset-settings copy) were missing. |
| Preset phrases and categories | 0 of 71 in Hindi. The arrays in `values/arrays.xml` only reference `@string/preset_*`, and `Phrase.text()` resolves them at runtime with `getIdentifier`, so translating `strings.xml` is all the presets need. No Hindi `arrays.xml` is required for phrases. |
| Keyboard | Latin A–Z only (`keyboard_keys`). A Hindi user cannot type Devanagari with head tracking. |
| TTS | Already works. `KeyboardViewModel`, `PresetsViewModel` and the voice picker pass `Locale.getDefault()`, so a `hi-IN` device gets a Hindi voice if one is installed. |
| Fonts | Fine. The theme uses `FontFamily.Default`, and the system Noto Devanagari font covers it. |

The iOS app (source of truth per `CLAUDE.md`) already ships Crowdin-approved Hindi for the UI (`Localizable.xcstrings`) and for all 71 presets (`CrowdinExport/hi/Presets.xliff`). It has **no** Hindi keyboard: `KeyboardLocale` is `en`/`de`/`it` only.

## Plan

1. **UI strings.** Fill `values-hi-rIN/strings.xml` completely.
   - Reuse iOS Crowdin translations where the English source matches (33 UI strings).
   - Translate the remaining 60 by hand.
   - Keep placeholders (`%1$s`, `%1$d`, `%s`), `<b>` markup and the `xliff:g` tag identical to English.
2. **Preset phrases.** Start from the iOS-approved Hindi, then fix two classes of problem (see Decisions).
3. **Devanagari keyboard.** This needs a product decision before building; see the open question below.
4. **Tests.** Add `HindiResourcesTest`, a JVM test that parses the XML off disk, following the `VoiceGridResourcesTest` pattern since there's no Robolectric. It asserts:
   - every translatable English key has a Hindi entry
   - every `preset_*` string is in Devanagari
   - format placeholders match between English and Hindi
5. **Optional, separate ticket.** Add `res/xml/locales_config.xml` plus `android:localeConfig` so Android 13+ users can choose Hindi for Vocable alone, from the system's per-app language setting. `LocalizedResourceUtility` already reads `AppCompatDelegate.getApplicationLocales()`, so the plumbing is half there.
6. **Crowdin.** `values-hi-rIN/strings.xml` is a Crowdin download target (`crowdin.yml`). Upload these translations to the Crowdin project too, or the next Crowdin sync will overwrite them.

## Decisions

- **Gender-neutral first-person presets.** Hindi marks the speaker's gender on verbs and adjectives. The iOS translations are all masculine (`मैं प्यासा हूँ`, `मैं थक गया`, `मैं नहीं जानता`, `मैं लेटना चाहता हूँ`), which is wrong for every female user of an AAC app. Where Hindi allows it, phrases were reworded into the gender-neutral `मुझे … है` form, for example `मुझे प्यास लगी है` (I am thirsty) and `मुझे नहीं पता` (I don't know). A per-user gender setting would be the fuller fix, but that is a feature, not a translation.
- **Corrected iOS mistranslations.** These diverge from iOS on purpose and should be reported back to the Crowdin project:
  - `preset_open_blinds`/`close_blinds`: `अंधा` means a blind *person*. Changed to `पर्दे` (curtains/blinds).
  - `preset_im_hot`: `में गर्म हूँ` is a typo (`में` means "in") and the wrong sense. Changed to `मुझे गर्मी लग रही है`.
  - `preset_how_is_day`: `आप` was a typo for `आपका`.
  - `preset_i_like_it`: `मुझें` was a typo for `मुझे`.
  - `preset_please`: `कृप्या` was a misspelling of `कृपया`.
  - `preset_good_morning`: `सुबह बख़ैर` is an Urdu greeting. Changed to `सुप्रभात`.
  - `preset_good_evening`: `सुसंध्या` is unidiomatic. Changed to `शुभ संध्या`.
  - `preset_please_stop`: `बंद करो` means "shut it". Changed to `कृपया रुकें`.
  - `preset_im_finished`: `मैं समाप्त हो चुका हूँ` reads as "I am finished off". Changed to `मेरा हो गया`.
  - `preset_close_window`: changed `विंडो` to `खिड़की` to match `preset_open_window`.
  - `preset_need_shower`: shortened the literal `फुहारे में स्नान` to `मुझे शॉवर लेना है`.
- **Digits stay Latin** (`1`–`0` on the 123 keypad). Those keys are `translatable="false"`, and Latin digits are what Indian users see on phones and money.
- **App name stays "Vocable"**, in Latin script.

## Keyboard: two pages with a toggle key

Devanagari needs about 60 keys. One grid with all of them would be 12 rows tall on a 5-column phone, too small to dwell-select reliably. Large keys in fixed positions are this app's accessibility contract, so the keyboard is split into two pages.

At 5 columns, the phone layout is:

```
Page 1 (व्यंजन)            Page 2 (स्वर / मात्रा)
क  ख  ग  घ  ङ              अ  आ  इ  ई  उ
च  छ  ज  झ  ञ              ऊ  ऋ  ए  ऐ  ओ
ट  ठ  ड  ढ  ण              औ  ा  ि  ी  ु
त  थ  द  ध  न              ू  ृ  े  ै  ो
प  फ  ब  भ  म              ौ  ं  ँ  ः  ़
य  र  ल  व  श              क्ष त्र ज्ञ श्र ।
ष  स  ह  ्  [अआ]           ,  ?  !  [कख]  ·
```

- **Page 1** holds the consonants in varnamala order, so each 5-key row is one varga. It ends with the halant (`्`), which builds any other conjunct.
- **Page 2** holds the vowels, matras, `ं ँ ः ़`, the four varnamala conjuncts, the danda and punctuation.
- **Toggle key.** The last key on each page switches to the other page. Its label previews the target page (`अआ` / `कख`), it uses `CategoryColor` so it doesn't read as a letter, and it announces "अक्षर बदलें" (switch letters) on gaze hover.
- **Fixed key size and position.** Both pages are padded to the same row count, so keys never move or resize when the page flips. That's 7 rows at 5 columns and 4 rows at 10 columns, which equals the German keyboard and stays inside the existing row budget.

Implementation:

- **Arrays.** `values/arrays.xml` gains an empty `keyboard_keys_page2`, so every other locale is unchanged. `values-hi/arrays.xml` overrides it along with `keyboard_keys`.
  - It uses `values-hi`, not `values-hi-rIN`, so it matches both `hi` and `hi-IN`.
  - The locale qualifier outranks `-land`/`-sw600dp`, so this one layout is used in every orientation.
- **Layout function.** `ui/keyboard/KeyboardPages.kt` holds `keyboardPageRows`, a pure function so it can be unit-tested without Compose. With one page it produces exactly the old `chunked(columns)` grid.
- **Screen.** `KeyboardScreen` keeps the page index in `rememberSaveable`, so it survives rotation, and renders `KeyboardCell.Character` / `PageToggle` / `Empty`.
- **New string.** `keyboard_switch_page` was added in English and Hindi. The other locales fall back to English until Crowdin translates it.

Not done on purpose:

- **No auto-return after a matra.** The page doesn't flip back to consonants after a vowel sign is typed. That would save a dwell per syllable, but it moves the user's target out from under them. It's worth trying only with real users.
- **No transliteration.** There is no phonetic (Latin→Devanagari) input.
- **Touch typing is unchanged.** With head tracking off, the keyboard screen uses the system keyboard, which already supports Hindi.

`KeyboardViewModel.onKey` lowercases keys, which is a no-op for Devanagari, so no ViewModel change was needed.

## Progress

| Step | Status |
|---|---|
| 1. UI strings | Done. All 174 translatable keys are in Hindi. 33 come from iOS Crowdin and 60 are hand-translated; the rest are corrections or keys that were already Hindi. |
| 2. Preset phrases | Done. All 71 are Hindi, with the iOS corrections and gender-neutral rewrites listed above. |
| 3. Devanagari keyboard | Done: the two-page layout described below. |
| 4. Tests | `HindiResourcesTest` (`app/src/test/java/com/willowtree/vocable/locale/`) and `KeyboardPagesTest` (`.../keyboard/`) are written. Neither has been run with Gradle. |
| 5. Per-app language picker | Not started; separate ticket. |
| 6. Crowdin upload | Not done; needs project access. |

## Verification status

- The same four assertions `HindiResourcesTest` makes were checked with a Python script against the generated XML, and all passed: no missing keys, no stale keys, no English copies, placeholders identical.
- `keyboardPageRows` was mirrored in Python and run against the real resources at every `keyboard_columns` breakpoint. Both pages came out at 7 rows for 5 columns and 4 rows for 10 columns, with no duplicate keys.
- The Kotlin has **not been compiled**. The keyboard has **not been seen on a device**, including how standalone matras render (Android draws them on a dotted circle ◌) and whether `क्ष`-style conjuncts fit at the key text size.

- No JDK or Android SDK on the machine this was written on, so `./gradlew testDebug` was **not** run.
- The translations have not been reviewed by a native Hindi speaker.
- Layout with Devanagari text (taller glyphs from matras above and below the line, in fixed-height tiles) has not been checked on a device.
