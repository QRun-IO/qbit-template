# Getting Started with QBit Development

## Quick Start

### 1. Use This Template

Click "Use this template" on GitHub to create a new repository from this template.

Or clone directly:

```bash
git clone https://github.com/qrun-io/qbit-template.git qbit-my-feature
cd qbit-my-feature
rm -rf .git
git init
```

### 2. Customize the Template

Run the customization script to rename packages and classes:

```bash
python3 scripts/customize_template.py
```

The script will prompt for:
- **QBit name**: Your QBit's name (e.g., "my-feature" or "MyFeature")
- **Maven groupId**: Usually `com.kingsrook.qbits` or your organization's groupId
- **Maven artifactId**: Usually `qbit-{your-name}`
- **GitHub organization**: Where you'll push the repo

### 3. Build and Test

```bash
mvn clean package
```

### 4. Push to Your Repository

```bash
git add .
git commit -m "Initial QBit setup"
git remote add origin git@github.com:your-org/qbit-your-name.git
git push -u origin main
```

## Project Structure

```
qbit-your-name/
├── src/main/java/com/kingsrook/qbits/yourname/
│   ├── YourNameQBitConfig.java    # Configuration class
│   └── YourNameQBitProducer.java  # Producer (entry point)
├── src/test/java/.../
│   └── BaseTest.java              # Test base class
├── docs/                          # Documentation
├── pom.xml                        # Maven configuration
└── scripts/customize_template.py  # One-time setup script
```

## Next Steps

1. Read [01-qbit-basics.md](01-qbit-basics.md) to understand QBit architecture
2. Read [02-extension-patterns.md](02-extension-patterns.md) to learn how to extend QQQ
3. Add your entities, processes, or extension hooks
4. Write tests for your functionality
