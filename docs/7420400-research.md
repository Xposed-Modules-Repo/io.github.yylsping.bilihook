# Bilibili 7.42.0 / 7420400 research notes

This record covers symbols checked against the 7.42.0 APK pulled from the connected PLQ110.
JADX output was cross-checked with original DEX smali where a decompiler ambiguity mattered;
runtime causality was then verified with narrowly scoped LSPosed debug logs. No analysis-time APK,
screenshot, dump, account credential, or diagnostic script is shipped in the module.

## Baseline

- Package/process: `tv.danmaku.bili` / `tv.danmaku.bili`
- Version: `7.42.0` / `7420400`
- UGC quality sample: `BV14p4y1X7Xc`, part 1 (`4096x2160 50FPS`)
- PGC sample: `bilibili://pgc/season/ep/693247`
- Logged-in non-VIP baseline: premium rows are visible but the host membership gate rejects them.
- The implementation uses exact symbols only. It contains no DexKit, runtime DEX scan, class-name
  search, stack-string match, or version-range fallback.

## Final quality design

| Meaning | Exact 7.42.0 symbol/signature | Final use |
| --- | --- | --- |
| Login state | `BiliAccounts.get(Context)` + `isLogin()` | Checked before creating or authorizing a transaction |
| Scoped current-account VIP | `BiliAccountInfo.isEffectiveVip()` | Temporarily overridden only inside two explicit quality scopes (transaction gate and startup/auto decision); everywhere else the host's original VIP result is returned |
| UGC selection/gate/request | `PlayerQualityService.t5(int, String)` / `N0(int, String)` / `e3()` | Owner/content-bound transaction and native request correlation |
| PGC selection/gate/request | `l.t5(int, String)` / `S1(int, String)` / `B1()` | Owner/content-bound transaction and native request correlation |
| Completion | each service's `a(boolean, int, int, boolean)` | Observed only; the callback, Toast, and UI are never rewritten |
| Startup/auto decision | UGC `i6(boolean)`, PGC `Z6(boolean, boolean)` | `QualityDecisionScope` opens the scoped VIP override only while the host's own decision runs |
| Decision ceiling | UGC field `h`, PGC field `i` | Read-only; the host-loaded preference ceiling decides whether the scope may open |
| Resource handoff | UGC `s3(MediaResource)`, PGC `c2(MediaResource)` | Debug-only observation of the native result |
| Content identity | current director `N0().f()` | Bound to every transaction in addition to service object identity |
| Request builders | UGC PlayView v1 / PlayURL v1; PGC PlayView v1 / PlayView v2 `setQn(long)` | Exact symbol availability is reported; none is hooked or mutated |

A premium selection creates one transaction keyed by the concrete quality-service object identity.
The transaction also stores the current content key, target quality, generation, phase, request id,
and expiry. A different player object, content key, target, or generation cannot authorize, consume,
or complete it. Normal-quality and logged-out selections clear the owner's transaction.

`N0`/`S1` authorizes the matching transaction and sets a `ThreadLocal` only around the original
quality gate. During that narrow call, `BiliAccountInfo.isEffectiveVip()` is overridden for the
logged-in current account. The marker is removed after the gate. The host then performs its normal
`j3`/`O1` switch logic and carries the selected qn through its own fields and request construction.
The module does not write `setQn`, resolver qn, callback arguments, media resources, or URLs.

For a direct successful switch, completion belongs to the transaction when the actual quality is
the selected target; this is intentionally independent of the service's mutable expected-quality
field, which can still contain the old value. A resolver failure is matched only after that owner's
transaction entered `REQUESTED` and its expected field equals the target. An ambiguous direct
failure is left to a subsequent owner selection/normal selection or expiry instead of risking that
an unrelated callback clears it. Retention is harmless to other requests because retained state is
not a source of request qn and remains owner/content scoped.

The four builders are classified as follows:

- required: UGC PlayView v1, PGC PlayView v2;
- optional legacy/alternate path: UGC PlayURL v1, PGC PlayView v1.

All four exact symbols were present on 7420400. Initialization reports every result individually
and marks either missing required symbol unavailable. “Present” is an ABI check, not a global hook.

## Why global VIP model hooks were removed

The initial candidate globally replaced `VipUserInfo.isEffectiveVip()` and
`VipExtraUserInfo.isEffectiveVip()`. DEX call-site review rejected that design: the models are not
quality-only and are reached from current-account UI as well as author/profile, comments, emoticon,
audio, projection, Story, and other membership-related presentation paths. They therefore cannot
be treated as safe process-wide current-account-only receivers.

The final implementation hooks neither model. The only VIP override is the current-account
`BiliAccountInfo.isEffectiveVip()` query, and it is temporarily overridden inside exactly two
explicit quality contexts:

1. the owner/content-bound user-initiated premium switch transaction (the `N0`/`S1` quality gate
   on the current thread, protected by the transaction marker above);
2. the host's synchronously executed `i6`/`Z6` startup/auto quality decision scope.

Outside these two scopes — including comment list rendering and user profiles — the query always
returns the host's original VIP result.

## Runtime quality evidence

On `BV14p4y1X7Xc`, the host returned real playable DASH entries and produced genuine callbacks for
the complete `80 -> 120 -> 116 -> 80` sequence:

- `80 -> 120`: `success=true`, actual quality `120`; the fullscreen control displayed `4K`.
- `120 -> 116`: `success=true`, actual quality `116`; the control displayed `1080P 60帧`.
- `116 -> 80`: `success=true`, actual quality `80`.
- A separate attempt at the end of the short video produced the real failure
  `success=false, requested=80, actual=80`; no callback, Toast, or UI state was forged.

The final completion matcher was rechecked after the state-machine fix. A direct `80 -> 120`
callback arrived with the service field still at `expected=80` and `actual=120`; it matched by the
actual target and emitted the correlated completion for generation 1. A later normal selection
cleared the transaction and completed natively at actual quality 80.

For rapid navigation, a 116 transaction on content `968340788` was followed immediately by opening
`BV1xx411c7mD`. The second video opened at its native 480P capability instead of inheriting 116;
its later native 360P selection logged `transaction=none`, content `2`, and completed at 16. This is
also guaranteed structurally: no global request builder reads transaction state, and the old
service/content pair cannot authorize the new pair. Recommendation cards remained active during
the UGC runs without consuming or receiving the selected qn.

The PGC sample played normally at quality 80. Selecting target 112 produced the correlated native
request for episode `693247`, but the server reply selected 80 and listed 112 as VIP with no
playable URL while 80 had a URL. The host emitted no success callback for that branch and remained
at the playable quality. Temporary RPC diagnostics used to establish this fact were removed from
the source after capture. This is the required real “server has no target source” failure, not a
client-generated substitute.

Detailed selection/resource logs are guarded by `BuildConfig.DEBUG`. Release builds retain only
the initialization summary and genuine hook-installation errors, and never log DASH URLs, cookies,
tokens, authorization headers, or account identifiers.

## Native comment regression

7420400 comments remain entirely host-native: there is no comment image, protobuf/REST,
RecyclerView/ViewHolder, URL, or image-viewer hook.

Agent-side regression used current public comments with known image payloads:

- `BV1ame16qE9Z`: a single-image comment rendered, opened in the native viewer, and returned to the
  same comment list normally.
- `BV1ZLeG6JEha`: a two-image comment rendered both thumbnails; the native viewer moved from `1/2`
  to `2/2` and returned normally. A visible reply thread remained available.
- The same list displayed a gray non-VIP author and a pink VIP author simultaneously. This agrees
  with the removal of global VIP model hooks and disproves uniform current-account VIP pollution.

## Advertising symbols and smoke checks

All 7420400 ad features install independently, so a missing optional ad symbol cannot prevent the
quality chain from installing.

| Area | Exact hook | Behavior |
| --- | --- | --- |
| Splash | `Splash.isValid()` + `isBirthSplash()` | Invalidates normal splash only; preserves birthday splash |
| View-unite related | `RelatesFeedReply.getRelatesList()` | Removes only `CM` / `hasCm` / `hasCmStock` cards |
| Legacy related/under-player | exact `ViewReply` and `RelatesFeedReply` getters | Filters CM lists and returns protobuf defaults for CM slots |
| View-unite under-player | exact `viewunite.v1.CM` getters | Clears only the under-player CM slot |
| Home JSON | `BaseTMApiParser.e(JSONArray)` | Filters explicit ad markers before parsing |
| Home BRPC | `BrpcRespConverterKt.a(List)` | Filters explicit ad enum cases and `AdItem` outputs |
| Search BRPC | `BrpcSearchResultConverterKt.a(List, SearchResultAll)` | Filters only `CM` cases |

After a force-stop, the app reached the home feed without an ordinary splash ad. Home, an LSPosed
search, video recommendations, and the under-player feed loaded normally with no visible `广告`
label, current-process hook exception, or Bilibili crash. No ad scope was added during the P1 work.

## Cross-video quality preference inheritance

The host already implements the persistent user quality preference; the module only removes the
membership clamp inside the host's own startup decision.

- Storage: `PlayerQualityService.t5(int, String)` -> `N2(int)` persists the user's last explicit
  selection to `pref_player_mediaSource_quality_wifi_key` in `app_blkv/biliplayer.blkv` (via
  `tv.danmaku.biliplayerv2.service.setting.c`). The value survives force-stop and process restart;
  device inspection showed `0x78` (120) after a 4K selection. With the cloud flag
  `Memorable_qn` at its default 0, every explicit selection is saved.
- Read-back: a new video's `s1()` (UGC) / `J0()` (PGC) reloads the preference through `k.c()`
  (auto-switch is off on the test device, so the saved Wi-Fi preference is returned) into field
  `h` (UGC) / `i` (PGC). The initial playurl request already asks for that qn
  (`PlayerQualityService$c.a(NORMAL_PLAY)` returned 120 for a non-VIP account).
- Startup decision: `r2()` calls `i6(boolean)` (UGC) / `Z6(boolean, boolean)` (PGC), which walks
  `VodIndex.a` descending and takes the first playable entry that is not above the ceiling
  (`s2(entry, ceiling) <= 0`), skipping `needVip` entries when
  `BiliAccountInfo.isEffectiveVip()` is false.
- Root cause of the P1: the ceiling (e.g. 120) was persisted and re-read correctly, but the VIP
  check inside `i6`/`Z6` dropped every premium entry, so a fresh video fell back to plain 80.
  Confirmed live: `i6 enter {h=120} available=[120:vip, 116:vip, 80, ...]` -> `i6 => 80`.
- Fix: `QualityDecisionScope`, a thread-scoped frame stack that wraps only the `i6`/`Z6` call, and
  only when the host-read ceiling is premium (>= 112) and the account is logged in. Within that
  call the existing scoped `BiliAccountInfo.isEffectiveVip()` override applies; the host's native
  strategy still performs all ranking and fallback. No qn is stored, rewritten, or reused across
  contents.
- Nesting: every `enter(...)` pushes an independent frame (a refused entry pushes an inactive
  frame), the innermost frame alone decides the current state, an inactive inner frame never
  inherits an active outer frame, `exit()` pops only the current frame so the outer frame is
  restored, and the `ThreadLocal` itself is removed once the outermost frame exits. An unmatched
  `exit()` is ignored safely (one debug diagnostic, no crash, no residue).

Device evidence on a logged-in non-VIP account (module active, single-part videos):

- 120 ceiling, video with 120: `i6 => 120`, real 4K playback, fullscreen control showed `4K`.
- 120 ceiling, `BV1n77i6EEVR` (max 116): `i6 => 116`, `r2() exit f=116`; the host even re-requested
  with `qualityProvider.a(UPDATE_MEDIA_RESOURCE) => 116`. No manual click involved.
- 116 ceiling (manually selected `1080P 60帧`, `N2 savePref 116`), 120-capable video: `i6 => 116`;
  no upgrade to 120, because the host skips entries above the ceiling regardless of capability.
- 80 reset (manually selected `1080P 高清`, `N2 savePref 80`): the scope never opens
  (`ceiling=80 < 112`), `i6 => 80`, no premium leakage on later videos.
- 120 ceiling, `BV19CbA6rE4x` (max 64, no premium source): `i6 => 64`; the preference is not
  erased, and the next 120-capable video auto-selected 120 again.
- Rapid switching across three videos (3 s apart): each video ran its own `s1` -> scoped `i6`
  bound to its own content key (116 / 120 / 64 respectively); no state crossed contents.

## Hi-Res audio research

`BV1ovYN68EEP` carries a VIP-only Hi-Res audio option. The parsed `MediaResource` on the logged-in
non-VIP device shows `hires={type=3, needVip=true, audio=null}` and only standard DASH audio tiers
(`30280/30232/30216`) with URLs. Unlike UGC premium video — where the server does return the
premium streams and only flags them `needVip` — the server withholds the Hi-Res stream entirely.
A local hook therefore cannot unlock Hi-Res the way video quality is unlocked, and the module does
not attempt it. The same server-side withholding applies to PGC premium URLs (the module preserves
that real server result).

## Compatibility and residual limits

- 7040300 still dispatches to its original `BiliHook` path; none of the 7420400 state machine is
  shared with it. Exact-version tests cover 7040300, 7420400, and unsupported versions.
- The PGC server may still withhold premium media from a non-VIP account after the local gate. The
  module deliberately preserves that server decision.
- Some direct-failure callbacks expose only the previous quality. Such an ambiguous transaction is
  retained until a new selection, a normal selection, or the 30-second expiry; it cannot mutate a
  request while retained.
- These checks are Agent-side evidence and remain subject to the maintainer's final device/release
  acceptance.
