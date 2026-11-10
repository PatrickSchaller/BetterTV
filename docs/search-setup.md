# Universal search and Gallery setup

Version 0.2.1 keeps the native launcher and adds on-demand movie/TV search. Streaming data is not bundled or fabricated: a personal TMDB API Read Access Token is required for live streaming search.

## Install and open

Install `BetterTV-0.2.1-debug.apk` over the existing Better TV app, then open it. Existing favorites use the same preferences and package ID. Search screens are private to the launcher. The existing optional Home-button fix is retained; this release does not change TCL's startup policy or repair an accessibility switch that the TV turns off after Restart.

The Gallery home has a native clock/date, favorite-app dock, and All apps drawer. Hold OK on an app to pin/unpin. **Settings → New background** changes the wallpaper; returning Home chooses a different bundled image when possible. There are three original static artworks and no wallpaper network feed, video loop, or recommendation timer. See `gallery-artwork.md` and `artwork.md` for provenance and prompts.

## Enable live streaming search

1. On your PC or phone, create/sign into your own account at [TMDB](https://www.themoviedb.org/).
2. Open [account API settings](https://www.themoviedb.org/settings/api) and apply for developer API access for this personal, noncommercial launcher. If a project description is requested, a factual description is: "Personal Android TV launcher for a TCL television, with movie and TV search and US streaming-availability listings." Read and accept the provider's terms yourself. No account, subscription, or paid service was created by this project.
3. Copy the **API Read Access Token**. This is the bearer token, not the shorter v3 API key. Do not send it in chat or put it in a screenshot.
4. In Better TV, open **Search → Settings** (or Home → Settings → Search settings).
5. Enter the token in the masked field and save. A blank field keeps an already-saved token. The token is stored in app-private preferences; cloud backup is disabled and search settings are explicitly excluded from Android 12 cloud/device-transfer extraction rules. It is sent only in the HTTPS Authorization header to `api.themoviedb.org`; it is not placed in URLs or logs. The token field requests no personalized keyboard learning and disables autofill/input-state saving.

For easier entry, the same screen offers **Import token text file** using Android's system document picker. Put only the token in a UTF-8 `.txt` file under 8 KB on a USB stick, select it from the picker, then remove the token file from the USB afterward. No general storage permission is requested. If the TV has no document picker, use the masked field instead. **Remove saved token** clears it from app settings.

If the token is missing, search shows setup instructions. If it is rejected, rate-limited, offline, or times out, the UI reports the failure rather than displaying sample availability. No streaming passwords or account access are requested.

## Search and My services

- Enter a movie or TV title and choose Search. Posters, release year, and Movie/TV series labels distinguish matches. People results are excluded.
- Select a title for reported **United States** offers, separated into Subscription, Free, Free with ads, Rent, and Buy. Offers may depend on subscription tier or add-ons; verify access in the service.
- **My services** loads the current provider list for the US and lets you select your services manually. An installed app is not proof of a subscription.
- Toggle **Filter viewing options to my services** on/off. It filters viewing offers for a chosen title; title search itself still matches the title catalog. With no services selected, the filtered offer list is empty. Show all services restores the other options.
- For series, **Choose season** can fetch a season-specific provider listing. Series/season availability is not a guarantee about every episode.
- **Open [service] app** opens an installed, recognized app. It does not promise to play that exact title. TMDB's watch-provider endpoints do not return full streaming-service title deep links. Supported app names have explicit package mappings; add-on channels are not silently mapped to unrelated direct subscriptions.
- Missing US data is reported as unknown. An empty result is not proof that a title is unavailable everywhere. Rental/purchase prices are not supplied by these endpoints and are not invented.

## Antenna / over-the-air TV

**Search antenna guide** works independently of the TMDB token and can prefill the selected movie/show title. The first search explains and requests Android's **TV listings** permission. Denying it leaves streaming search usable.

This reads searchable listings already exposed by the TV's own antenna guide. It does not download a national ZIP-code guide, rescan stations, change TV setup, or upload the TV's guide. It considers browsable, unlocked US terrestrial ATSC/ATSC3 video channels and searches up to 100 channels / 10,000 cached rows over the next 48 hours, showing up to 40 matches. Cable, preview, radio, unknown-type and locked channels are excluded.

Version 0.2.1 defaults to **54650** when no ZIP has been saved. A ZIP you previously saved takes precedence; you can change it in Search settings, including to a ZIP with leading zeros. **Saving a ZIP does not configure the tuner or establish which stations you receive.** The displayed guide remains tied to the TV's existing antenna/channel setup. No free, no-key, officially supported ZIP-based schedule API was established for this build; external ZIP lineups would require an additional licensed data source and setup.

TCL may not share its native antenna listings with third-party apps, or its cached guide may be empty/stale. The app explains when guide data is unavailable; it does not fabricate local airings. Opening a match tunes the channel now, even if the listed program is in the future. It does not schedule a recording or future tune-in.

## Data sources and credits

- [TMDB API FAQ](https://developer.themoviedb.org/docs/faq): personal/noncommercial API access and attribution requirements.
- [TMDB application authentication](https://developer.themoviedb.org/docs/authentication-application): read access bearer token.
- [Movie watch providers](https://developer.themoviedb.org/reference/movie-watch-providers), [series watch providers](https://developer.themoviedb.org/reference/tv-series-watch-providers), and [season watch providers](https://developer.themoviedb.org/reference/tv-season-watch-providers): country-level data supplied through the JustWatch partnership; full provider deep links are not included.
- The Search settings/Credits section includes TMDB's approved logo, required nonendorsement notice, and JustWatch availability credit.
- `app/src/main/assets/tmdb_logo.png` is a faithful raster rendering of [TMDB's official logo SVG](https://www.themoviedb.org/assets/v4/logos/v2/blue_long_2-9665a76b1ae401a510ec1e0ca40ddcb3b0cfe45f1d51b77a308fea0845885648.svg), obtained from its [logos and attribution page](https://www.themoviedb.org/about/logos-attribution). It retains the approved colors/aspect ratio and is less prominent than Better TV's own mark.
- Antenna-guide permissions/access were checked against [Android 12 TvProvider](https://android.googlesource.com/platform/packages/providers/TvProvider/+/refs/tags/android-12.0.0_r1/AndroidManifest.xml) and its provider implementation. Only `READ_TV_LISTINGS` is requested at runtime; privileged EPG access is not requested.
