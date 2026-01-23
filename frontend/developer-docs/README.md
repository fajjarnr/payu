# PayU Developer Documentation

Developer documentation site for PayU digital banking platform integration.

## Features

- **Integration Guides**: Step-by-step guides for Partner payments, QRIS, and BI-FAST
- **SDK Examples**: Ready-to-use SDK samples for Java, Python, and TypeScript
- **i18n Support**: Documentation in Bahasa Indonesia and English
- **Premium Emerald Design**: Consistent with PayU design system
- **Static Site Generation**: Optimized for performance

## Tech Stack

- **Framework**: Next.js 16 with App Router
- **Styling**: Tailwind CSS v4 with Premium Emerald design system
- **i18n**: next-intl for internationalization
- **Language**: TypeScript

## Getting Started

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

## Project Structure

```
developer-docs/
├── src/
│   ├── app/[locale]/          # Localized pages
│   │   ├── getting-started/   # Quick start guide
│   │   ├── guides/            # Integration guides
│   │   │   ├── partner-payments/
│   │   │   ├── qris-payments/
│   │   │   └── bifast-transfers/
│   │   └── sdk/              # SDK examples
│   │       ├── java/
│   │       ├── python/
│   │       └── typescript/
│   ├── components/            # Reusable components
│   ├── i18n/                 # Internationalization
│   │   ├── id/               # Bahasa Indonesia
│   │   └── en/               # English
│   └── lib/                  # Utility functions
├── public/                   # Static assets
└── package.json
```

## Deployment

The site is configured for static export. Build output will be in the `out/` directory.

```bash
npm run build
```

## Design System

The documentation follows the **Premium Emerald** design system:

- **Primary Color**: Bank Green (#10b981)
- **Secondary Color**: Bank Emerald (#059669)
- **Font**: Inter (default) and Geist Mono (code)
- **Border Radius**: 2.5rem for main cards, 3xl for modals
- **Spacing**: Consistent spacing with Tailwind utilities

## Contributing

When adding new documentation:

1. Create pages in both `src/app/[locale]/` directories
2. Add translations in `src/i18n/id/messages.ts` and `src/i18n/en/messages.ts`
3. Follow the Premium Emerald design system
4. Test locally before committing

## License

Proprietary - PayU Digital Banking
