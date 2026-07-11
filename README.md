# react-native-nitro-date-picker

A fast, type-safe date and time picker for React Native, built with [Nitro Modules](https://nitro.margelo.com).
A Nitro-based reimplementation of [henninghall/react-native-date-picker](https://github.com/henninghall/react-native-date-picker) — the same spinner/wheel UI and props, powered by Nitro.

## Repository layout

This is a monorepo:

- [`packages/react-native-nitro-date-picker`](packages/react-native-nitro-date-picker) — the publishable library. **See its [README](packages/react-native-nitro-date-picker/README.md) for installation, usage and the full props table.**
- [`example`](example) — an Expo app demonstrating the picker.

## Development

```bash
# install workspace deps
bun install

# regenerate Nitro bindings after changing src/specs/*.nitro.ts
bun run specs

# build the library's TS into lib/
bun --cwd packages/react-native-nitro-date-picker run typescript

# run the example app
cd example && bun run ios     # or android / web
```

## License

MIT
