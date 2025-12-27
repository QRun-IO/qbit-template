#!/usr/bin/env python3
"""
Customize the QBit template:
 - Set your QBit name (e.g., "my-feature" or "MyFeature")
 - Set groupId / artifactId
 - Move source folders to the new package path
 - Rename classes

This is intended for one-time use right after cloning.

Usage:
    python3 scripts/customize_template.py

Or make executable and run:
    chmod +x scripts/customize_template.py
    ./scripts/customize_template.py
"""

import re
import shutil
from pathlib import Path


def prompt(msg: str, default: str) -> str:
    val = input(f"{msg} [{default}]: ").strip()
    return val or default


def to_pascal_case(name: str) -> str:
    """Convert 'my-qbit' or 'my_qbit' to 'MyQbit'"""
    words = re.split(r'[-_\s]+', name)
    return ''.join(word.capitalize() for word in words)


def to_camel_case(name: str) -> str:
    """Convert 'my-qbit' or 'MyQbit' to 'myQbit'"""
    pascal = to_pascal_case(name)
    return pascal[0].lower() + pascal[1:] if pascal else ''


def to_kebab_case(name: str) -> str:
    """Convert 'MyQbit' or 'my_qbit' to 'my-qbit'"""
    # Handle PascalCase
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1-\2', name)
    s2 = re.sub('([a-z0-9])([A-Z])', r'\1-\2', s1)
    return s2.lower().replace('_', '-').replace(' ', '-')


def to_package_name(name: str) -> str:
    """Convert 'my-qbit' to 'myqbit' (no separators, lowercase)"""
    return re.sub(r'[-_\s]+', '', name).lower()


def replace_in_files(files, replacements):
    for file in files:
        if not file.exists():
            continue
        try:
            text = file.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue  # Skip binary files
        changed = text
        for old, new in replacements.items():
            changed = changed.replace(old, new)
        if changed != text:
            file.write_text(changed, encoding="utf-8")
            print(f"  Updated: {file}")


def main():
    repo = Path(__file__).resolve().parents[1]
    pom = repo / "pom.xml"
    if not pom.exists():
        raise SystemExit("Run from the template repo; pom.xml not found.")

    print("=" * 60)
    print("QBit Template Customization")
    print("=" * 60)
    print()
    print("This script will rename the template to match your QBit name.")
    print("Enter your QBit name in any format (e.g., 'my-feature', 'MyFeature').")
    print()

    # Get QBit name
    qbit_name = prompt("QBit name", "my-qbit")

    # Derive various forms
    kebab_name = to_kebab_case(qbit_name)
    pascal_name = to_pascal_case(qbit_name)
    camel_name = to_camel_case(qbit_name)
    package_name = to_package_name(qbit_name)

    print()
    print(f"Derived names:")
    print(f"  kebab-case:  {kebab_name}")
    print(f"  PascalCase:  {pascal_name}")
    print(f"  camelCase:   {camel_name}")
    print(f"  package:     {package_name}")
    print()

    # Get Maven coordinates
    group_id = prompt("Maven groupId", "com.kingsrook.qbits")
    artifact_id = prompt("Maven artifactId", f"qbit-{kebab_name}")

    # Get GitHub org
    github_org = prompt("GitHub organization", "qrun-io")

    print()
    print("Configuration:")
    print(f"  groupId:     {group_id}")
    print(f"  artifactId:  {artifact_id}")
    print(f"  GitHub:      {github_org}/{artifact_id}")
    print()

    confirm = input("Proceed with customization? [Y/n]: ").strip().lower()
    if confirm == 'n':
        print("Aborted.")
        return

    # Old values (from template)
    old_package = "com.kingsrook.qbits.todo"
    old_package_path = Path("com/kingsrook/qbits/todo")
    new_package = f"{group_id}.{package_name}"
    new_package_path = Path(*new_package.split("."))

    # Move Java sources to the new package path
    print()
    print("Moving source files...")
    for src_root in ["src/main/java", "src/test/java"]:
        src_dir = repo / src_root / old_package_path
        if src_dir.exists():
            dst_dir = repo / src_root / new_package_path
            dst_dir.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(src_dir), str(dst_dir))
            print(f"  Moved: {src_root}/{old_package_path} -> {src_root}/{new_package_path}")
            # Remove empty parent dirs
            try:
                (repo / src_root / "com/kingsrook/qbits").rmdir()
                (repo / src_root / "com/kingsrook").rmdir()
                (repo / src_root / "com").rmdir()
            except OSError:
                pass  # Dir not empty, that's fine

    # Rename Java files
    print()
    print("Renaming Java files...")
    for src_root in ["src/main/java", "src/test/java"]:
        new_dir = repo / src_root / new_package_path
        if new_dir.exists():
            for java_file in new_dir.glob("Todo*.java"):
                new_name = java_file.name.replace("Todo", pascal_name)
                new_path = java_file.parent / new_name
                java_file.rename(new_path)
                print(f"  Renamed: {java_file.name} -> {new_name}")

    # Collect all files to update
    java_files = list((repo / "src").rglob("*.java"))
    xml_files = [repo / "pom.xml"]
    md_files = list(repo.glob("*.md")) + list((repo / "docs").rglob("*.md"))
    yml_files = list(repo.rglob("*.yml")) + list(repo.rglob("*.yaml"))
    all_files = java_files + xml_files + md_files + yml_files

    # Define replacements
    replacements = {
        # Package
        f"package {old_package}": f"package {new_package}",
        f"import {old_package}.": f"import {new_package}.",
        old_package: new_package,

        # Class names
        "TodoQBitConfig": f"{pascal_name}QBitConfig",
        "TodoQBitProducer": f"{pascal_name}QBitProducer",
        "todoQBitConfig": f"{camel_name}QBitConfig",
        "Todo QBit": f"{pascal_name} QBit",
        "Todo": pascal_name,  # Catch-all for remaining references

        # Maven coordinates
        "<artifactId>qbit-todo</artifactId>": f"<artifactId>{artifact_id}</artifactId>",
        "<name>QBit Todo</name>": f"<name>QBit {pascal_name}</name>",
        "qbit-todo": artifact_id,

        # GitHub URLs
        "qrun-io/qbit-todo": f"{github_org}/{artifact_id}",
        "github.com/qrun-io/qbit-todo": f"github.com/{github_org}/{artifact_id}",
    }

    print()
    print("Updating file contents...")
    replace_in_files(all_files, replacements)

    print()
    print("=" * 60)
    print("Customization complete!")
    print("=" * 60)
    print()
    print(f"  Package:     {new_package}")
    print(f"  groupId:     {group_id}")
    print(f"  artifactId:  {artifact_id}")
    print(f"  GitHub:      {github_org}/{artifact_id}")
    print()
    print("Next steps:")
    print("  1. Review the changes: git diff")
    print("  2. Build the project:  mvn clean package")
    print("  3. Commit your changes")
    print()


if __name__ == "__main__":
    main()
