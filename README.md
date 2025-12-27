# QBit Template

[![License](https://img.shields.io/badge/license-GNU%20Affero%20GPL%20v3-green.svg)](https://www.gnu.org/licenses/agpl-3.0.en.html)
[![Java](https://img.shields.io/badge/java-17+-blue.svg)](https://adoptium.net/)

> GitHub template repository for creating QBits for the QQQ framework

## Quick Start

1. **Use this template** - Click "Use this template" on GitHub
2. **Customize** - Run `python3 scripts/customize_template.py`
3. **Build** - Run `mvn clean package`

## What is a QBit?

QBits are modular, reusable components for QQQ applications:

| Type | Purpose | Examples |
|------|---------|----------|
| **Extension** | Extend QQQ functionality | Audit customization, custom auth, PHI masking |
| **Data** | Provide reference data | Countries, currencies, time zones |
| **Application** | Complete mini-apps | WMS, CRM, Inventory, Shipping |

## Documentation

- [Getting Started](docs/00-getting-started.md) - Setup and customization
- [QBit Basics](docs/01-qbit-basics.md) - Producer, Config, and entities
- [Extension Patterns](docs/02-extension-patterns.md) - Hooks and customizers
- [Data QBits](docs/03-data-qbits.md) - Reference data patterns
- [Application QBits](docs/04-application-qbits.md) - Full applications
- [Extension Points](docs/05-existing-extension-points.md) - Complete reference

## Project Structure

```
qbit-your-name/
├── src/main/java/com/kingsrook/qbits/yourname/
│   ├── YourNameQBitConfig.java    # Configuration
│   └── YourNameQBitProducer.java  # Entry point
├── src/test/java/...
│   └── BaseTest.java              # Test base class
├── docs/                          # Documentation
├── scripts/customize_template.py  # Setup script
└── pom.xml
```

## Usage in Applications

```java
new MyQBitProducer()
   .withMyQBitConfig(new MyQBitConfig()
      .withBackendName("myBackend")
      .withSomeOption(true))
   .produce(qInstance);
```

## Requirements

- Java 17+
- Maven 3.8+
- QQQ framework knowledge

## License

GNU Affero General Public License v3.0 - see [LICENSE](LICENSE)

## Resources

- [QQQ Framework](https://github.com/Kingsrook/qqq)
- [QQQ Wiki](https://github.com/Kingsrook/qqq/wiki)
- [Issues](https://github.com/Kingsrook/qqq/issues)
