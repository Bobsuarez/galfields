# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Commands

```bash
npm start           # Start Expo dev server (opens QR code for Expo Go)
npm run ios         # Start with iOS simulator
npm run android     # Start with Android emulator
npm run web         # Start with web browser
npm run lint        # Run ESLint (eslint-config-expo/flat)
```

There is no test runner configured yet.

## Architecture

This is an **Expo 54 / React Native 0.81** app using **file-based routing via expo-router 6**. The New Architecture (`newArchEnabled: true`) and React Compiler (`experiments.reactCompiler: true`) are both enabled.

### Routing

Routes live under `app/`. expo-router turns filenames into URL segments automatically:

- `app/_layout.tsx` — root Stack navigator; wraps everything in `ThemeProvider` (light/dark)
- `app/(tabs)/_layout.tsx` — bottom tab navigator (Home, Explore)
- `app/(tabs)/index.tsx` — Home tab
- `app/(tabs)/explore.tsx` — Explore tab
- `app/modal.tsx` — modal screen (opened via `router.push('/modal')`)

The `unstable_settings.anchor = '(tabs)'` in the root layout makes the tabs group the default deep-link anchor.

### Theming

- `constants/theme.ts` — `Colors` (light/dark palettes) and `Fonts` (platform-specific font stacks)
- `hooks/use-color-scheme.ts` — re-exports `useColorScheme` from `react-native`
- `hooks/use-color-scheme.web.ts` — platform override for web
- `hooks/use-theme-color.ts` — resolves a named color key from `Colors` for the current scheme; accepts per-prop overrides

Themed components (`ThemedText`, `ThemedView`) use `useThemeColor` internally.

### Platform-specific files

Use React Native's platform extension convention for platform splits:
- `.ios.tsx` — iOS-only implementation (e.g., `icon-symbol.ios.tsx` uses native SF Symbols)
- `.web.ts` — web-only implementation (e.g., `use-color-scheme.web.ts`)
- No suffix — Android and web fallback

`IconSymbol` maps SF Symbol names (iOS) to Material Icons (Android/web). Add new icon mappings in the `MAPPING` object inside `components/ui/icon-symbol.tsx`.

### Path alias

`@/` resolves to the repo root (configured in `tsconfig.json`). Use `@/components/...`, `@/hooks/...`, `@/constants/...` for all internal imports.
