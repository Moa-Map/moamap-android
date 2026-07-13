# Android Design System Design

## Goal

Apply the Color and Typography tokens from `Moa-Map/design-system` revision `1b68bb68f959b85f66d7a9c421f67ba1459a96f6` to the Android app as a manual Compose design system.

## Scope

Included:

- Primitive and semantic Compose colors
- The semantic primary background gradient
- NanumSquareOTF Light, Regular, Bold, and ExtraBold
- Pretendard Regular
- All 20 semantic typography styles
- Custom Moa Map token access integrated with `MaterialTheme`
- A preview-only design-system catalog
- Build and existing-test verification

Excluded:

- Icons, dark theme, dynamic colors, spacing, and radius
- Figma synchronization, code generation, and scheduling
- Project README changes

## Architecture

Use a hybrid theme. Moa Map custom tokens are the canonical API because Material 3 cannot represent all semantic colors, the gradient, or all 20 typography styles. The same tokens also map into Material 3 `ColorScheme` and `Typography` so standard components remain usable.

```kotlin
Text(
    text = "Moa Map",
    color = MoaMapTheme.colors.textNormal,
    style = MoaMapTheme.typography.title1,
)
```

## Files

### `app/src/main/res/font/`

Add:

- `nanum_square_light.otf`
- `nanum_square_regular.otf`
- `nanum_square_bold.otf`
- `nanum_square_extra_bold.otf`
- `pretendard_regular.otf`

Verify official redistribution terms before committing the font binaries.

### `Color.kt`

Keep primitive colors internal and expose an immutable `MoaMapColors` with:

- Text: `textNormal`, `textAlternative`, `textAssistive`, `textDisable`, `textWhite`
- Background: `backgroundSecondary`, `backgroundPrimary`
- Status: `statusAlert`, `statusCaution`, `statusPositive`
- Line: `lineNormal`, `lineAlternative`

`backgroundPrimary` is a vertical `Brush` from Blue 100 to White.

### `Type.kt`

Define NanumSquareOTF weights 300, 400, 700, and 800 plus Pretendard weight 400. Expose exactly 20 styles through `MoaMapTypography`:

- `display1`, `display2`
- `title1`, `title2`, `title3`
- `subtitle1`, `subtitle2`, `subtitle3`, `subtitle4`
- `body1`, `body2`, `body3`
- `button0`, `button1`, `button2`, `button3`, `button4`
- `caption0`, `caption1`, `caption2`

Figma sizes and calculated letter spacing map to Compose `sp`. Ratio line heights map to `fontSize * ratio` in `sp`.

### `Theme.kt`

`MoaMapTheme` provides custom colors and typography through composition locals, exposes `MoaMapTheme.colors` and `MoaMapTheme.typography`, and installs mapped Material 3 values. Only the approved light token set is used; Android dynamic colors are disabled.

### `DesignSystemPreview.kt`

Display every semantic solid color, the primary gradient, and all 20 typography styles. The preview catalog is not connected to navigation or the runtime start screen.

## Data Flow

```text
Common DTCG tokens
    -> manual Android mapping for issue #11
    -> Color.kt / Type.kt
    -> MoaMapTheme composition locals and MaterialTheme
    -> feature composables
```

## Verification

- Confirm all five font resources exist with valid Android names.
- Compare every semantic Color and all 20 Typography tokens with the common repository.
- Render the preview catalog for visual comparison.
- Compile existing unit and instrumentation tests.
- Run `assembleDebug` to verify Kotlin, Compose, and font resource integration.

## Completion Criteria

- App code accesses semantic colors and all typography styles through `MoaMapTheme`.
- Standard Material 3 components receive mapped values.
- The preview catalog renders without missing resources.
- The Android project builds and existing tests pass.
- No excluded work is introduced.
