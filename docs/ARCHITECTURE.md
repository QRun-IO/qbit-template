# QBit Architecture

This document covers the core architecture patterns shared across all QBit types. For type-specific guidance, see the individual type documents.

## Base Class Hierarchy

### QBitConfig and QBitProducer

Every QBit has two core components:

| Component | Purpose |
|-----------|---------|
| `QBitConfig` | Holds configuration options; validates before production |
| `QBitProducer` | Entry point; produces metadata into QInstance |

```java
// Config validates and stores options
public class MyQBitConfig implements QBitConfig
{
   private String backendName;

   @Override
   public void validate(QInstance qInstance, List<String> errors) { ... }
}

// Producer creates and registers metadata
public class MyQBitProducer implements QBitProducer
{
   @Override
   public void produce(QInstance qInstance, String namespace) { ... }
}
```

### AbstractDataQBitConfig (Data QBits)

Base class for Data QBits providing prefix and backend management:

```java
public abstract class AbstractDataQBitConfig implements QBitConfig
{
   private String backendName;           // Required: target backend
   private String tableNamePrefix;       // Optional: "geo" -> "geo_country"

   public abstract List<String> getEnabledTableNames();

   @Override
   public String getDefaultBackendNameForTables()
   {
      return backendName;
   }

   public String applyPrefix(String tableName)
   {
      if(StringUtils.hasContent(tableNamePrefix))
      {
         return tableNamePrefix + "_" + tableName;
      }
      return tableName;
   }
}
```

### AbstractDataQBitProducer (Data QBits)

Base class handling metadata discovery, prefix application, and scope tracking:

```java
public abstract class AbstractDataQBitProducer<C extends AbstractDataQBitConfig>
   implements QBitProducer
{
   @Override
   public void produce(QInstance qInstance, String namespace) throws QException
   {
      C config = getConfig();

      // 1. Create QBit identity
      QBitMetaData qBitMetaData = createQBitMetaData(namespace, config);
      qInstance.addQBit(qBitMetaData);

      // 2. Discover all producers in package
      List<MetaDataProducerInterface<?>> producers =
         MetaDataProducerHelper.findProducers(getClass().getPackageName());

      // 3. Filter disabled tables
      producers.removeIf(p -> !isProducerEnabled(p, config));

      // 4. Produce with prefix and scope
      for(MetaDataProducerInterface<?> producer : producers)
      {
         MetaDataProducerOutput output = producer.produce(qInstance);

         // Apply prefix to table names
         if(output instanceof QTableMetaData table)
         {
            table.setName(config.applyPrefix(table.getName()));
         }

         // Mark scope
         if(output instanceof SourceQBitAware sqa)
         {
            sqa.setSourceQBitName(qBitMetaData.getName());
         }

         output.addSelfToInstance(qInstance);
      }
   }

   protected abstract C getConfig();
   protected abstract QBitMetaData createQBitMetaData(String namespace, C config);
   protected abstract boolean isProducerEnabled(MetaDataProducerInterface<?> p, C config);
}
```

---

## Namespace and Scope Management

### Namespace

The namespace parameter in `produce(qInstance, namespace)` identifies a specific instance of a QBit. It's used when:

1. **Multiple instances** of the same QBit exist in one application
2. **QBit-to-QBit references** need to be resolved
3. **Admin UI** needs to filter by QBit source

```java
// Two geo instances with different namespaces
new GeoDataQBitProducer()
   .withConfig(config.withTableNamePrefix("shipping"))
   .produce(qInstance, "shipping-geo");

new GeoDataQBitProducer()
   .withConfig(config.withTableNamePrefix("billing"))
   .produce(qInstance, "billing-geo");
```

### Scope via SourceQBitAware

The `SourceQBitAware` interface marks metadata with its originating QBit:

```java
public interface SourceQBitAware
{
   String getSourceQBitName();
   void setSourceQBitName(String name);
}
```

**Implementing types:**
- `QTableMetaData`
- `QProcessMetaData`
- `QPossibleValueSource`
- `QWidgetMetaData`

**Use cases:**
- Query all tables belonging to a specific QBit
- Show QBit source in admin UI
- Dependency analysis between QBits

---

## Inter-QBit Dependencies

### Pattern 1: Maven Dependency

Classpath-level dependency for compile-time access:

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-geo-data</artifactId>
   <version>1.0.0</version>
</dependency>
```

### Pattern 2: Config Validation

Runtime validation that required components exist:

```java
@Override
public void validate(QInstance qInstance, List<String> errors)
{
   // Ensure geo QBit is loaded first
   if(qInstance.getTable("country") == null)
   {
      errors.add("geo-data QBit must be loaded before oms QBit");
   }
}
```

### Pattern 3: Namespace References

Config stores namespace of dependent QBit for cross-references:

```java
public class OMSQBitConfig implements QBitConfig
{
   private String geoQBitNamespace;  // e.g., "shipping-geo"

   // When building FK, reference prefixed table:
   // geoNamespace + "_country" -> "shipping_geo_country"
}
```

---

## Data Versioning and Updates

### Sync Process Pattern

Data QBits use a sync process to load and update reference data:

```
Source Data (JSON) --> Sync Process --> Database Tables
                           |
                           v
                    [Compare by natural key]
                           |
         +-----------------+------------------+
         |                 |                  |
      Insert           Update            Deactivate
    (new keys)    (changed values)    (removed keys)
```

### Natural Key Design

Each table defines a natural key for upsert matching:

| Table | Natural Key | Example |
|-------|-------------|---------|
| country | `alpha2Code` | "US", "CA", "MX" |
| stateProvince | `countryId` + `code` | (1, "CA"), (1, "TX") |
| city | `stateProvinceId` + `name` | (5, "Los Angeles") |

### Data File Format

Reference data ships as JSON in classpath resources:

```json
[
   {
      "alpha2Code": "US",
      "alpha3Code": "USA",
      "name": "United States",
      "numericCode": 840
   },
   {
      "alpha2Code": "CA",
      "alpha3Code": "CAN",
      "name": "Canada",
      "numericCode": 124
   }
]
```

---

## Liquibase Integration

### Template-Based Generation

Data QBits ship a template changelog; host apps generate final changelogs:

```
changelog-template.xml  -->  LiquibaseGenerator  -->  shipping-geo-changelog.xml
       +                          +                         |
  ${prefix} tokens        GeoDataQBitConfig          Host app includes
                          (prefix="shipping")
```

### Changeset ID Convention

Use prefix in changeset IDs to avoid collisions:

```xml
<changeSet id="${prefix}-create-country-v1" author="geo-qbit">
   <createTable tableName="${prefix}_country">
      ...
   </createTable>
</changeSet>
```

### Host Application Integration

```xml
<!-- Host app's master-changelog.xml -->
<databaseChangeLog>
   <!-- App's own tables -->
   <include file="db/app-changelog.xml"/>

   <!-- Generated QBit changelogs -->
   <include file="db/generated/shipping-geo-changelog.xml"/>
   <include file="db/generated/billing-geo-changelog.xml"/>
</databaseChangeLog>
```

---

## Type Comparison

| Aspect | Extension | Data | Application |
|--------|-----------|------|-------------|
| Base classes | `QBitConfig`, `QBitProducer` | `AbstractDataQBitConfig`, `AbstractDataQBitProducer` | `QBitConfig`, `QBitProducer` |
| Table prefix | N/A | Required pattern | Optional |
| Multi-instance | Rare | Common | Possible |
| QAppSection | No | Optional | Required |
| Liquibase | Minimal/none | Template generator | Standard |
| Data sync | N/A | Core feature | N/A |

---

## See Also

- [01-qbit-basics.md](01-qbit-basics.md) - Core concepts
- [02-extension-patterns.md](02-extension-patterns.md) - Extension QBits
- [03-data-qbits.md](03-data-qbits.md) - Data QBits
- [04-application-qbits.md](04-application-qbits.md) - Application QBits
- [QBIT_TYPE_TAXONOMY.md](../QBIT_TYPE_TAXONOMY.md) - Type taxonomy and architecture patterns
