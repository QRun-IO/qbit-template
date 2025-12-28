# QBit Template (Archived)

[![License](https://img.shields.io/badge/license-GNU%20Affero%20GPL%20v3-green.svg)](https://www.gnu.org/licenses/agpl-3.0.en.html)

> **This repository has been superseded by type-specific templates.**

## Use the Right Template

Choose the template that matches your QBit type:

| Template | Use When |
|----------|----------|
| **[qbit-template-extension](https://github.com/QRun-IO/qbit-template-extension)** | Extending QQQ infrastructure (auth, audit, customizers) |
| **[qbit-template-data](https://github.com/QRun-IO/qbit-template-data)** | Providing reference data (countries, currencies, codes) |
| **[qbit-template-application](https://github.com/QRun-IO/qbit-template-application)** | Building complete applications (WMS, CRM, inventory) |

## Why Three Templates?

Each QBit type has distinct patterns:

- **Extension QBits** - No tables, hooks into existing QQQ behavior
- **Data QBits** - Table prefixing, sync processes, Liquibase generators
- **Application QBits** - QAppSection (required), widgets, processes

Type-specific templates provide the right structure and documentation for each use case.

## Documentation

Architecture documentation remains available in this repo:

- [QBit Basics](docs/01-qbit-basics.md) - Core concepts
- [Architecture](docs/ARCHITECTURE.md) - Base classes and patterns
- [Type Taxonomy](QBIT_TYPE_TAXONOMY.md) - Type definitions and guidance

## Resources

- [QQQ Framework](https://github.com/Kingsrook/qqq)
- [QQQ Wiki](https://github.com/Kingsrook/qqq/wiki)
